package com.example.transportapp.core.datastore.di

import com.example.transportapp.core.datastore.session.AuthTokenProvider
import com.example.transportapp.core.datastore.session.MockAuthTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The identity seam binding (Phase2.md D5). When real auth lands, only this binding changes —
 * no caller of [AuthTokenProvider] may need to change with it.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {

    @Binds
    @Singleton
    abstract fun bindAuthTokenProvider(impl: MockAuthTokenProvider): AuthTokenProvider
}
