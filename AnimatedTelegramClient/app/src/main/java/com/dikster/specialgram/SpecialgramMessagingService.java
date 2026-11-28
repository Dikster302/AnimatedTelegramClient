package com.dikster.specialgram;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Java implementation of the Firebase bridge to the existing TDLib client.
 * Keeps push payload handling in the Android platform layer.
 */
public final class SpecialgramMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(String token) {
        if (getApplication() instanceof SpecialgramApp) {
            ((SpecialgramApp) getApplication()).getTd().updatePushToken(token);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        JSONObject payload = new JSONObject(message.getData());
        try {
            payload.put("google.sent_time", message.getSentTime());
            if (message.getNotification() != null
                    && message.getNotification().getSound() != null) {
                payload.put("google.notification.sound",
                        message.getNotification().getSound());
            }
            if (getApplication() instanceof SpecialgramApp) {
                ((SpecialgramApp) getApplication()).getTd()
                        .processPushPayload(payload.toString());
            }
        } catch (JSONException exception) {
            FirebaseCrashlytics.getInstance().recordException(exception);
        }
    }
}
