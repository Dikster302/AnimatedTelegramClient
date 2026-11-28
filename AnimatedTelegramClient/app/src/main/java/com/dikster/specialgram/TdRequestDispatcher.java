package com.dikster.specialgram;

import android.content.Context;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

/** Java-side TDLib request dispatch and diagnostics. */
public final class TdRequestDispatcher {
    public interface Listener {
        void onResult(TdApi.Object result);
        void onError(TdApi.Error error);
    }

    private final Context app;
    private final Client client;

    public TdRequestDispatcher(Context context, Client client) {
        this.app = context.getApplicationContext();
        this.client = client;
    }

    public void send(TdApi.Function<?> function, Listener listener) {
        client.send(function, result -> {
            if (result instanceof TdApi.Error) {
                TdApi.Error error = (TdApi.Error) result;
                if (!isExpectedTerminalError(function, error)) {
                    DiagnosticsJournal.record(app, function.getClass().getSimpleName(),
                            error.code, error.message);
                    FirebaseCrashlytics.getInstance().log("TDLib request failed: "
                            + function.getClass().getSimpleName() + ", code=" + error.code);
                    FirebaseCrashlytics.getInstance().setCustomKey(
                            "tdlib_function", function.getClass().getSimpleName());
                    FirebaseCrashlytics.getInstance().setCustomKey("tdlib_error_code", error.code);
                    // Flood-control responses are recoverable and are handled by request queues.
                    // Keep them in the local journal, but don't pollute Crashlytics as exceptions.
                    if (error.code != 429) {
                        FirebaseCrashlytics.getInstance().recordException(new IllegalStateException(
                                "TDLib request failed: " + function.getClass().getSimpleName()
                                        + ", code=" + error.code));
                    }
                }
            }
            if (listener == null) return;
            if (result instanceof TdApi.Error) {
                listener.onError((TdApi.Error) result);
            } else {
                listener.onResult(result);
            }
        });
    }

    /**
     * TDLib uses 404 from LoadChats to signal that the requested chat list is exhausted.
     * Story lookups can also legitimately fail when the current account cannot access
     * stories for a particular chat. Neither condition is an application crash.
     */
    private static boolean isExpectedTerminalError(TdApi.Function<?> function, TdApi.Error error) {
        if (error == null) return false;
        if (function instanceof TdApi.LoadChats && error.code == 404) return true;
        return function instanceof TdApi.GetChatActiveStories
                && error.code == 400
                && error.message != null
                && error.message.toLowerCase(java.util.Locale.ROOT)
                        .contains("cant access the chat");
    }
}
