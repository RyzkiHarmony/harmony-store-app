package com.harmony.tokoharmony.core.database

import android.content.ContentValues
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.Locale

class JdbcSupportSQLiteDatabase(val connection: Connection) : SupportSQLiteDatabase {
    override fun execSQL(sql: String) {
        connection.createStatement().use { stmt ->
            stmt.execute(sql)
        }
    }

    override fun execSQL(sql: String, bindArgs: Array<out Any?>) {
        connection.prepareStatement(sql).use { stmt ->
            bindArgs.forEachIndexed { index, arg ->
                stmt.setObject(index + 1, arg)
            }
            stmt.execute()
        }
    }

    fun executeQuery(sql: String): ResultSet {
        val stmt = connection.createStatement()
        return stmt.executeQuery(sql)
    }

    override fun beginTransaction() {}
    override fun beginTransactionNonExclusive() {}
    override fun beginTransactionWithListener(transactionListener: android.database.sqlite.SQLiteTransactionListener) {}
    override fun beginTransactionWithListenerNonExclusive(transactionListener: android.database.sqlite.SQLiteTransactionListener) {}
    override fun endTransaction() {}
    override fun setTransactionSuccessful() {}
    override fun inTransaction(): Boolean = false
    override val isDbLockedByCurrentThread: Boolean get() = false
    override fun yieldIfContendedSafely(): Boolean = false
    override fun yieldIfContendedSafely(sleepAfterYieldDelayMillis: Long): Boolean = false
    override var version: Int = 1
    override val maximumSize: Long get() = 0L
    override fun setMaximumSize(numBytes: Long): Long = 0L
    override var pageSize: Long = 4096L
    override fun compileStatement(sql: String): SupportSQLiteStatement = throw UnsupportedOperationException()
    override fun query(query: String) = throw UnsupportedOperationException()
    override fun query(query: String, bindArgs: Array<out Any?>) = throw UnsupportedOperationException()
    override fun query(query: SupportSQLiteQuery) = throw UnsupportedOperationException()
    override fun query(query: SupportSQLiteQuery, cancellationSignal: android.os.CancellationSignal?) = throw UnsupportedOperationException()
    override fun insert(table: String, conflictAlgorithm: Int, values: ContentValues): Long = throw UnsupportedOperationException()
    override fun delete(table: String, whereClause: String?, whereArgs: Array<out Any?>?): Int = throw UnsupportedOperationException()
    override fun update(table: String, conflictAlgorithm: Int, values: ContentValues, whereClause: String?, whereArgs: Array<out Any?>?): Int = throw UnsupportedOperationException()
    override fun needUpgrade(newVersion: Int): Boolean = true
    override val path: String? get() = ":memory:"
    override val isReadOnly: Boolean get() = false
    override val isOpen: Boolean get() = !connection.isClosed
    override fun close() = connection.close()
    override val attachedDbs: List<android.util.Pair<String, String>>? get() = emptyList()
    override val isWriteAheadLoggingEnabled: Boolean get() = false
    override fun enableWriteAheadLogging(): Boolean = false
    override fun disableWriteAheadLogging() {}
    override fun setForeignKeyConstraintsEnabled(enabled: Boolean) {
        execSQL("PRAGMA foreign_keys = ${if (enabled) "ON" else "OFF"}")
    }
    override fun setLocale(locale: Locale) {}
    override fun setMaxSqlCacheSize(cacheSize: Int) {}
    override val isDatabaseIntegrityOk: Boolean get() = true
}

class Migration1To2Test {

    @Test
    fun migration1To2_createsNewTablesAndPreservesV1UserData() {
        val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        val supportDb = JdbcSupportSQLiteDatabase(connection)

        // 1. Create Schema Version 1
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

        // 2. Insert V1 sample data
        supportDb.execSQL(
            """
            INSERT INTO `users` (`user_id`, `display_name`, `role`, `pin_hash`, `is_active`, `created_at`, `updated_at`)
            VALUES ('admin-test', 'Owner Toko', 'ADMIN', 'salt:hash', 1, 1700000000000, 1700000000000)
            """.trimIndent()
        )

        // 3. Execute MIGRATION_1_2
        AppDatabase.MIGRATION_1_2.migrate(supportDb)

        // 4. Verify V1 users table preserved
        val rsUser = supportDb.executeQuery("SELECT user_id, display_name, role, pin_hash, is_active FROM users WHERE user_id = 'admin-test'")
        assertTrue(rsUser.next())
        assertEquals("admin-test", rsUser.getString("user_id"))
        assertEquals("Owner Toko", rsUser.getString("display_name"))
        assertEquals("ADMIN", rsUser.getString("role"))
        assertEquals("salt:hash", rsUser.getString("pin_hash"))
        assertEquals(1, rsUser.getInt("is_active"))
        rsUser.close()

        // 5. Verify categories table created and operational
        supportDb.execSQL(
            """
            INSERT INTO `categories` (`category_id`, `name`, `is_active`, `created_at`, `updated_at`)
            VALUES ('cat-1', 'Sembako', 1, 1700000000000, 1700000000000)
            """.trimIndent()
        )
        val rsCat = supportDb.executeQuery("SELECT name FROM categories WHERE category_id = 'cat-1'")
        assertTrue(rsCat.next())
        assertEquals("Sembako", rsCat.getString("name"))
        rsCat.close()

        // 6. Verify products table created and operational
        supportDb.execSQL(
            """
            INSERT INTO `products` (
                `product_id`, `category_id`, `name`, `barcode`, `product_kind`, 
                `pricing_method`, `quantity_type`, `selling_unit`, `stock_unit`, 
                `current_price`, `is_active`, `created_at`, `updated_at`
            )
            VALUES ('prod-1', 'cat-1', 'Beras 5kg', '8991234567890', 'PHYSICAL', 'PER_UNIT', 'COUNT', 'karung', 'karung', 75000, 1, 1700000000000, 1700000000000)
            """.trimIndent()
        )
        val rsProd = supportDb.executeQuery("SELECT name, current_price FROM products WHERE product_id = 'prod-1'")
        assertTrue(rsProd.next())
        assertEquals("Beras 5kg", rsProd.getString("name"))
        assertEquals(75000L, rsProd.getLong("current_price"))
        rsProd.close()

        // 7. Verify price_history table created and operational
        supportDb.execSQL(
            """
            INSERT INTO `price_history` (`history_id`, `product_id`, `old_price`, `new_price`, `changed_at`, `changed_by`)
            VALUES ('hist-1', 'prod-1', 70000, 75000, 1700000000000, 'admin-test')
            """.trimIndent()
        )
        val rsHist = supportDb.executeQuery("SELECT old_price, new_price, changed_by FROM price_history WHERE history_id = 'hist-1'")
        assertTrue(rsHist.next())
        assertEquals(70000L, rsHist.getLong("old_price"))
        assertEquals(75000L, rsHist.getLong("new_price"))
        assertEquals("admin-test", rsHist.getString("changed_by"))
        rsHist.close()

        // 8. Verify unique barcode constraint in SQLite
        var duplicateBarcodeFailed = false
        try {
            supportDb.execSQL(
                """
                INSERT INTO `products` (
                    `product_id`, `category_id`, `name`, `barcode`, `product_kind`, 
                    `pricing_method`, `quantity_type`, `selling_unit`, `stock_unit`, 
                    `current_price`, `is_active`, `created_at`, `updated_at`
                )
                VALUES ('prod-2', 'cat-1', 'Beras 5kg Duplikat', '8991234567890', 'PHYSICAL', 'PER_UNIT', 'COUNT', 'karung', 'karung', 75000, 1, 1700000000000, 1700000000000)
                """.trimIndent()
            )
        } catch (e: Exception) {
            duplicateBarcodeFailed = true
        }
        assertTrue("Duplicate non-null barcode should fail in SQLite schema", duplicateBarcodeFailed)

        // 9. Verify multiple NULL barcodes are allowed in SQLite
        supportDb.execSQL(
            """
            INSERT INTO `products` (
                `product_id`, `category_id`, `name`, `barcode`, `product_kind`, 
                `pricing_method`, `quantity_type`, `selling_unit`, `stock_unit`, 
                `current_price`, `is_active`, `created_at`, `updated_at`
            )
            VALUES ('prod-null-1', 'cat-1', 'Telur Curah', NULL, 'PHYSICAL', 'PER_KG', 'GRAM', 'kg', 'kg', 28000, 1, 1700000000000, 1700000000000)
            """.trimIndent()
        )
        supportDb.execSQL(
            """
            INSERT INTO `products` (
                `product_id`, `category_id`, `name`, `barcode`, `product_kind`, 
                `pricing_method`, `quantity_type`, `selling_unit`, `stock_unit`, 
                `current_price`, `is_active`, `created_at`, `updated_at`
            )
            VALUES ('prod-null-2', 'cat-1', 'Cabai Rawit', NULL, 'PHYSICAL', 'PER_KG', 'GRAM', 'kg', 'kg', 45000, 1, 1700000000000, 1700000000000)
            """.trimIndent()
        )

        val rsNulls = supportDb.executeQuery("SELECT COUNT(*) as count FROM products WHERE barcode IS NULL")
        assertTrue(rsNulls.next())
        assertEquals(2, rsNulls.getInt("count"))
        rsNulls.close()

        supportDb.close()
    }
}
