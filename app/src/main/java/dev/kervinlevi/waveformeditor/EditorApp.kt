package dev.kervinlevi.waveformeditor

import android.app.Application
import dev.kervinlevi.waveformeditor.di.AppModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class EditorApp: Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@EditorApp)
            modules(AppModule.module)
        }
    }
}
