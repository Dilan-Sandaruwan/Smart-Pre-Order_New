package com.smartpreorder.app.notifications

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.smartpreorder.app.SmartPreOrderApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/** Firebase-only registration for public announcements. Topics are not authorization boundaries. */
object PushNotifications {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private val roles = setOf("admin", "customer", "restaurant")
    private val topics = roles + "all_users"
    private val prefs get() = SmartPreOrderApp.instance.getSharedPreferences("push", Context.MODE_PRIVATE)
    val configured: Boolean get() = FirebaseApp.getApps(SmartPreOrderApp.instance).isNotEmpty()

    fun signedIn(userId: String, role: String) {
        prefs.edit().putString("user_id", userId)
            .putString("role", role.takeIf { it in roles }).apply()
        syncToken()
    }

    /** Called at startup, on resume, after login, and when Firebase rotates the token. */
    fun syncToken() {
        if (!configured) return
        scope.launch {
            repeat(5) { attempt ->
                try {
                    lock.withLock {
                        val messaging = FirebaseMessaging.getInstance()
                        val token = messaging.token.await()
                        prefs.edit().putString("token", token).apply()
                        val role = prefs.getString("role", null)
                        val wanted = if (role != null && role in roles) setOf("all_users", role) else emptySet()
                        for (topic in topics - wanted) messaging.unsubscribeFromTopic(topic).await()
                        for (topic in wanted) messaging.subscribeToTopic(topic).await()
                    }
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("PushNotifications", "Firebase registration failed; retrying", e)
                    delay(2000L * (attempt + 1))
                }
            }
        }
    }

    fun clearLocalSession() {
        prefs.edit().remove("user_id").remove("role").apply()
        NotificationManagerCompat.from(SmartPreOrderApp.instance).cancelAll()
        syncToken()
    }

    /** Extra filtering for data messages; Console notification messages display via the FCM SDK. */
    fun accepts(userId: String?, role: String?): Boolean {
        if (userId != null && prefs.getString("user_id", null) != userId) return false
        if (role != null && prefs.getString("role", null) != role) return false
        return true
    }
}
