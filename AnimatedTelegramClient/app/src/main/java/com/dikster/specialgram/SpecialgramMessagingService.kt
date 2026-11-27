package com.dikster.specialgram

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

class SpecialgramMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        (application as? SpecialgramApp)?.td?.updatePushToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val payload = JSONObject(message.data as Map<*, *>)
        payload.put("google.sent_time", message.sentTime)
        message.notification?.sound?.let { payload.put("google.notification.sound", it) }
        (application as? SpecialgramApp)?.td?.processPushPayload(payload.toString())
    }
}
