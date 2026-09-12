package com.finora.android

import android.app.Application
import com.finora.android.core.di.DatabaseModule

class FinoraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DatabaseModule.initialize(this)
    }
}
