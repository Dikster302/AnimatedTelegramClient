package com.dikster.specialgram;

import android.content.Context;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

/** Java-side TDLib request dispatch and diagnostics. */
public final class TdRequestDispatcher {
    public interface Listener {
        void onResult(TdApi.Object result);
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
                DiagnosticsJournal.record(app, function.getClass().getSimpleName(),
                        error.code, error.message);
                FirebaseCrashlytics.getInstance().log("TDLib request failed: "
                        + function.getClass().getSimpleName() + ", code=" + error.code);
                FirebaseCrashlytics.getInstance().setCustomKey(
                        "tdlib_function", function.getClass().getSimpleName());
                FirebaseCrashlytics.getInstance().setCustomKey("tdlib_error_code", error.code);
                FirebaseCrashlytics.getInstance().recordException(new IllegalStateException(
                        "TDLib request failed: " + function.getClass().getSimpleName()
                                + ", code=" + error.code));
            }
            if (listener != null) listener.onResult(result);
        });
    }
}
