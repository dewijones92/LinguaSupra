package com.dewijones.linguasupra

import android.app.Application
import com.dewijones.linguasupra.data.AppContainer

class LinguaSupraApp : Application() {

    val container: AppContainer by lazy { AppContainer.get(this) }

    override fun onCreate() {
        super.onCreate()
        container
    }
}
