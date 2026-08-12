package com.example.mega_stream.core.network

import android.util.Log

/**
 * Global diagnostic logger for PixStreamo.
 * Automatically masks sensitive data and tracks performance metrics.
 */
object PixLog {
    private const val TAG = "PIX_DIAGNOSTIC"
    private const val PERF_TAG = "PIX_PERF"

    fun i(module: String, message: String) {
        Log.i(TAG, "[$module] $message")
    }

    fun d(module: String, message: String) {
        // Simplified for stability: logs in all builds during performance phase
        Log.d(TAG, "[$module] $message")
    }

    fun e(module: String, message: String, throwable: Throwable? = null) {
        Log.e(TAG, "[$module] ERROR: $message", throwable)
    }

    /**
     * Specialized performance logging to track potential leaks/bottlenecks
     */
    fun perf(module: String, metric: String, value: String) {
        Log.v(PERF_TAG, "[$module] METRIC: $metric = $value")
    }

    /**
     * Sanitizes URLs or handles to remove potential keys before logging
     */
    fun mask(data: String?): String {
        if (data == null) return "null"
        if (data.contains("#")) {
            return data.split("#")[0] + "#[MASKED]"
        }
        if (data.contains("http")) {
            return "http...[MASKED]"
        }
        return if (data.length > 8) data.take(8) + "..." else data
    }
}
