package com.kttelematictask.utils

import android.app.Application
import com.kttelematictask.dependency_injection.repositoryModule
import com.kttelematictask.dependency_injection.viewModelModule
import org.koin.core.context.startKoin

class AppConfig : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            modules(listOf(repositoryModule, viewModelModule))
        }
    }

}