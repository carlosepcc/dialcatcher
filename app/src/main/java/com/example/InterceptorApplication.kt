package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.InterceptedCallRepository

class InterceptorApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { InterceptedCallRepository(database.interceptedCallDao()) }
}
