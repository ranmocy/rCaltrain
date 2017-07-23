package me.ranmocy.rcaltrain

import android.app.Application

/** rCaltrain Application */
class rCaltrain : Application() {
    override fun onCreate() {
        super.onCreate()
        DataLoader.loadDataIfNot(this)
    }
}
