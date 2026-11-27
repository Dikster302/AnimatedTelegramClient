package com.dikster.specialgram

import android.app.Application

class SpecialgramApp : Application() {
    lateinit var td: TdLibClient
        private set

    override fun onCreate() {
        super.onCreate()
        td = TdLibClient(this)
    }
}
