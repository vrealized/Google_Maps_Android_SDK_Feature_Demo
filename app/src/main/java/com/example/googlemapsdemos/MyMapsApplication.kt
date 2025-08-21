package com.example.googlemapsdemos

import android.app.Application
import android.util.Log
import com.google.android.libraries.places.api.Places

/**
 * 自定义的Application类，用于全局初始化。
 * 这是初始化Places SDK等单例库的最佳实践。
 */
class MyMapsApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 获取API密钥
        val apiKey = BuildConfig.MAPS_API_KEY

        // 【核心修正】我们不再需要检查apiKey是否等于占位符。
        // 一个更健壮的检查是，确认这个密钥不是空的。
        if (apiKey.isEmpty()) {
            // 如果密钥是空的，我们可以在开发阶段通过日志来警告开发者。
            // 在生产环境中，这通常意味着配置错误。
            Log.e("MyMapsApplication", "API Key is not set in local.properties. Maps and Places services will not work.")
            return // 如果没有密钥，就不进行初始化
        }

        // 在应用启动时，全局初始化Places SDK一次
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }
    }
}