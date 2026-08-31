package com.example.transportapp.data.transport.di

import com.example.transportapp.data.transport.account.AccountDataRepository
import com.example.transportapp.data.transport.company.CompanyRepository
import com.example.transportapp.data.transport.company.CompanyRepositoryImpl
import com.example.transportapp.data.transport.dashboard.DashboardRepository
import com.example.transportapp.data.transport.dashboard.DashboardRepositoryImpl
import com.example.transportapp.data.transport.consignment.CaseFileRepository
import com.example.transportapp.data.transport.consignment.CaseFileRepositoryImpl
import com.example.transportapp.data.transport.consignment.ConsignmentRepository
import com.example.transportapp.data.transport.consignment.ConsignmentRepositoryImpl
import com.example.transportapp.data.transport.consignment.RegisterRepository
import com.example.transportapp.data.transport.consignment.RegisterRepositoryImpl
import com.example.transportapp.data.transport.billing.BillingRepository
import com.example.transportapp.data.transport.billing.BillingRepositoryImpl
import com.example.transportapp.data.transport.masters.MastersRepository
import com.example.transportapp.data.transport.masters.MastersRepositoryImpl
import com.example.transportapp.data.transport.reports.ReportsRepository
import com.example.transportapp.data.transport.reports.ReportsRepositoryImpl
import com.example.transportapp.data.transport.numbering.NumberingRepository
import com.example.transportapp.data.transport.numbering.NumberingRepositoryImpl
import com.example.transportapp.data.transport.rate.RateCardRepository
import com.example.transportapp.data.transport.rate.RateCardRepositoryImpl
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.session.SessionRepositoryImpl
import com.example.transportapp.data.transport.tracking.StatusRepository
import com.example.transportapp.data.transport.tracking.StatusRepositoryImpl
import com.example.transportapp.data.transport.trip.TripRepository
import com.example.transportapp.data.transport.trip.TripRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Repository bindings grow per sprint (Phase2.md §3.1) — one `@Binds` per aggregate. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindCompanyRepository(impl: CompanyRepositoryImpl): CompanyRepository

    @Binds
    @Singleton
    abstract fun bindMastersRepository(impl: MastersRepositoryImpl): MastersRepository

    @Binds
    @Singleton
    abstract fun bindRateCardRepository(impl: RateCardRepositoryImpl): RateCardRepository

    @Binds
    @Singleton
    abstract fun bindNumberingRepository(impl: NumberingRepositoryImpl): NumberingRepository

    @Binds
    @Singleton
    abstract fun bindConsignmentRepository(impl: ConsignmentRepositoryImpl): ConsignmentRepository

    @Binds
    @Singleton
    abstract fun bindRegisterRepository(impl: RegisterRepositoryImpl): RegisterRepository

    @Binds
    @Singleton
    abstract fun bindCaseFileRepository(impl: CaseFileRepositoryImpl): CaseFileRepository

    @Binds
    @Singleton
    abstract fun bindTripRepository(impl: TripRepositoryImpl): TripRepository

    @Binds
    @Singleton
    abstract fun bindStatusRepository(impl: StatusRepositoryImpl): StatusRepository

    @Binds
    @Singleton
    abstract fun bindBillingRepository(impl: BillingRepositoryImpl): BillingRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindReportsRepository(impl: ReportsRepositoryImpl): ReportsRepository
}
