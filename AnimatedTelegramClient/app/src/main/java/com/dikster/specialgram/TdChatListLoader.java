package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Loads complete TDLib chat lists using the update-driven LoadChats API. */
public final class TdChatListLoader implements AutoCloseable {
    public interface Callback {
        void onResult(TdApi.Object result);
        void onError(TdApi.Error error);
    }
    public interface CompletionListener { void onAllChatListsLoaded(); }
    public interface Requester {
        void request(TdApi.Function<?> function, Callback callback);
    }

    private static final int PAGE_SIZE = 100;
    private static final long REQUEST_INTERVAL_MS = 50L;
    private static final long DEFAULT_BACKOFF_MS = 60_000L;
    private static final long MAX_BACKOFF_MS = 900_000L;
    private static final Pattern RETRY_AFTER =
            Pattern.compile("retry after\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final class Task {
        final TdApi.ChatList list;
        final String key;
        Task(TdApi.ChatList list, String key) {
            this.list = list;
            this.key = key;
        }
    }

    private final Requester requester;
    private final CompletionListener completionListener;
    private final java.util.Set<String> completedLists = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean completionNotified = new AtomicBoolean(false);
    private final ConcurrentLinkedQueue<Task> tasks = new ConcurrentLinkedQueue<>();
    private final java.util.Set<String> loadingLists = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, Integer> transientRetries = new ConcurrentHashMap<>();
    private final AtomicBoolean scheduled = new AtomicBoolean(false);
    private final AtomicBoolean inFlight = new AtomicBoolean(false);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "specialgram-chat-loader");
        thread.setDaemon(true);
        return thread;
    });

    public TdChatListLoader(Requester requester) { this(requester, null); }

    public TdChatListLoader(Requester requester, CompletionListener completionListener) {
        if (requester == null) throw new IllegalArgumentException("requester == null");
        this.requester = requester;
        this.completionListener = completionListener;
    }

    private void markListCompleted(String key) {
        loadingLists.remove(key);
        transientRetries.remove(key);
        completedLists.add(key);
        if (completedLists.contains("main") && completedLists.contains("archive")
                && completionNotified.compareAndSet(false, true)
                && completionListener != null) {
            try { completionListener.onAllChatListsLoaded(); }
            catch (RuntimeException ignored) { }
        }
    }

    public void load(TdApi.ChatList list, boolean archived) {
        String key = archived ? "archive" : "main";
        if (!loadingLists.add(key)) return;
        tasks.offer(new Task(list, key));
        schedule(0L);
    }

    private void schedule(long delayMs) {
        if (!scheduled.compareAndSet(false, true)) return;
        try {
            executor.schedule(() -> {
                scheduled.set(false);
                dispatch();
            }, Math.max(0L, delayMs), TimeUnit.MILLISECONDS);
        } catch (RuntimeException ignored) {
            scheduled.set(false);
        }
    }

    private void dispatch() {
        if (!inFlight.compareAndSet(false, true)) return;
        Task task = tasks.poll();
        if (task == null) {
            inFlight.set(false);
            return;
        }
        try {
            requester.request(new TdApi.LoadChats(task.list, PAGE_SIZE), new Callback() {
                @Override public void onResult(TdApi.Object result) {
                    if (result instanceof TdApi.Ok) {
                        transientRetries.remove(task.key);
                        // TDLib emits updateNewChat/updateChatPosition for each loaded page.
                        // Keep loading until TDLib explicitly reports that the list is exhausted.
                        tasks.offer(task);
                    } else {
                        markListCompleted(task.key);
                    }
                    finish(REQUEST_INTERVAL_MS);
                }

                @Override public void onError(TdApi.Error error) {
                    if (error != null && error.code == 429) {
                        tasks.offer(task);
                        finish(parseBackoff(error.message));
                    } else if (error != null && error.code == 404) {
                        // 404 is TDLib's normal end-of-list signal for LoadChats.
                        markListCompleted(task.key);
                        finish(REQUEST_INTERVAL_MS);
                    } else {
                        // Do not silently abandon the first chat-list load on a transient
                        // TDLib error. Retry a few times with bounded exponential backoff.
                        int attempt = transientRetries.merge(task.key, 1, Integer::sum);
                        if (attempt <= 5) {
                            tasks.offer(task);
                            finish(Math.min(15_000L, 750L << attempt));
                        } else {
                            loadingLists.remove(task.key);
                            transientRetries.remove(task.key);
                            finish(REQUEST_INTERVAL_MS);
                        }
                    }
                }
            });
        } catch (RuntimeException failure) {
            loadingLists.remove(task.key);
            finish(REQUEST_INTERVAL_MS);
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
        tasks.clear();
        loadingLists.clear();
        transientRetries.clear();
        completedLists.clear();
    }
}
