package com.example.meshtracker_v1.di

import android.content.Context
import com.example.meshtracker_v1.repository.MeshRepository
import com.example.meshtracker_v1.repository.MeshServiceRepository
import com.example.meshtracker_v1.service.MeshServiceManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindMeshRepository(impl: MeshServiceRepository): MeshRepository

    companion object {

        @Provides
        @Singleton
        fun provideMeshServiceManager(
            @ApplicationContext context: Context
        ): MeshServiceManager = MeshServiceManager.getInstance(context)
    }
}
