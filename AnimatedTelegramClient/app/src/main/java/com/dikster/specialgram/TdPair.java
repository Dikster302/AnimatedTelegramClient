package com.dikster.specialgram;

/** Small immutable Java pair used by TDLib state maps without Kotlin runtime types. */
public final class TdPair<F, S> {
    private final F first;
    private final S second;

    public TdPair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public F getFirst() { return first; }
    public S getSecond() { return second; }
}
