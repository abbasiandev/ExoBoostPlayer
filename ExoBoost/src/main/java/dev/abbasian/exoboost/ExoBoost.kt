package dev.abbasian.exoboost

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import dev.abbasian.exoboost.di.cacheModule
import dev.abbasian.exoboost.di.dataModule
import dev.abbasian.exoboost.di.networkModule
import dev.abbasian.exoboost.di.playerModule
import dev.abbasian.exoboost.domain.error.ErrorClassifier
import dev.abbasian.exoboost.util.DefaultLogger
import dev.abbasian.exoboost.util.ExoBoostLogger
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

object ExoBoost {

    private const val TAG = "ExoBoost"
    private var isInitialized = false
    private var customLogger: ExoBoostLogger? = null

    @OptIn(UnstableApi::class)
    fun initialize(context: Context) {
        if (isInitialized) {
            customLogger?.warning(TAG, "Already initialized")
            return
        }

        internalInitialize(context)
    }

    @UnstableApi
    private fun internalInitialize(context: Context) {
        startKoin {
            androidContext(context)
            modules(getModules())
        }
        isInitialized = true
    }

    @UnstableApi
    private fun getModules() = listOf(
        createLoggingModule(),
        networkModule,
        cacheModule,
        dataModule,
        playerModule,
    )

    @UnstableApi
    private fun createLoggingModule() = module {
        single<ExoBoostLogger> {
            customLogger ?: DefaultLogger()
        }
        single { ErrorClassifier(context = androidContext(), logger = get()) }
    }
}