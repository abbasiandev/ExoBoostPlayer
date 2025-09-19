package dev.abbasian.exoboostplayer

import android.app.Application
import dev.abbasian.exoboost.di.cacheModule
import dev.abbasian.exoboost.di.networkModule
import dev.abbasian.exoboost.di.playerModule
import dev.abbasian.exoboostplayer.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class ExoBoostPlayerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@ExoBoostPlayerApplication)
            modules(
                networkModule,
                cacheModule,
                playerModule,
                appModule
            )
        }
    }
}