package com.dikster.specialgram;

/** Tracks profile requests so late TDLib responses cannot overwrite a newer profile. */
public final class TdProfileSessionController {
    private long activeUserId;
    private boolean hasActiveUser;
    private int generation;

    public synchronized int open() {
        activeUserId = 0L;
        hasActiveUser = false;
        return ++generation;
    }

    public synchronized void close() {
        generation++;
        activeUserId = 0L;
        hasActiveUser = false;
    }

    public synchronized boolean isCurrent(int expectedGeneration) {
        return generation == expectedGeneration;
    }

    public synchronized void setActiveUser(long userId) {
        activeUserId = userId;
        hasActiveUser = true;
    }

    public synchronized boolean isActiveUser(long userId) {
        return hasActiveUser && activeUserId == userId;
    }
}
