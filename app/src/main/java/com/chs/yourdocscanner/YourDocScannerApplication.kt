package com.chs.yourdocscanner

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinApplication
import org.koin.core.context.GlobalContext.startKoin
import org.koin.ksp.generated.defaultModule

@KoinApplication
class YourDocScannerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@YourDocScannerApplication)
            modules(defaultModule)
        }
    }
}