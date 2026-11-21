package com.example

import android.app.Application
import android.content.Context
import androidx.appfunctions.service.AppFunctionConfiguration
import com.example.ai.RecetarioAppFunctions
import com.example.util.LocaleHelper
import com.example.util.TimerAlertManager

class RecetarioApplication : Application(), AppFunctionConfiguration.Provider {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(base))
    }

    override fun onCreate() {
        super.onCreate()
        TimerAlertManager.ensureNotificationChannel(this)
    }

    override val appFunctionConfiguration: AppFunctionConfiguration
        get() = AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(RecetarioAppFunctions::class.java) {
                RecetarioAppFunctions()
            }
            .build()
}
