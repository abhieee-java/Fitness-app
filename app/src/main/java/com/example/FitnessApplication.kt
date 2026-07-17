package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.FitnessRepository
import java.io.PrintWriter
import java.io.StringWriter

class FitnessApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var repository: FitnessRepository

    override fun onCreate() {
        super.onCreate()
        setupGlobalCrashHandler()

        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "fitness_database"
        )
        .fallbackToDestructiveMigration()
        .build()
        repository = FitnessRepository(database.fitnessDao())
    }

    private fun setupGlobalCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stackTraceString = sw.toString()

                Log.e("FitnessApplication", "CRITICAL: Uncaught exception in thread ${thread.name}: ${throwable.message}\n$stackTraceString")

                val prefs = getSharedPreferences("crash_diagnostics", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("has_crashed", true)
                    .putString("last_crash_message", throwable.message ?: "Unknown Exception")
                    .putString("last_crash_trace", stackTraceString)
                    .putLong("last_crash_time", System.currentTimeMillis())
                    .apply()
            } catch (e: Exception) {
                Log.e("FitnessApplication", "Failed to save crash diagnostics", e)
            } finally {
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable)
                } else {
                    System.exit(1)
                }
            }
        }
    }
}
