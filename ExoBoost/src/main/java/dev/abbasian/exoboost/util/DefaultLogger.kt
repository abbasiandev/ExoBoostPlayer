package dev.abbasian.exoboost.util

import android.util.Log
import dev.abbasian.exoboost.BuildConfig

internal class DefaultLogger : ExoBoostLogger {

    override fun debug(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    override fun info(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun warning(tag: String, message: String, throwable: Throwable?) {
        Log.w(tag, message, throwable)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }
}