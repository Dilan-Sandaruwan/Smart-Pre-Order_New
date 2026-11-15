package com.smartpreorder.app.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        PushNotifications.syncToken()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (!PushNotifications.accepts(data["user_id"], data["role"])) return
        val title = message.notification?.title ?: data["title"] ?: "SmartPreOrder"
        val body = message.notification?.body ?: data["body"] ?: return
        Notifier.show(title, body, (data["event_id"] ?: message.messageId ?: title).hashCode())
    }
}
