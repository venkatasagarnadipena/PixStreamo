package com.example.mega_stream

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.mega_stream.core.network.PixLog

/**
 * Custom Application class for strict RAM and Cache management.
 * Optimized for 512MB - 1GB devices.
 */
class MegaApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        PixLog.i("Application", "Initializing Optimized ImageLoader for Low-RAM targets")
        
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // STRICT CAP: Use only 15% of available heap for images
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // Small internal disk footprint
                    .build()
            }
            // Optimization for Sony BRAVIA hardware
            .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
            .allowHardware(true)
            .crossfade(true)
            .build()
    }
}
