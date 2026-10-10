package com.smartpreorder.app

import android.app.Application
import com.smartpreorder.app.notifications.Notifier

class SmartPreOrderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        Notifier.createChannel(this)
    }

    companion object {
        lateinit var instance: SmartPreOrderApp
            private set
    }
}
