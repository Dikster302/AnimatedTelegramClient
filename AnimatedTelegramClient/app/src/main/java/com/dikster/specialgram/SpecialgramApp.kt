package com.dikster.specialgram

import android.app.Application

class SpecialgramApp : Application() {
    lateinit var td: TdLibClient
        private set

    override fun onCreate() {
        super.onCreate()
        td = TdLibClient(this)
        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(true)
            setCustomKey("app_package", packageName)
            setCustomKey("diagnostics_scope", "crashes_and_technical_errors_only")
        }
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> td.updatePushToken(token) }
    }
}
