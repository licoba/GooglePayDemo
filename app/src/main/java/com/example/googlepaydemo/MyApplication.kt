package com.example.googlepaydemo

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

/**
 * @author licoba
 * @date 2023/2/9 20:53
 * @email licoba@qq.com
 * @desc
 */
class MyApplication : Application() {
    companion object{
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context =applicationContext
    }
}
