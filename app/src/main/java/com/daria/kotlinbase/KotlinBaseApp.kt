package com.daria.kotlinbase

import android.app.Application
import com.daria.kotlinbase.application.AppModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class KotlinBaseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.NONE)
            androidContext(this@KotlinBaseApp)
            modules(AppModules.modules)
        }
    }
}
