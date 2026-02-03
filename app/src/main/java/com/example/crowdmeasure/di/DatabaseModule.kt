package com.example.crowdmeasure.di

import android.content.Context
import androidx.room.Room
import com.example.crowdmeasure.data.db.AppDatabase
import com.example.crowdmeasure.data.db.MeasurementDao
import com.example.crowdmeasure.data.db.Migrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDb(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "crowdmeasure.db")
            .addMigrations(*Migrations.ALL)
            .fallbackToDestructiveMigrationOnDowngrade(false)
            .build()
    }

    @Provides
    fun provideDao(db: AppDatabase): MeasurementDao = db.measurementDao()
}