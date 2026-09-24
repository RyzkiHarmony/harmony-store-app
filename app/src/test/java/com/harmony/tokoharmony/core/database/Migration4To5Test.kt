package com.harmony.tokoharmony.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.DriverManager

class Migration4To5Test {

    @Test
    fun migration4To5_createsStockInsAndStockAdjustmentsTablesAndPreservesExistingData() {
        val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        val supportDb = JdbcSupportSQLiteDatabase(connection)

        // 1. Create Schema Version 4
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

        supportDb.execSQL(
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

        // 2. Insert V4 Sample Data
        supportDb.execSQL(
            """
            INSERT INTO `users` (`user_id`, `display_name`, `role`, `pin_hash`, `is_active`, `created_at`, `updated_at`)
            VALUES ('user-admin-1', 'Admin 1', 'ADMIN', NULL, 1, 1700000000000, 1700000000000)
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
            INSERT INTO `stock_movements` (
                `movement_id`, `product_id`, `movement_type`, `quantity_delta`,
                `reference_type`, `reference_id`, `reason`, `created_at`, `created_by`
            )
            VALUES ('mov-1', 'prod-indomie', 'INITIAL_STOCK', 100, NULL, NULL, 'Stok Awal', 1700000000000, 'user-admin-1')
            """.trimIndent()
        )

        // 3. Execute MIGRATION_4_5
        AppDatabase.MIGRATION_4_5.migrate(supportDb)

        // 4. Verify existing V4 data remains intact
        val rsMov = supportDb.executeQuery("SELECT quantity_delta, movement_type FROM stock_movements WHERE movement_id = 'mov-1'")
        assertTrue(rsMov.next())
        assertEquals(100L, rsMov.getLong("quantity_delta"))
        assertEquals("INITIAL_STOCK", rsMov.getString("movement_type"))
        rsMov.close()

        val rsProd = supportDb.executeQuery("SELECT name, current_price FROM products WHERE product_id = 'prod-indomie'")
        assertTrue(rsProd.next())
        assertEquals("Indomie Goreng", rsProd.getString("name"))
        rsProd.close()

        // 5. Verify stock_ins table operational
        supportDb.execSQL(
            """
            INSERT INTO `stock_ins` (
                `stock_in_id`, `product_id`, `purchase_quantity`, `purchase_unit`,
                `conversion_factor`, `stock_quantity`, `note`, `created_at`, `created_by`
            )
            VALUES ('stockin-1', 'prod-indomie', 4, 'Dus', 40, 160, 'Supplier A', 1700000000000, 'user-admin-1')
            """.trimIndent()
        )

        val rsIn = supportDb.executeQuery("SELECT purchase_quantity, stock_quantity, purchase_unit FROM stock_ins WHERE stock_in_id = 'stockin-1'")
        assertTrue(rsIn.next())
        assertEquals(4L, rsIn.getLong("purchase_quantity"))
        assertEquals(160L, rsIn.getLong("stock_quantity"))
        assertEquals("Dus", rsIn.getString("purchase_unit"))
        rsIn.close()

        // 6. Verify stock_adjustments table operational
        supportDb.execSQL(
            """
            INSERT INTO `stock_adjustments` (
                `adjustment_id`, `product_id`, `system_quantity`, `physical_quantity`,
                `difference`, `reason`, `note`, `created_at`, `created_by`
            )
            VALUES ('adj-1', 'prod-indomie', 100, 95, -5, 'Barang rusak', 'Bungkus bocor', 1700000000000, 'user-admin-1')
            """.trimIndent()
        )

        val rsAdj = supportDb.executeQuery("SELECT system_quantity, physical_quantity, difference, reason FROM stock_adjustments WHERE adjustment_id = 'adj-1'")
        assertTrue(rsAdj.next())
        assertEquals(100L, rsAdj.getLong("system_quantity"))
        assertEquals(95L, rsAdj.getLong("physical_quantity"))
        assertEquals(-5L, rsAdj.getLong("difference"))
        assertEquals("Barang rusak", rsAdj.getString("reason"))
        rsAdj.close()

        supportDb.close()
    }
}
