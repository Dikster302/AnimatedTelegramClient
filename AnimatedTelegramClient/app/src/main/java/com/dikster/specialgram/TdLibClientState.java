package com.dikster.specialgram;

/**
 * Small Java-owned observable state cell used by the TDLib client.
 *
 * The value is safely published across TDLib callback and UI threads. Updates
 * are intentionally immutable snapshots (lists/models are replaced, not edited
 * in place), matching the state publication contract used by the UI.
 */
public final class TdLibClientState<T> {
    private volatile T value;

    public TdLibClientState(T initialValue) {
        this.value = initialValue;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T newValue) {
        value = newValue;
    }
}
