package com.grinch.rivo4

import android.app.Activity
import android.app.Application
import android.os.Bundle
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import com.grinch.rivo4.view.components.ad.AdPreloader

class RivoApp : Application() {

    companion object {
        var isAppInForeground: Boolean = false
            private set
    }

    private var resumedActivities = 0

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@RivoApp)
            modules(appModule)
        }

        AdPreloader.init(this@RivoApp)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                resumedActivities++
                isAppInForeground = resumedActivities > 0
            }
            override fun onActivityPaused(activity: Activity) {
                resumedActivities = (resumedActivities - 1).coerceAtLeast(0)
                isAppInForeground = resumedActivities > 0
            }
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
