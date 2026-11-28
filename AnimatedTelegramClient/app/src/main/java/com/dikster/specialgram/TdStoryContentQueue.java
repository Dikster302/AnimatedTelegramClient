package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Rate-limited queue for fetching individual story payloads. */
public final class TdStoryContentQueue implements AutoCloseable {
    public interface Callback {
        void onResult(TdApi.Object result);
        void onError(TdApi.Error error);
    }
    public interface Requester {
        void send(TdApi.Function<?> function, Callback callback);
    }
    public interface Listener {
        void onStory(TdApi.Story story, boolean viewed);
    }

    private static final long REQUEST_GAP_MS = 350L;
    private static final long MAX_BACKOFF_MS = 900_000L;
    private static final int MAX_RETRIES = 5;
    private static final Pattern RETRY_AFTER =
            Pattern.compile("retry after\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final class Item {
        final long chatId;
        final int storyId;
        final boolean viewed;
        int retries;
        Item(long chatId, int storyId, boolean viewed) {
            this.chatId = chatId;
            this.storyId = storyId;
            this.viewed = viewed;
        }
        String key() { return chatId + ":" + storyId; }
    }

    private final Requester requester;
    private final Listener listener;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ArrayDeque<Item> pending = new ArrayDeque<>();
    private final Set<String> queued = new HashSet<>();
    private final Set<String> completed = new HashSet<>();
    private boolean inFlight;
    private boolean closed;

    public TdStoryContentQueue(Requester requester, Listener listener) {
        this.requester = requester;
        this.listener = listener;
    }

    public void enqueue(long chatId, int storyId, boolean viewed) {
        boolean schedule;
        synchronized (this) {
            if (closed) return;
            Item item = new Item(chatId, storyId, viewed);
            if (completed.contains(item.key()) || !queued.add(item.key())) return;
            pending.addLast(item);
            schedule = !inFlight;
            if (schedule) inFlight = true;
        }
        if (schedule) scheduler.schedule(this::requestNext, 0L, TimeUnit.MILLISECONDS);
    }

    private void requestNext() {
        final Item item;
        synchronized (this) {
            if (closed) { inFlight = false; return; }
            item = pending.pollFirst();
            if (item == null) { inFlight = false; return; }
        }
        try {
            requester.send(new TdApi.GetStory(item.chatId, item.storyId, false), new Callback() {
                @Override public void onResult(TdApi.Object result) {
                    synchronized (TdStoryContentQueue.this) {
                        queued.remove(item.key());
                        if (result instanceof TdApi.Story) completed.add(item.key());
                    }
                    if (result instanceof TdApi.Story) listener.onStory((TdApi.Story) result, item.viewed);
                    finish(REQUEST_GAP_MS);
                }

                @Override public void onError(TdApi.Error error) {
                    long delay = REQUEST_GAP_MS;
                    synchronized (TdStoryContentQueue.this) {
                        if (closed) { inFlight = false; return; }
                        if (error != null && error.code == 429 && ++item.retries <= MAX_RETRIES) {
                            pending.addFirst(item);
                            delay = retryDelay(error.message, item.retries);
                        } else {
                            queued.remove(item.key());
                        }
                    }
                    finish(delay);
                }
            });
        } catch (RuntimeException failure) {
            synchronized (this) { queued.remove(item.key()); }
            finish(REQUEST_GAP_MS);
        }
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
        return Math.min(MAX_BACKOFF_MS, 1_000L << Math.min(attempt, 5));
    }

    private void finish(long delayMs) {
        synchronized (this) {
            if (closed) { inFlight = false; return; }
            if (pending.isEmpty()) { inFlight = false; return; }
        }
        scheduler.schedule(this::requestNext, delayMs, TimeUnit.MILLISECONDS);
    }

    @Override public synchronized void close() {
        closed = true;
        pending.clear();
        queued.clear();
        completed.clear();
        inFlight = false;
        scheduler.shutdownNow();
    }
}
