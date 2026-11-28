package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Loads Telegram contacts without firing one GetUser request per contact at once.
 * Requests are serialized and paced; IDs are deduplicated during a load.
 */
public final class TdContactsController {
    public interface Callback {
        void onResult(TdApi.Object result);
        void onError(TdApi.Error error);
    }

    public interface Requester {
        void send(TdApi.Function<?> function, Callback callback);
    }

    public interface Listener {
        void onUser(TdApi.User user);
    }

    private static final long REQUEST_GAP_MS = 150L;
    private final Requester requester;
    private final Listener listener;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private final ArrayDeque<Long> pending = new ArrayDeque<>();
    private final Set<Long> queued = new HashSet<>();
    private boolean loading;
    private boolean inFlight;
    private boolean contactsRequested;
    private boolean closed;

    public TdContactsController(Requester requester, Listener listener) {
        this.requester = requester;
        this.listener = listener;
    }

    public void load() {
        synchronized (this) {
            if (closed || contactsRequested) return;
            contactsRequested = true;
        }
        requester.send(new TdApi.GetContacts(), new Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (!(result instanceof TdApi.Users)) return;
                for (long id : ((TdApi.Users) result).userIds) enqueueUser(id);
            }
            @Override public void onError(TdApi.Error error) { }
        });
    }

    /** Adds a user lookup to the same paced queue used by the contacts screen. */
    public void enqueueUser(long userId) {
        boolean shouldSchedule;
        synchronized (this) {
            if (closed || !queued.add(userId)) return;
            pending.addLast(userId);
            loading = true;
            shouldSchedule = !inFlight;
        }
        if (shouldSchedule) scheduleNext(0L);
    }

    private void scheduleNext(long delayMs) {
        synchronized (this) {
            if (closed || !loading || inFlight) return;
            if (pending.isEmpty()) {
                loading = false;
                queued.clear();
                return;
            }
            inFlight = true;
        }
        scheduler.schedule(this::requestNext, delayMs, TimeUnit.MILLISECONDS);
    }

    private void requestNext() {
        final Long userId;
        synchronized (this) {
            if (closed || !loading) {
                inFlight = false;
                return;
            }
            userId = pending.pollFirst();
            if (userId == null) {
                inFlight = false;
                loading = false;
                queued.clear();
                return;
            }
        }
        requester.send(new TdApi.GetUser(userId), new Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (result instanceof TdApi.User) listener.onUser((TdApi.User) result);
                completeOne();
            }

            @Override public void onError(TdApi.Error error) {
                completeOne();
            }
        });
    }

    private void completeOne() {
        synchronized (this) {
            inFlight = false;
            if (closed || !loading) return;
        }
        scheduleNext(REQUEST_GAP_MS);
    }

    private synchronized void finish() {
        loading = false;
        inFlight = false;
        pending.clear();
        queued.clear();
    }

    public synchronized void close() {
        closed = true;
        loading = false;
        inFlight = false;
        pending.clear();
        queued.clear();
        scheduler.shutdownNow();
    }
}
