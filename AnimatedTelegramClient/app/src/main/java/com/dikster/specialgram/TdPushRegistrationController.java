package com.dikster.specialgram;

import android.content.Context;
import org.drinkless.tdlib.TdApi;

/** Persists and registers the Firebase device token with TDLib. */
public final class TdPushRegistrationController {
    public interface Callback {
        void onResult(TdApi.Object result);
        void onError(TdApi.Error error);
    }

    public interface Requester {
        void send(TdApi.Function<?> function, Callback callback);
    }

    private final Context app;
    private final Requester requester;
    private String pendingToken;
    private String registeredToken;
    private boolean inFlight;
    private boolean authorized;
    private boolean closed;

    public TdPushRegistrationController(Context context, Requester requester) {
        this.app = context.getApplicationContext();
        this.requester = requester;
        this.pendingToken = app.getSharedPreferences("push", Context.MODE_PRIVATE)
                .getString("fcm_token", null);
    }

    public synchronized void updateToken(String token, boolean isAuthorized) {
        if (closed || token == null || token.trim().isEmpty()) return;
        pendingToken = token;
        authorized = isAuthorized;
        app.getSharedPreferences("push", Context.MODE_PRIVATE)
                .edit().putString("fcm_token", token).apply();
        registerIfNeeded();
    }

    public synchronized void onAuthorized() {
        if (closed) return;
        authorized = true;
        registerIfNeeded();
    }

    private synchronized void registerIfNeeded() {
        if (closed || !authorized || inFlight || pendingToken == null
                || pendingToken.isEmpty() || pendingToken.equals(registeredToken)) return;
        final String token = pendingToken;
        inFlight = true;
        try {
            requester.send(new TdApi.RegisterDevice(
                    new TdApi.DeviceTokenFirebaseCloudMessaging(token, true),
                    new long[0]), new Callback() {
                @Override public void onResult(TdApi.Object result) {
                    synchronized (TdPushRegistrationController.this) {
                        inFlight = false;
                        if (closed) return;
                        if (result instanceof TdApi.Ok) registeredToken = token;
                        if (!token.equals(pendingToken)) registerIfNeeded();
                    }
                }

                @Override public void onError(TdApi.Error error) {
                    synchronized (TdPushRegistrationController.this) {
                        inFlight = false;
                        // A later token refresh or authorization event may retry registration.
                    }
                }
            });
        } catch (RuntimeException failure) {
            inFlight = false;
        }
    }

    public synchronized void close() {
        closed = true;
        authorized = false;
        inFlight = false;
    }
}
