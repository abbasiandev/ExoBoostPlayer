package dev.abbasian.exoboostplayer

import android.app.Application
import dev.abbasian.exoboost.ExoBoost
import dev.abbasian.exoboostplayer.di.appModule
import org.koin.core.context.loadKoinModules

class ExoBoostPlayerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ExoBoost.initialize(this)
        loadKoinModules(appModule)
    }
}