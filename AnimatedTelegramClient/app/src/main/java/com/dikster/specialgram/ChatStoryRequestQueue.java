package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Serializes story lookups and respects TDLib flood-control responses. */
public final class ChatStoryRequestQueue implements AutoCloseable {
    public interface Callback {
        void onResult(TdApi.Object result);
        void onError(TdApi.Error error);
    }
    public interface Requester {
        void request(long chatId, Callback callback);
    }
    public interface ResultListener {
        void onResult(TdApi.Object result);
    }

    private static final Pattern RETRY_AFTER =
            Pattern.compile("retry after\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    // Story lookups are deliberately paced to avoid flooding Telegram while a large chat list loads.
    private static final long NORMAL_DELAY_MS = 2_500L;
    private static final long DEFAULT_BACKOFF_MS = 60_000L;
    private static final long MAX_BACKOFF_MS = 900_000L;

    private final Requester requester;
    private final ResultListener resultListener;
    private final ConcurrentLinkedQueue<Long> queue = new ConcurrentLinkedQueue<>();
    private final java.util.Set<Long> pending = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean scheduled = new AtomicBoolean(false);
    private final AtomicBoolean inFlight = new AtomicBoolean(false);
    private final AtomicLong backoffUntil = new AtomicLong(0L);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "specialgram-story-queue");
        thread.setDaemon(true);
        return thread;
    });

    public ChatStoryRequestQueue(Requester requester, ResultListener resultListener) {
        this.requester = requester;
        this.resultListener = resultListener;
    }

    public void enqueue(long chatId) {
        if (pending.add(chatId)) {
            queue.offer(chatId);
            schedule(0L);
        }
    }

    private void schedule(long delayMs) {
        if (!scheduled.compareAndSet(false, true)) return;
        try {
            executor.schedule(() -> {
                scheduled.set(false);
                dispatchNext();
            }, Math.max(0L, delayMs), TimeUnit.MILLISECONDS);
        } catch (RuntimeException rejected) {
            scheduled.set(false);
        }
    }

    private void dispatchNext() {
        if (!inFlight.compareAndSet(false, true)) return;
        Long chatId = queue.poll();
        if (chatId == null) {
            inFlight.set(false);
            return;
        }
        long waitMs = Math.max(0L, backoffUntil.get() - System.currentTimeMillis());
        if (waitMs > 0L) {
            queue.offer(chatId);
            inFlight.set(false);
            schedule(waitMs);
            return;
        }
        try {
            requester.request(chatId, new Callback() {
                @Override public void onResult(TdApi.Object result) {
                    pending.remove(chatId);
                    try { resultListener.onResult(result); }
                    finally { finish(NORMAL_DELAY_MS); }
                }
                @Override public void onError(TdApi.Error error) {
                    if (error != null && error.code == 429) {
                        long delay = parseBackoff(error.message);
                        backoffUntil.set(System.currentTimeMillis() + delay);
                        queue.offer(chatId);
                        finish(delay);
                    } else {
                        pending.remove(chatId);
                        finish(NORMAL_DELAY_MS);
                    }
                }
            });
        } catch (RuntimeException failure) {
            pending.remove(chatId);
            finish(NORMAL_DELAY_MS);
        }
    }

    private void finish(long delayMs) {
        inFlight.set(false);
        schedule(delayMs);
    }

    private static long parseBackoff(String message) {
        if (message == null) return DEFAULT_BACKOFF_MS;
        Matcher matcher = RETRY_AFTER.matcher(message);
        if (!matcher.find()) return DEFAULT_BACKOFF_MS;
        try {
            long seconds = Long.parseLong(matcher.group(1));
            return Math.max(1_000L, Math.min(MAX_BACKOFF_MS, seconds * 1_000L));
        } catch (NumberFormatException ignored) {
            return DEFAULT_BACKOFF_MS;
        }
    }

    @Override public void close() {
        executor.shutdownNow();
        queue.clear();
        pending.clear();
    }
}
