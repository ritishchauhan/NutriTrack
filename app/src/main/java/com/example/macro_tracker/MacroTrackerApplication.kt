package com.example.macro_tracker

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.macro_tracker.di.AppContainer
import com.example.macro_tracker.di.DefaultAppContainer

/**
 * Custom Application class maintaining the [AppContainer] for clean MVVM Dependency Injection
 * and low-memory Coil ImageLoader configuration for low-end device optimization.
 */
class MacroTrackerApplication : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // Restrict in-memory bitmap cache to 15% of available heap to prevent OOMs on 2GB RAM devices
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("nutritrack_image_cache"))
                    .maxSizeBytes(40L * 1024 * 1024) // 40 MB disk cache limit
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }
}
