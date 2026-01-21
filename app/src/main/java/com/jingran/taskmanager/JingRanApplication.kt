package com.jingran.taskmanager

import android.app.Application
import com.jingran.utils.LogManager

class JingRanApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Logging
        LogManager.init(this)
        LogManager.i(TAG, "Application initialized")
    }

    companion object {
        private const val TAG = "JingRanApp"
    }
}
