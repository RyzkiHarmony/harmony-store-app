package com.harmony.tokoharmony.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.harmony.tokoharmony.core.database.dao.CategoryDao
import com.harmony.tokoharmony.core.database.dao.DigitalTransactionDao
import com.harmony.tokoharmony.core.database.dao.PriceHistoryDao
import com.harmony.tokoharmony.core.database.dao.ProductDao
import com.harmony.tokoharmony.core.database.dao.SyncQueueDao
import com.harmony.tokoharmony.core.database.dao.StockAdjustmentDao
import com.harmony.tokoharmony.core.database.dao.StockInDao
import com.harmony.tokoharmony.core.database.dao.StockMovementDao
import com.harmony.tokoharmony.core.database.dao.TransactionDao
import com.harmony.tokoharmony.core.database.dao.TransactionItemDao
import com.harmony.tokoharmony.core.database.dao.UserDao
import com.harmony.tokoharmony.core.database.entity.CategoryEntity
import com.harmony.tokoharmony.core.database.entity.DigitalTransactionEntity
import com.harmony.tokoharmony.core.database.entity.PriceHistoryEntity
import com.harmony.tokoharmony.core.database.entity.ProductEntity
import com.harmony.tokoharmony.core.database.entity.StockAdjustmentEntity
import com.harmony.tokoharmony.core.database.entity.StockInEntity
import com.harmony.tokoharmony.core.database.entity.StockMovementEntity
import com.harmony.tokoharmony.core.database.entity.SyncQueueEntity
import com.harmony.tokoharmony.core.database.entity.TransactionEntity
import com.harmony.tokoharmony.core.database.entity.TransactionItemEntity
import com.harmony.tokoharmony.core.database.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        PriceHistoryEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        StockMovementEntity::class,
        StockInEntity::class,
        StockAdjustmentEntity::class,
        SyncQueueEntity::class,
        DigitalTransactionEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionItemDao(): TransactionItemDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun stockInDao(): StockInDao
    abstract fun stockAdjustmentDao(): StockAdjustmentDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun digitalTransactionDao(): DigitalTransactionDao

    companion object {
        const val DATABASE_NAME = "tokoharmony.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create categories table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `category_id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `is_active` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`category_id`)
                    )
                    """.trimIndent()
                )

                // Create products table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `products` (
                        `product_id` TEXT NOT NULL,
                        `category_id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `barcode` TEXT,
                        `product_kind` TEXT NOT NULL,
                        `pricing_method` TEXT NOT NULL,
                        `quantity_type` TEXT NOT NULL,
                        `selling_unit` TEXT NOT NULL,
                        `stock_unit` TEXT NOT NULL,
                        `purchase_unit` TEXT,
                        `purchase_conversion_factor` INTEGER,
                        `current_price` INTEGER NOT NULL,
                        `minimum_stock` INTEGER,
                        `is_active` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`product_id`),
                        FOREIGN KEY(`category_id`) REFERENCES `categories`(`category_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )

                // Create product indices
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_products_barcode` ON `products` (`barcode`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_name` ON `products` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_category_id` ON `products` (`category_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_is_active` ON `products` (`is_active`)")

                // Create price_history table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `price_history` (
                        `history_id` TEXT NOT NULL,
                        `product_id` TEXT NOT NULL,
                        `old_price` INTEGER NOT NULL,
                        `new_price` INTEGER NOT NULL,
                        `changed_at` INTEGER NOT NULL,
                        `changed_by` TEXT NOT NULL,
                        PRIMARY KEY(`history_id`),
                        FOREIGN KEY(`product_id`) REFERENCES `products`(`product_id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`changed_by`) REFERENCES `users`(`user_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )

                // Create price_history indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_price_history_product_id` ON `price_history` (`product_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_price_history_changed_by` ON `price_history` (`changed_by`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_price_history_changed_at` ON `price_history` (`changed_at`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create transactions table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transactions` (
                        `transaction_id` TEXT NOT NULL,
                        `transaction_number` TEXT NOT NULL,
                        `transaction_status` TEXT NOT NULL,
                        `payment_status` TEXT NOT NULL,
                        `payment_method` TEXT,
                        `total_amount` INTEGER NOT NULL,
                        `amount_received` INTEGER,
                        `change_amount` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `completed_at` INTEGER,
                        `cancelled_at` INTEGER,
                        `created_by` TEXT NOT NULL,
                        `cancelled_by` TEXT,
                        `cancellation_reason` TEXT,
                        PRIMARY KEY(`transaction_id`),
                        FOREIGN KEY(`created_by`) REFERENCES `users`(`user_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`cancelled_by`) REFERENCES `users`(`user_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )

                // Create transaction indices
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_transaction_number` ON `transactions` (`transaction_number`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_transaction_status` ON `transactions` (`transaction_status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_created_by` ON `transactions` (`created_by`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_cancelled_by` ON `transactions` (`cancelled_by`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_created_at` ON `transactions` (`created_at`)")

                // Create transaction_items table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transaction_items` (
                        `item_id` TEXT NOT NULL,
                        `transaction_id` TEXT NOT NULL,
                        `product_id` TEXT NOT NULL,
                        `product_name_snapshot` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `unit_price` INTEGER NOT NULL,
                        `subtotal` INTEGER NOT NULL,
                        `selling_unit` TEXT NOT NULL,
                        PRIMARY KEY(`item_id`),
                        FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`transaction_id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`product_id`) REFERENCES `products`(`product_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )

                // Create transaction_items indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_items_transaction_id` ON `transaction_items` (`transaction_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_items_product_id` ON `transaction_items` (`product_id`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create stock_movements table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `stock_movements` (
                        `movement_id` TEXT NOT NULL,
                        `product_id` TEXT NOT NULL,
                        `movement_type` TEXT NOT NULL,
                        `quantity_delta` INTEGER NOT NULL,
                        `reference_type` TEXT,
                        `reference_id` TEXT,
                        `reason` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `created_by` TEXT NOT NULL,
                        PRIMARY KEY(`movement_id`),
                        FOREIGN KEY(`product_id`) REFERENCES `products`(`product_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`created_by`) REFERENCES `users`(`user_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )

                // Create stock_movements indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_product_id` ON `stock_movements` (`product_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_movement_type` ON `stock_movements` (`movement_type`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_created_by` ON `stock_movements` (`created_by`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_created_at` ON `stock_movements` (`created_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_reference_id` ON `stock_movements` (`reference_id`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create stock_ins table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `stock_ins` (
                        `stock_in_id` TEXT NOT NULL,
                        `product_id` TEXT NOT NULL,
                        `purchase_quantity` INTEGER NOT NULL,
                        `purchase_unit` TEXT NOT NULL,
                        `conversion_factor` INTEGER NOT NULL,
                        `stock_quantity` INTEGER NOT NULL,
                        `note` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `created_by` TEXT NOT NULL,
                        PRIMARY KEY(`stock_in_id`),
                        FOREIGN KEY(`product_id`) REFERENCES `products`(`product_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`created_by`) REFERENCES `users`(`user_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_ins_product_id` ON `stock_ins` (`product_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_ins_created_by` ON `stock_ins` (`created_by`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_ins_created_at` ON `stock_ins` (`created_at`)")

                // Create stock_adjustments table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `stock_adjustments` (
                        `adjustment_id` TEXT NOT NULL,
                        `product_id` TEXT NOT NULL,
                        `system_quantity` INTEGER NOT NULL,
                        `physical_quantity` INTEGER NOT NULL,
                        `difference` INTEGER NOT NULL,
                        `reason` TEXT NOT NULL,
                        `note` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `created_by` TEXT NOT NULL,
                        PRIMARY KEY(`adjustment_id`),
                        FOREIGN KEY(`product_id`) REFERENCES `products`(`product_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`created_by`) REFERENCES `users`(`user_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_adjustments_product_id` ON `stock_adjustments` (`product_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_adjustments_created_by` ON `stock_adjustments` (`created_by`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_adjustments_created_at` ON `stock_adjustments` (`created_at`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create sync_queue table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_queue` (
                        `queue_id` TEXT NOT NULL,
                        `entity_type` TEXT NOT NULL,
                        `entity_id` TEXT NOT NULL,
                        `operation` TEXT NOT NULL,
                        `payload` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `retry_count` INTEGER NOT NULL,
                        `last_error` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `synced_at` INTEGER,
                        PRIMARY KEY(`queue_id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_status` ON `sync_queue` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_created_at` ON `sync_queue` (`created_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_entity_type_entity_id` ON `sync_queue` (`entity_type`, `entity_id`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create digital_transactions table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `digital_transactions` (
                        `digital_transaction_id` TEXT NOT NULL PRIMARY KEY,
                        `transaction_item_id` TEXT NOT NULL,
                        `service_type` TEXT NOT NULL,
                        `customer_number` TEXT NOT NULL,
                        `nominal` INTEGER NOT NULL,
                        `provider_reference` TEXT,
                        `status` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        FOREIGN KEY(`transaction_item_id`) REFERENCES `transaction_items`(`item_id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_digital_transactions_transaction_item_id` ON `digital_transactions` (`transaction_item_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_digital_transactions_status` ON `digital_transactions` (`status`)")
            }
        }
    }
}
