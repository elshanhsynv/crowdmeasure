package com.example.crowdmeasure.di

import com.example.crowdmeasure.domain.repo.UploadRepository
import com.example.crowdmeasure.domain.repo.UploadRepositoryFirestore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindsModule {

    @Binds
    abstract fun bindUploadRepository(impl: UploadRepositoryFirestore): UploadRepository
}