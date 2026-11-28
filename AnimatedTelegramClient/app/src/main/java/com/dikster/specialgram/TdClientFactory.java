package com.dikster.specialgram;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

/** Creates TDLib client and centralizes callback-failure diagnostics. */
public final class TdClientFactory {
    public interface Listener {
        void onUpdate(TdApi.Object update);
        void onUpdateError(Throwable error);
        void onFatalError(Throwable error);
    }

    private TdClientFactory() { }

    public static Client create(Listener listener) {
        return Client.create(
                update -> listener.onUpdate(update),
                error -> {
                    recordCallbackFailure("update_error", error);
                    listener.onUpdateError(error);
                },
                error -> {
                    recordCallbackFailure("fatal_error", error);
                    listener.onFatalError(error);
                });
    }

    private static void recordCallbackFailure(String type, Throwable error) {
        FirebaseCrashlytics.getInstance().setCustomKey("tdlib_callback", type);
        FirebaseCrashlytics.getInstance().recordException(new IllegalStateException(
                "TDLib " + type + " callback failed", error));
    }
}
