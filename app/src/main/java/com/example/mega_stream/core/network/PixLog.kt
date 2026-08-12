package com.example.mega_stream.core.network

/**
 * PRODUCTION LOGGER: All commands are strictly disabled.
 * Zero output to Logcat for final build.
 */
object PixLog {
    fun i(module: String, message: String) {}
    fun d(module: String, message: String) {}
    fun e(module: String, message: String, throwable: Throwable? = null) {}
    fun perf(module: String, metric: String, value: String) {}
    fun mask(data: String?): String = ""
}
