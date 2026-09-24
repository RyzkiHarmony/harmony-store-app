package com.harmony.tokoharmony.core.di

import android.content.Context
import androidx.room.Room
import com.harmony.tokoharmony.core.database.AppDatabase
import com.harmony.tokoharmony.core.database.dao.CategoryDao
import com.harmony.tokoharmony.core.database.dao.PriceHistoryDao
import com.harmony.tokoharmony.core.database.dao.ProductDao
import com.harmony.tokoharmony.core.database.dao.StockAdjustmentDao
import com.harmony.tokoharmony.core.database.dao.StockInDao
import com.harmony.tokoharmony.core.database.dao.StockMovementDao
import com.harmony.tokoharmony.core.database.dao.SyncQueueDao
import com.harmony.tokoharmony.core.database.dao.TransactionDao
import com.harmony.tokoharmony.core.database.dao.TransactionItemDao
import com.harmony.tokoharmony.core.database.dao.UserDao
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
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7
            )
            .build()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideProductDao(database: AppDatabase): ProductDao {
        return database.productDao()
    }

    @Provides
    fun providePriceHistoryDao(database: AppDatabase): PriceHistoryDao {
        return database.priceHistoryDao()
    }

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideTransactionItemDao(database: AppDatabase): TransactionItemDao {
        return database.transactionItemDao()
    }

    @Provides
    fun provideStockMovementDao(database: AppDatabase): StockMovementDao {
        return database.stockMovementDao()
    }

    @Provides
    fun provideStockInDao(database: AppDatabase): StockInDao {
        return database.stockInDao()
    }

    @Provides
    fun provideStockAdjustmentDao(database: AppDatabase): StockAdjustmentDao {
        return database.stockAdjustmentDao()
    }

    @Provides
    fun provideSyncQueueDao(database: AppDatabase): SyncQueueDao {
        return database.syncQueueDao()
    }

    @Provides
    fun provideDigitalTransactionDao(database: AppDatabase): com.harmony.tokoharmony.core.database.dao.DigitalTransactionDao {
        return database.digitalTransactionDao()
    }
}
