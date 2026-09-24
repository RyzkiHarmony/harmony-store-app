package com.harmony.tokoharmony.core.di

import com.harmony.tokoharmony.data.repository.AuthRepositoryImpl
import com.harmony.tokoharmony.data.repository.CategoryRepositoryImpl
import com.harmony.tokoharmony.data.repository.PriceRepositoryImpl
import com.harmony.tokoharmony.data.repository.ProductRepositoryImpl
import com.harmony.tokoharmony.data.repository.TransactionRepositoryImpl
import com.harmony.tokoharmony.domain.repository.AuthRepository
import com.harmony.tokoharmony.domain.repository.CategoryRepository
import com.harmony.tokoharmony.domain.repository.PriceRepository
import com.harmony.tokoharmony.domain.repository.ProductRepository
import com.harmony.tokoharmony.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.harmony.tokoharmony.data.repository.StockRepositoryImpl
import com.harmony.tokoharmony.data.repository.SyncRepositoryImpl
import com.harmony.tokoharmony.domain.repository.StockRepository
import com.harmony.tokoharmony.domain.repository.SyncRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        productRepositoryImpl: ProductRepositoryImpl
    ): ProductRepository

    @Binds
    @Singleton
    abstract fun bindPriceRepository(
        priceRepositoryImpl: PriceRepositoryImpl
    ): PriceRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindStockRepository(
        stockRepositoryImpl: StockRepositoryImpl
    ): StockRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        syncRepositoryImpl: SyncRepositoryImpl
    ): SyncRepository

    @Binds
    @Singleton
    abstract fun bindDigitalTransactionRepository(
        digitalTransactionRepositoryImpl: com.harmony.tokoharmony.data.repository.DigitalTransactionRepositoryImpl
    ): com.harmony.tokoharmony.domain.repository.DigitalTransactionRepository
}
