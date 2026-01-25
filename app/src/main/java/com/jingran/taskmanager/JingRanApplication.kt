package com.jingran.taskmanager

import android.app.Application
import com.jingran.utils.LogManager
import com.jingran.taskmanager.di.DependencyInjectionModule

class JingRanApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        LogManager.init(this)
        LogManager.i(TAG, "Application initialized")
        
        DependencyInjectionModule.initialize(this)
        LogManager.i(TAG, "Dependency injection initialized")
    }

    companion object {
        private const val TAG = "JingRanApp"
    }
}