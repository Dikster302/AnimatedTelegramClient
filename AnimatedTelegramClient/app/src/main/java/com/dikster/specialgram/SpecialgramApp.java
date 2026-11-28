package com.dikster.specialgram;

import android.app.Application;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.dikster.specialgram.td.TgClient;

/** Android process entry point. TDLib remains the shared messaging engine. */
public final class SpecialgramApp extends Application {
    private TgClient tgClient;

    public TdLibClient getTd() {
        return tgClient.client();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        tgClient = TgClient.initialize(this);

        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.setCrashlyticsCollectionEnabled(true);
        crashlytics.setCustomKey("app_package", getPackageName());
        crashlytics.setCustomKey("diagnostics_scope", "crashes_and_technical_errors_only");

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> getTd().updatePushToken(token));
    }
}
