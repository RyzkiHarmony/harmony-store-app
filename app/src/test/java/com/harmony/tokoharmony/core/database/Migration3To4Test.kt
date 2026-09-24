package com.harmony.tokoharmony.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.DriverManager

class Migration3To4Test {

    @Test
    fun migration3To4_createsStockMovementsTableAndPreservesExistingData() {
        val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        val supportDb = JdbcSupportSQLiteDatabase(connection)

        // 1. Create Schema Version 3
        supportDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `users` (
                `user_id` TEXT NOT NULL,
                `display_name` TEXT NOT NULL,
                `role` TEXT NOT NULL,
                `pin_hash` TEXT,
                `is_active` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`user_id`)
            )
            """.trimIndent()
        )

        supportDb.execSQL(
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

        supportDb.execSQL(
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

        supportDb.execSQL(
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

        supportDb.execSQL(
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

        supportDb.execSQL(
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

        // 2. Insert V3 Sample Data
        supportDb.execSQL(
            """
            INSERT INTO `users` (`user_id`, `display_name`, `role`, `pin_hash`, `is_active`, `created_at`, `updated_at`)
            VALUES ('user-cashier-1', 'Kasir 1', 'CASHIER', NULL, 1, 1700000000000, 1700000000000)
            """.trimIndent()
        )

        supportDb.execSQL(
            """
            INSERT INTO `categories` (`category_id`, `name`, `is_active`, `created_at`, `updated_at`)
            VALUES ('cat-sembako', 'Sembako', 1, 1700000000000, 1700000000000)
            """.trimIndent()
        )

        supportDb.execSQL(
            """
            INSERT INTO `products` (
                `product_id`, `category_id`, `name`, `barcode`, `product_kind`,
                `pricing_method`, `quantity_type`, `selling_unit`, `stock_unit`,
                `current_price`, `is_active`, `created_at`, `updated_at`
            )
            VALUES ('prod-indomie', 'cat-sembako', 'Indomie Goreng', '8991234567', 'PHYSICAL', 'PER_UNIT', 'COUNT', 'pcs', 'pcs', 3500, 1, 1700000000000, 1700000000000)
            """.trimIndent()
        )

        supportDb.execSQL(
            """
            INSERT INTO `transactions` (
                `transaction_id`, `transaction_number`, `transaction_status`, `payment_status`,
                `total_amount`, `change_amount`, `created_at`, `created_by`
            )
            VALUES ('tx-1', 'TRX-20260924-001', 'DRAFT', 'UNPAID', 7000, 0, 1700000000000, 'user-cashier-1')
            """.trimIndent()
        )

        // 3. Execute MIGRATION_3_4
        AppDatabase.MIGRATION_3_4.migrate(supportDb)

        // 4. Verify existing V3 data remains intact
        val rsTx = supportDb.executeQuery("SELECT transaction_number, total_amount FROM transactions WHERE transaction_id = 'tx-1'")
        assertTrue(rsTx.next())
        assertEquals("TRX-20260924-001", rsTx.getString("transaction_number"))
        assertEquals(7000L, rsTx.getLong("total_amount"))
        rsTx.close()

        val rsProd = supportDb.executeQuery("SELECT name, current_price FROM products WHERE product_id = 'prod-indomie'")
        assertTrue(rsProd.next())
        assertEquals("Indomie Goreng", rsProd.getString("name"))
        rsProd.close()

        // 5. Verify stock_movements table operational
        supportDb.execSQL(
            """
            INSERT INTO `stock_movements` (
                `movement_id`, `product_id`, `movement_type`, `quantity_delta`,
                `reference_type`, `reference_id`, `reason`, `created_at`, `created_by`
            )
            VALUES ('mov-1', 'prod-indomie', 'INITIAL_STOCK', 100, NULL, NULL, 'Stok Awal', 1700000000000, 'user-cashier-1')
            """.trimIndent()
        )

        supportDb.execSQL(
            """
            INSERT INTO `stock_movements` (
                `movement_id`, `product_id`, `movement_type`, `quantity_delta`,
                `reference_type`, `reference_id`, `reason`, `created_at`, `created_by`
            )
            VALUES ('mov-2', 'prod-indomie', 'SALE', -5, 'TRANSACTION', 'tx-1', 'Penjualan TRX-20260924-001', 1700000000000, 'user-cashier-1')
            """.trimIndent()
        )

        val rsSum = supportDb.executeQuery("SELECT SUM(quantity_delta) as current_stock FROM stock_movements WHERE product_id = 'prod-indomie'")
        assertTrue(rsSum.next())
        assertEquals(95L, rsSum.getLong("current_stock"))
        rsSum.close()

        supportDb.close()
    }
}
