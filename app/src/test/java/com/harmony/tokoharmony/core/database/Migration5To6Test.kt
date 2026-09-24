package com.harmony.tokoharmony.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.DriverManager

class Migration5To6Test {

    @Test
    fun migration5To6_createsSyncQueueTableAndPreservesExistingData() {
        val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        val supportDb = JdbcSupportSQLiteDatabase(connection)

        // 1. Create Schema Version 5
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

        // 2. Insert V5 Sample Data
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

        // 3. Execute MIGRATION_5_6
        AppDatabase.MIGRATION_5_6.migrate(supportDb)

        // 4. Verify existing V5 data remains intact
        val rsProd = supportDb.executeQuery("SELECT name, current_price FROM products WHERE product_id = 'prod-indomie'")
        assertTrue(rsProd.next())
        assertEquals("Indomie Goreng", rsProd.getString("name"))
        assertEquals(3500L, rsProd.getLong("current_price"))
        rsProd.close()

        // 5. Verify sync_queue table operational
        supportDb.execSQL(
            """
            INSERT INTO `sync_queue` (
                `queue_id`, `entity_type`, `entity_id`, `operation`,
                `payload`, `status`, `retry_count`, `last_error`, `created_at`, `synced_at`
            )
            VALUES ('queue-1', 'PRODUCT', 'prod-indomie', 'CREATE', '{"productId":"prod-indomie"}', 'PENDING', 0, NULL, 1700000000000, NULL)
            """.trimIndent()
        )

        val rsQueue = supportDb.executeQuery("SELECT entity_type, entity_id, operation, status, retry_count FROM sync_queue WHERE queue_id = 'queue-1'")
        assertTrue(rsQueue.next())
        assertEquals("PRODUCT", rsQueue.getString("entity_type"))
        assertEquals("prod-indomie", rsQueue.getString("entity_id"))
        assertEquals("CREATE", rsQueue.getString("operation"))
        assertEquals("PENDING", rsQueue.getString("status"))
        assertEquals(0, rsQueue.getInt("retry_count"))
        rsQueue.close()

        supportDb.close()
    }
}
