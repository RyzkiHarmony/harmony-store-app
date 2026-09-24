package com.harmony.tokoharmony.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.DriverManager

class Migration2To3Test {

    @Test
    fun migration2To3_createsTransactionTablesAndPreservesExistingData() {
        val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        val supportDb = JdbcSupportSQLiteDatabase(connection)

        // 1. Create Schema Version 2
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

        // 2. Insert V2 Sample Data
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

        // 3. Execute MIGRATION_2_3
        AppDatabase.MIGRATION_2_3.migrate(supportDb)

        // 4. Verify existing V2 records remain intact
        val rsUser = supportDb.executeQuery("SELECT user_id, display_name FROM users WHERE user_id = 'user-cashier-1'")
        assertTrue(rsUser.next())
        assertEquals("Kasir 1", rsUser.getString("display_name"))
        rsUser.close()

        val rsProd = supportDb.executeQuery("SELECT name, current_price FROM products WHERE product_id = 'prod-indomie'")
        assertTrue(rsProd.next())
        assertEquals("Indomie Goreng", rsProd.getString("name"))
        assertEquals(3500L, rsProd.getLong("current_price"))
        rsProd.close()

        // 5. Verify transactions table operational
        supportDb.execSQL(
            """
            INSERT INTO `transactions` (
                `transaction_id`, `transaction_number`, `transaction_status`, `payment_status`,
                `total_amount`, `change_amount`, `created_at`, `created_by`
            )
            VALUES ('tx-draft-1', 'TRX-20260924-001', 'DRAFT', 'UNPAID', 7000, 0, 1700000000000, 'user-cashier-1')
            """.trimIndent()
        )

        val rsTx = supportDb.executeQuery("SELECT transaction_number, transaction_status, total_amount FROM transactions WHERE transaction_id = 'tx-draft-1'")
        assertTrue(rsTx.next())
        assertEquals("TRX-20260924-001", rsTx.getString("transaction_number"))
        assertEquals("DRAFT", rsTx.getString("transaction_status"))
        assertEquals(7000L, rsTx.getLong("total_amount"))
        rsTx.close()

        // 6. Verify transaction_items table operational
        supportDb.execSQL(
            """
            INSERT INTO `transaction_items` (
                `item_id`, `transaction_id`, `product_id`, `product_name_snapshot`,
                `quantity`, `unit_price`, `subtotal`, `selling_unit`
            )
            VALUES ('item-1', 'tx-draft-1', 'prod-indomie', 'Indomie Goreng', 2, 3500, 7000, 'pcs')
            """.trimIndent()
        )

        val rsItem = supportDb.executeQuery("SELECT product_name_snapshot, quantity, unit_price, subtotal FROM transaction_items WHERE item_id = 'item-1'")
        assertTrue(rsItem.next())
        assertEquals("Indomie Goreng", rsItem.getString("product_name_snapshot"))
        assertEquals(2L, rsItem.getLong("quantity"))
        assertEquals(3500L, rsItem.getLong("unit_price"))
        assertEquals(7000L, rsItem.getLong("subtotal"))
        rsItem.close()

        // 7. Verify unique transaction_number constraint
        var duplicateTxNumberFailed = false
        try {
            supportDb.execSQL(
                """
                INSERT INTO `transactions` (
                    `transaction_id`, `transaction_number`, `transaction_status`, `payment_status`,
                    `total_amount`, `change_amount`, `created_at`, `created_by`
                )
                VALUES ('tx-draft-2', 'TRX-20260924-001', 'DRAFT', 'UNPAID', 3500, 0, 1700000000000, 'user-cashier-1')
                """.trimIndent()
            )
        } catch (e: Exception) {
            duplicateTxNumberFailed = true
        }
        assertTrue("Duplicate transaction_number must be rejected by unique index", duplicateTxNumberFailed)

        supportDb.close()
    }
}
