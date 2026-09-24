package com.stockpilot.app

import android.app.Application

class StockPilotApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: StockPilotApp
            private set
    }
}
