package com.example.transportapp.data.transport.di

import com.example.transportapp.core.network.ApiClient
import com.example.transportapp.core.network.AuthApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * S23 — the network bindings (§14: one typed HTTP boundary). The base URL points at the
 * test backend; the emulator reaches the host at 10.0.2.2. The debug manifest allows
 * cleartext to that host only (D62).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    const val BASE_URL = "http://10.0.2.2:3000/"

    @Provides
    @Singleton
    fun tokenProvider(store: com.example.transportapp.core.datastore.session.SessionStore): com.example.transportapp.core.network.TokenProvider =
        com.example.transportapp.core.network.TokenProvider { store.token() }

    @Provides
    @Singleton
    fun apiClient(tokenProvider: com.example.transportapp.core.network.TokenProvider): ApiClient =
        ApiClient(baseUrl = BASE_URL, tokenProvider = tokenProvider)

    @Provides
    @Singleton
    fun authApi(client: ApiClient): AuthApi = AuthApi(client)

    @Provides
    @Singleton
    fun numberingApi(client: ApiClient): com.example.transportapp.core.network.NumberingApi = com.example.transportapp.core.network.NumberingApi(client)

    @Provides
    @Singleton
    fun mastersApi(client: ApiClient): com.example.transportapp.core.network.MastersApi = com.example.transportapp.core.network.MastersApi(client)
}

