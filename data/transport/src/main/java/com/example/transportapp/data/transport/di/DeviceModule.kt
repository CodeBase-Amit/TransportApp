package com.example.transportapp.data.transport.di

import com.example.transportapp.data.transport.numbering.DeviceIdProvider
import com.example.transportapp.data.transport.numbering.AndroidDeviceIdProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DeviceModule {

    @Binds
    @Singleton
    abstract fun bindDeviceIdProvider(impl: AndroidDeviceIdProvider): DeviceIdProvider
}
