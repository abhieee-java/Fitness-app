package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.FitnessRepository

class FitnessApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var repository: FitnessRepository

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "fitness_database"
        ).build()
        repository = FitnessRepository(database.fitnessDao())
    }
}
