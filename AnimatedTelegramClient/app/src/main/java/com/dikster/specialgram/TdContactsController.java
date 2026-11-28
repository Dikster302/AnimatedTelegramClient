package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final long MAX_BACKOFF_MS = 30_000L;
    private static final int MAX_RETRIES = 5;
    private static final Pattern RETRY_AFTER =
            Pattern.compile("retry after\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private final Requester requester;
    private final Listener listener;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private final ArrayDeque<Long> pending = new ArrayDeque<>();
    private final Set<Long> queued = new HashSet<>();
    private final Set<Long> completedUsers = new HashSet<>();
    private final Map<Long, Integer> retries = new HashMap<>();
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
            if (closed || completedUsers.contains(userId) || !queued.add(userId)) return;
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
                retries.remove(userId);
                if (result instanceof TdApi.User) {
                    synchronized (TdContactsController.this) {
                        completedUsers.add(userId);
                        queued.remove(userId);
                    }
                    listener.onUser((TdApi.User) result);
                } else {
                    synchronized (TdContactsController.this) { queued.remove(userId); }
                }
                completeOne(REQUEST_GAP_MS);
            }

            @Override public void onError(TdApi.Error error) {
                long delay = REQUEST_GAP_MS;
                synchronized (TdContactsController.this) {
                    if (error != null && error.code == 429) {
                        int attempt = retries.getOrDefault(userId, 0) + 1;
                        if (attempt <= MAX_RETRIES) {
                            retries.put(userId, attempt);
                            pending.addFirst(userId);
                            delay = retryDelay(error.message, attempt);
                        } else {
                            retries.remove(userId);
                            queued.remove(userId);
                        }
                    } else {
                        retries.remove(userId);
                        queued.remove(userId);
                    }
                }
                completeOne(delay);
            }
        });
    }

    private void completeOne(long delayMs) {
        synchronized (this) {
            inFlight = false;
            if (closed || !loading) return;
        }
        scheduleNext(delayMs);
    }

    private long retryDelay(String message, int attempt) {
        if (message != null) {
            Matcher matcher = RETRY_AFTER.matcher(message);
            if (matcher.find()) {
                try {
                    long seconds = Long.parseLong(matcher.group(1));
                    return Math.min(MAX_BACKOFF_MS, Math.max(1_000L, seconds * 1_000L));
                } catch (NumberFormatException ignored) { }
            }
        }
        return Math.min(MAX_BACKOFF_MS, 1_000L << Math.min(attempt, 4));
    }

    private synchronized void finish() {
        loading = false;
        inFlight = false;
        pending.clear();
        queued.clear();
        retries.clear();
    }

    public synchronized void close() {
        closed = true;
        loading = false;
        inFlight = false;
        pending.clear();
        queued.clear();
        completedUsers.clear();
        retries.clear();
        scheduler.shutdownNow();
    }
}
