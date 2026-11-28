package com.dikster.specialgram;

import android.content.Context;
import org.drinkless.tdlib.TdApi;

/** Routes TDLib authorization-state transitions without UI or Kotlin dependencies. */
public final class TdAuthorizationStateRouter {
    public interface Listener {
        void onAuthChanged(Auth state);
        void onSend(TdApi.Function<?> function);
        void onAuthorized();
    }

    private final Context app;
    private final String apiId;
    private final String apiHash;
    private final Listener listener;

    public TdAuthorizationStateRouter(Context context, String apiId, String apiHash,
                                      Listener listener) {
        this.app = context.getApplicationContext();
        this.apiId = apiId == null ? "" : apiId;
        this.apiHash = apiHash == null ? "" : apiHash;
        this.listener = listener;
    }

    public void onState(TdApi.AuthorizationState state) {
        if (state instanceof TdApi.AuthorizationStateWaitTdlibParameters) {
            try {
                listener.onSend(TdAuthorizationController.createParameters(app, apiId, apiHash));
            } catch (Exception error) {
                listener.onAuthChanged(new Auth.Error(
                        error.getMessage() == null ? "Не удалось настроить TDLib" : error.getMessage()));
            }
        } else if (state instanceof TdApi.AuthorizationStateWaitPhoneNumber) {
            listener.onAuthChanged(Auth.Phone);
        } else if (state instanceof TdApi.AuthorizationStateWaitCode) {
            listener.onAuthChanged(Auth.Code);
        } else if (state instanceof TdApi.AuthorizationStateWaitPassword) {
            listener.onAuthChanged(Auth.Password);
        } else if (state instanceof TdApi.AuthorizationStateReady) {
            listener.onAuthChanged(Auth.Ready);
            listener.onAuthorized();
        } else if (state instanceof TdApi.AuthorizationStateClosed) {
            listener.onAuthChanged(new Auth.Error("TDLib connection closed"));
        }
    }
}
