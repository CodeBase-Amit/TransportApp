package com.example.transportapp.core.database.di

import android.content.Context
import androidx.room.Room
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.cursor.SyncCursorDao
import com.example.transportapp.core.database.dao.BillingDao
import com.example.transportapp.core.database.dao.ConsignmentDao
import com.example.transportapp.core.database.dao.MastersDao
import com.example.transportapp.core.database.dao.NumberingDao
import com.example.transportapp.core.database.dao.OrgDao
import com.example.transportapp.core.database.dao.TripDao
import com.example.transportapp.core.database.outbox.OutboxDao
import com.example.transportapp.core.database.seed.SeedVersionDao
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
    fun provideDatabase(@ApplicationContext context: Context): TransportDatabase =
        Room.databaseBuilder(context, TransportDatabase::class.java, TransportDatabase.NAME)
            .addMigrations(*TransportDatabase.ALL_MIGRATIONS)
            .build()

    @Provides
    fun provideOutboxDao(database: TransportDatabase): OutboxDao = database.outboxDao()

    @Provides
    fun provideSyncCursorDao(database: TransportDatabase): SyncCursorDao = database.syncCursorDao()

    @Provides
    fun provideSeedVersionDao(database: TransportDatabase): SeedVersionDao = database.seedVersionDao()

    @Provides
    fun provideOrgDao(database: TransportDatabase): OrgDao = database.orgDao()

    @Provides
    fun provideMastersDao(database: TransportDatabase): MastersDao = database.mastersDao()

    @Provides
    fun provideNumberingDao(database: TransportDatabase): NumberingDao = database.numberingDao()

    @Provides
    fun provideConsignmentDao(database: TransportDatabase): ConsignmentDao = database.consignmentDao()

    @Provides
    fun provideTripDao(database: TransportDatabase): TripDao = database.tripDao()

    @Provides
    fun provideBillingDao(database: TransportDatabase): BillingDao = database.billingDao()

    @Provides
    fun provideDashboardDao(database: TransportDatabase): com.example.transportapp.core.database.dao.DashboardDao = database.dashboardDao()

    @Provides
    fun provideReportsDao(database: TransportDatabase): com.example.transportapp.core.database.dao.ReportsDao = database.reportsDao()

    @Provides
    fun provideTemplateDao(database: TransportDatabase): com.example.transportapp.core.database.dao.TemplateDao = database.templateDao()

    @Provides
    fun provideSettingsDao(database: TransportDatabase): com.example.transportapp.core.database.dao.SettingsDao = database.settingsDao()
}
