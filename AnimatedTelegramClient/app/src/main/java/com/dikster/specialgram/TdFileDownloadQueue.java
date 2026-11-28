package com.dikster.specialgram;

import org.drinkless.tdlib.TdApi;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Serializes media downloads and backs off on TDLib 429 responses. */
public final class TdFileDownloadQueue {
    public interface Callback {
        void onResult(TdApi.Object result);
        void onError(TdApi.Error error);
    }
    public interface Requester {
        void send(TdApi.Function<?> function, Callback callback);
    }

    private static final long REQUEST_GAP_MS = 180L;
    private static final long MAX_BACKOFF_MS = 30_000L;
    private static final int MAX_RETRIES = 6;
    private static final Pattern RETRY_AFTER =
            Pattern.compile("retry after\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

    private final Requester requester;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private final ArrayDeque<Integer> pending = new ArrayDeque<>();
    private final Set<Integer> requested = new HashSet<>();
    private final Set<Integer> completed = new HashSet<>();
    private final Map<Integer, Integer> retries = new HashMap<>();
    private boolean inFlight;
    private boolean closed;

    public TdFileDownloadQueue(Requester requester) {
        this.requester = requester;
    }

    public void enqueue(int fileId) {
        boolean schedule;
        synchronized (this) {
            if (closed || fileId <= 0 || completed.contains(fileId) || !requested.add(fileId)) return;
            pending.addLast(fileId);
            schedule = !inFlight;
            if (schedule) inFlight = true;
        }
        if (schedule) scheduler.schedule(this::requestNext, 0L, TimeUnit.MILLISECONDS);
    }

    public synchronized void markCompleted(int fileId) {
        requested.remove(fileId);
        completed.add(fileId);
        retries.remove(fileId);
        pending.removeIf(id -> id == fileId);
    }

    private void requestNext() {
        final Integer fileId;
        synchronized (this) {
            if (closed) { inFlight = false; return; }
            fileId = pending.pollFirst();
            if (fileId == null) { inFlight = false; return; }
        }
        requester.send(new TdApi.DownloadFile(fileId, 32, 0, 0, false), new Callback() {
            @Override public void onResult(TdApi.Object result) {
                if (result instanceof TdApi.File && ((TdApi.File) result).local.isDownloadingCompleted)
                    markCompleted(fileId);
                finishAndContinue(REQUEST_GAP_MS);
            }

            @Override public void onError(TdApi.Error error) {
                long delay = REQUEST_GAP_MS;
                synchronized (TdFileDownloadQueue.this) {
                    if (closed) { inFlight = false; return; }
                    if (error.code == 429) {
                        int attempt = retries.getOrDefault(fileId, 0) + 1;
                        if (attempt <= MAX_RETRIES) {
                            retries.put(fileId, attempt);
                            pending.addFirst(fileId);
                            delay = retryDelay(error.message, attempt);
                        } else {
                            requested.remove(fileId);
                            retries.remove(fileId);
                        }
                    } else {
                        requested.remove(fileId);
                        retries.remove(fileId);
                    }
                }
                finishAndContinue(delay);
            }
        });
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

    private void finishAndContinue(long delayMs) {
        synchronized (this) {
            if (closed) { inFlight = false; return; }
            if (pending.isEmpty()) { inFlight = false; return; }
        }
        scheduler.schedule(this::requestNext, delayMs, TimeUnit.MILLISECONDS);
    }

    public synchronized void close() {
        closed = true;
        pending.clear();
        requested.clear();
        completed.clear();
        retries.clear();
        inFlight = false;
        scheduler.shutdownNow();
    }
}
