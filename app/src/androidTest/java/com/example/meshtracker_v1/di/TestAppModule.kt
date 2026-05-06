package com.example.meshtracker_v1.di

import com.example.meshtracker_v1.fake.FakeMeshRepository
import com.example.meshtracker_v1.repository.MeshRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Testowy moduł Hilt.
 * Zastępuje [AppModule] — wiąże [MeshRepository] z [FakeMeshRepository]
 * zamiast z [com.example.meshtracker_v1.repository.MeshServiceRepository].
 *
 * Dzięki temu każdy test korzysta z FakeMeshRepository bez połączenia z prawdziwym serwisem.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces   = [AppModule::class]
)
abstract class TestAppModule {

    @Binds
    @Singleton
    abstract fun bindMeshRepository(fake: FakeMeshRepository): MeshRepository
}
