package com.dikster.specialgram;

import android.app.Application;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.messaging.FirebaseMessaging;

/** Android process entry point. TDLib remains the shared messaging engine. */
public final class SpecialgramApp extends Application {
    private TdLibClient td;

    public TdLibClient getTd() {
        return td;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        td = new TdLibClient(this);

        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.setCrashlyticsCollectionEnabled(true);
        crashlytics.setCustomKey("app_package", getPackageName());
        crashlytics.setCustomKey("diagnostics_scope", "crashes_and_technical_errors_only");

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> td.updatePushToken(token));
    }
}
