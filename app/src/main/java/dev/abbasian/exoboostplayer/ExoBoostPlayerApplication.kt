package dev.abbasian.exoboostplayer

import android.app.Application
import dev.abbasian.exoboost.ExoBoost

class ExoBoostPlayerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ExoBoost.initialize(this)
    }
}