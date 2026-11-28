package com.dikster.specialgram;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

/** Owns operation-error reporting; UI state remains supplied by the Kotlin adapter. */
public final class TdOperationErrorController {
    public interface Listener {
        void onOperationError(String message);
        void onClearOperationError();
    }

    private final Listener listener;

    public TdOperationErrorController(Listener listener) {
        this.listener = listener;
    }

    public void clear() {
        listener.onClearOperationError();
    }

    public void report(String message) {
        FirebaseCrashlytics.getInstance().setCustomKey("operation_error_source", "local_ui_or_media");
        FirebaseCrashlytics.getInstance().recordException(
                new IllegalStateException("Local media or UI operation failed"));
        listener.onOperationError(message == null ? "Неизвестная ошибка" : message);
    }
}
