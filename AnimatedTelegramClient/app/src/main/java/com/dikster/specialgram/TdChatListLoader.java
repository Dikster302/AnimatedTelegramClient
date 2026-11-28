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

/** Loads chat lists through one paced TDLib request queue to avoid request bursts. */
public final class TdChatListLoader implements AutoCloseable {
    public interface Callback {
        void onResult(TdApi.Object result);
        void onError(TdApi.Error error);
    }
    public interface Requester {
        void request(TdApi.Function<?> function, Callback callback);
    }
    public interface ChatListener {
        void onChat(TdApi.Chat chat, boolean archived);
    }

    private static final long REQUEST_INTERVAL_MS = 150L;
    private static final long DEFAULT_BACKOFF_MS = 60_000L;
    private static final long MAX_BACKOFF_MS = 900_000L;
    private static final Pattern RETRY_AFTER =
            Pattern.compile("retry after\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final class Task {
        final TdApi.Function<?> function;
        final boolean archived;
        final long chatId;
        Task(TdApi.Function<?> function, boolean archived, long chatId) {
            this.function = function;
            this.archived = archived;
            this.chatId = chatId;
        }
    }

    private final Requester requester;
    private final ChatListener listener;
    private final ConcurrentLinkedQueue<Task> tasks = new ConcurrentLinkedQueue<>();
    private final java.util.Set<Long> pendingChatIds = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean scheduled = new AtomicBoolean(false);
    private final AtomicBoolean inFlight = new AtomicBoolean(false);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "specialgram-chat-loader");
        thread.setDaemon(true);
        return thread;
    });

    public TdChatListLoader(Requester requester, ChatListener listener) {
        this.requester = requester;
        this.listener = listener;
    }

    public void load(TdApi.ChatList list, boolean archived) {
        tasks.offer(new Task(new TdApi.GetChats(list, 100), archived, 0L));
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
            requester.request(task.function, new Callback() {
                @Override public void onResult(TdApi.Object result) {
                    try {
                        if (result instanceof TdApi.Chats) {
                            long[] ids = ((TdApi.Chats) result).chatIds;
                            for (long id : ids) {
                                if (pendingChatIds.add(id)) {
                                    tasks.offer(new Task(new TdApi.GetChat(id), task.archived, id));
                                }
                            }
                        } else if (result instanceof TdApi.Chat) {
                            listener.onChat((TdApi.Chat) result, task.archived);
                        }
                    } finally {
                        if (task.chatId != 0L) pendingChatIds.remove(task.chatId);
                        finish(REQUEST_INTERVAL_MS);
                    }
                }

                @Override public void onError(TdApi.Error error) {
                    if (error != null && error.code == 429) {
                        tasks.offer(task);
                        finish(parseBackoff(error.message));
                    } else {
                        if (task.chatId != 0L) pendingChatIds.remove(task.chatId);
                        finish(REQUEST_INTERVAL_MS);
                    }
                }
            });
        } catch (RuntimeException failure) {
            if (task.chatId != 0L) pendingChatIds.remove(task.chatId);
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
        pendingChatIds.clear();
    }
}
