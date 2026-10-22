package com.grinch.rivo4

import android.app.Activity
import android.app.Application
import android.os.Bundle
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import com.grinch.rivo4.view.components.ad.AdPreloader
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.ProBrandingManager

class RivoApp : Application(), ImageLoaderFactory {

    companion object {
        lateinit var instance: RivoApp
            private set
        var isAppInForeground: Boolean = false
            private set
    }

    private var resumedActivities = 0

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(64L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ProBrandingManager.apply(this, PreferenceManager(this))
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
