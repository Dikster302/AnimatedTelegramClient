package com.dikster.specialgram;

import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared flood-control gate for every story request queue in this process.
 * TDLib's retry_after applies to story traffic as a whole, not just one queue.
 */
public final class TdStoryRateLimit {
    private static final Pattern RETRY_AFTER =
            Pattern.compile("retry after\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final long DEFAULT_BACKOFF_MS = 60_000L;
    private static final long MAX_BACKOFF_MS = 900_000L;
    private static final AtomicLong blockedUntil = new AtomicLong(0L);

    private TdStoryRateLimit() { }

    public static void blockFor(long delayMs) {
        long bounded = Math.max(1_000L, Math.min(MAX_BACKOFF_MS, delayMs));
        long deadline = System.currentTimeMillis() + bounded;
        blockedUntil.accumulateAndGet(deadline, Math::max);
    }

    public static void blockFromError(String message) {
        long delay = DEFAULT_BACKOFF_MS;
        if (message != null) {
            Matcher matcher = RETRY_AFTER.matcher(message);
            if (matcher.find()) {
                try {
                    delay = Long.parseLong(matcher.group(1)) * 1_000L;
                } catch (NumberFormatException ignored) { }
            }
        }
        blockFor(delay);
    }

    public static long remainingDelayMs() {
        return Math.max(0L, blockedUntil.get() - System.currentTimeMillis());
    }
}
