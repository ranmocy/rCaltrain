package me.ranmocy.rcaltrain

import android.app.Application
import java.util.concurrent.Executors

/** rCaltrain Application */
class rCaltrain : Application() {
    private val executor = Executors.newCachedThreadPool()

    override fun onCreate() {
        super.onCreate()
        executor.submit {
            DataLoader.loadDataIfNot(this)
        }
    }
}
