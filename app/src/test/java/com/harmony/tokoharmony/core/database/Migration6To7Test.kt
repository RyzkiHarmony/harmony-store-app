package com.harmony.tokoharmony.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.DriverManager

class Migration6To7Test {

    @Test
    fun migration6To7_createsDigitalTransactionsTableAndPreservesExistingData() {
        val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        val supportDb = JdbcSupportSQLiteDatabase(connection)

        // 1. Create Schema Version 6
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
            CREATE TABLE IF NOT EXISTS `transactions` (
                `transaction_id` TEXT NOT NULL,
                `transaction_number` TEXT NOT NULL,
                `transaction_status` TEXT NOT NULL,
                `payment_status` TEXT NOT NULL,
                `payment_method` TEXT,
                `total_amount` INTEGER NOT NULL,
                `amount_received` INTEGER NOT NULL,
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

        // 2. Insert V6 Sample Data
        supportDb.execSQL(
            """
            INSERT INTO `users` (`user_id`, `display_name`, `role`, `pin_hash`, `is_active`, `created_at`, `updated_at`)
            VALUES ('user-admin-1', 'Admin 1', 'ADMIN', NULL, 1, 1700000000000, 1700000000000)
            """.trimIndent()
        )

        supportDb.execSQL(
            """
            INSERT INTO `categories` (`category_id`, `name`, `is_active`, `created_at`, `updated_at`)
            VALUES ('cat-digital', 'Produk Digital', 1, 1700000000000, 1700000000000)
            """.trimIndent()
        )

        supportDb.execSQL(
            """
            INSERT INTO `products` (
                `product_id`, `category_id`, `name`, `barcode`, `product_kind`,
                `pricing_method`, `quantity_type`, `selling_unit`, `stock_unit`,
                `current_price`, `is_active`, `created_at`, `updated_at`
            )
            VALUES ('prod-pulsa', 'cat-digital', 'Pulsa 50k', NULL, 'DIGITAL', 'PER_UNIT', 'COUNT', 'layanan', 'layanan', 52000, 1, 1700000000000, 1700000000000)
            """.trimIndent()
        )

        supportDb.execSQL(
            """
            INSERT INTO `transactions` (
                `transaction_id`, `transaction_number`, `transaction_status`, `payment_status`,
                `payment_method`, `total_amount`, `amount_received`, `change_amount`,
                `created_at`, `completed_at`, `cancelled_at`, `created_by`
            )
            VALUES ('tx-100', 'TRX-100', 'COMPLETED', 'PAID', 'CASH', 52000, 60000, 8000, 1700000000000, 1700000000000, NULL, 'user-admin-1')
            """.trimIndent()
        )

        supportDb.execSQL(
            """
            INSERT INTO `transaction_items` (
                `item_id`, `transaction_id`, `product_id`, `product_name_snapshot`,
                `quantity`, `unit_price`, `subtotal`, `selling_unit`
            )
            VALUES ('item-100', 'tx-100', 'prod-pulsa', 'Pulsa 50k', 1, 52000, 52000, 'layanan')
            """.trimIndent()
        )

        // 3. Execute MIGRATION_6_7
        AppDatabase.MIGRATION_6_7.migrate(supportDb)

        // 4. Verify existing V6 data remains intact
        val rsProd = supportDb.executeQuery("SELECT name, current_price, product_kind FROM products WHERE product_id = 'prod-pulsa'")
        assertTrue(rsProd.next())
        assertEquals("Pulsa 50k", rsProd.getString("name"))
        assertEquals(52000L, rsProd.getLong("current_price"))
        assertEquals("DIGITAL", rsProd.getString("product_kind"))
        rsProd.close()

        val rsTx = supportDb.executeQuery("SELECT total_amount, payment_method FROM transactions WHERE transaction_id = 'tx-100'")
        assertTrue(rsTx.next())
        assertEquals(52000L, rsTx.getLong("total_amount"))
        assertEquals("CASH", rsTx.getString("payment_method"))
        rsTx.close()

        // 5. Verify digital_transactions table is created and operational
        supportDb.execSQL(
            """
            INSERT INTO `digital_transactions` (
                `digital_transaction_id`, `transaction_item_id`, `service_type`,
                `customer_number`, `nominal`, `provider_reference`, `status`, `created_at`
            )
            VALUES ('dt-100', 'item-100', 'PULSA', '081234567890', 50000, 'REF-SN-12345', 'SUCCESS', 1700000000000)
            """.trimIndent()
        )

        val rsDt = supportDb.executeQuery("SELECT service_type, customer_number, nominal, status FROM digital_transactions WHERE digital_transaction_id = 'dt-100'")
        assertTrue(rsDt.next())
        assertEquals("PULSA", rsDt.getString("service_type"))
        assertEquals("081234567890", rsDt.getString("customer_number"))
        assertEquals(50000L, rsDt.getLong("nominal"))
        assertEquals("SUCCESS", rsDt.getString("status"))
        rsDt.close()

        supportDb.close()
    }
}
