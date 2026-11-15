package com.smartpreorder.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.smartpreorder.app.SmartPreOrderApp
import com.smartpreorder.app.MainActivity
import com.smartpreorder.app.data.Order

/** Shared display for Firebase messages and live order updates. */
object Notifier {
    private const val CHANNEL_ID = "orders"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Order updates", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Order status updates" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    /** Customer side: order status changed. */
    fun statusChanged(order: Order) {
        val place = order.restaurants?.name ?: "the restaurant"
        val text = when (order.status) {
            "accepted" -> {
                val eta = order.estimatedMinutes?.let { " Ready in about $it min." } ?: ""
                "$place accepted your order.$eta"
            }
            "preparing" -> "$place is preparing your food."
            "ready" -> {
                if (order.orderType == "dine_in" && !order.tableNo.isNullOrBlank()) {
                    "Your order is ready! Please be seated at Table No. ${order.tableNo}."
                } else {
                    "Your order is ready! Please come and collect it."
                }
            }
            "completed" -> "Order completed. Enjoy your meal - and don't forget to rate it!"
            "rejected" -> "$place couldn't accept your order." +
                (order.rejectReason?.let { " Reason: $it" } ?: "")
            else -> return
        }
        show("Order #${order.id.take(6).uppercase()}", text, order.id.hashCode())
    }

    /** Restaurant side: a new order arrived. */
    fun newOrder(order: Order) {
        val who = order.customerName?.ifBlank { null } ?: "A customer"
        val type = if (order.orderType == "dine_in") "dine-in" else "pickup"
        show("New order!", "$who placed a $type order.", order.id.hashCode())
    }

    fun show(title: String, text: String, id: Int) {
        val ctx = SmartPreOrderApp.instance
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setContentIntent(PendingIntent.getActivity(ctx, id,
                Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}
