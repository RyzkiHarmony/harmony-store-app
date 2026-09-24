package com.harmony.tokoharmony.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.DriverManager

class SyncQueueDaoJdbcTest {

    @Test
    fun syncQueue_lifecycle_enqueue_syncing_retry_and_stale_recovery() {
        val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        val supportDb = JdbcSupportSQLiteDatabase(connection)

        // 1. Create sync_queue table
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

        val now = 1700000000000L

        // 2. Insert PENDING items
        supportDb.execSQL(
            """
            INSERT INTO `sync_queue` (`queue_id`, `entity_type`, `entity_id`, `operation`, `payload`, `status`, `retry_count`, `last_error`, `created_at`, `synced_at`)
            VALUES 
            ('q-1', 'PRODUCT', 'prod-1', 'CREATE', '{"productId":"prod-1"}', 'PENDING', 0, NULL, $now, NULL),
            ('q-2', 'TRANSACTION', 'tx-1', 'CREATE', '{"transactionId":"tx-1"}', 'PENDING', 0, NULL, ${now + 1000}, NULL)
            """.trimIndent()
        )

        // 3. Verify Pending query
        val rsPending = supportDb.executeQuery("SELECT queue_id FROM sync_queue WHERE status = 'PENDING' ORDER BY created_at ASC")
        assertTrue(rsPending.next())
        assertEquals("q-1", rsPending.getString("queue_id"))
        assertTrue(rsPending.next())
        assertEquals("q-2", rsPending.getString("queue_id"))
        rsPending.close()

        // 4. Mark Syncing
        supportDb.execSQL("UPDATE sync_queue SET status = 'SYNCING' WHERE queue_id IN ('q-1', 'q-2')")
        val rsSyncing = supportDb.executeQuery("SELECT COUNT(*) AS c FROM sync_queue WHERE status = 'SYNCING'")
        assertTrue(rsSyncing.next())
        assertEquals(2L, rsSyncing.getLong("c"))
        rsSyncing.close()

        // 5. Mark q-1 as SYNCED
        val syncedAt = now + 5000
        supportDb.execSQL("UPDATE sync_queue SET status = 'SYNCED', synced_at = $syncedAt, last_error = NULL WHERE queue_id = 'q-1'")
        val rsSynced = supportDb.executeQuery("SELECT status, synced_at FROM sync_queue WHERE queue_id = 'q-1'")
        assertTrue(rsSynced.next())
        assertEquals("SYNCED", rsSynced.getString("status"))
        assertEquals(syncedAt, rsSynced.getLong("synced_at"))
        rsSynced.close()

        // 6. Mark q-2 as FAILED with retry
        supportDb.execSQL("UPDATE sync_queue SET status = 'PENDING', retry_count = 1, last_error = 'Timeout' WHERE queue_id = 'q-2'")
        val rsFailed = supportDb.executeQuery("SELECT status, retry_count, last_error FROM sync_queue WHERE queue_id = 'q-2'")
        assertTrue(rsFailed.next())
        assertEquals("PENDING", rsFailed.getString("status"))
        assertEquals(1, rsFailed.getInt("retry_count"))
        assertEquals("Timeout", rsFailed.getString("last_error"))
        rsFailed.close()

        // 7. Stale SYNCING Recovery test
        val staleTime = now - (10 * 60 * 1000L) // 10 minutes ago
        supportDb.execSQL(
            """
            INSERT INTO `sync_queue` (`queue_id`, `entity_type`, `entity_id`, `operation`, `payload`, `status`, `retry_count`, `last_error`, `created_at`, `synced_at`)
            VALUES ('q-stale', 'PRODUCT', 'prod-2', 'UPDATE', '{"productId":"prod-2"}', 'SYNCING', 0, NULL, $staleTime, NULL)
            """.trimIndent()
        )

        // Run recovery: status = 'SYNCING' AND created_at < threshold (5 mins ago)
        val threshold = now - (5 * 60 * 1000L)
        supportDb.execSQL("UPDATE sync_queue SET status = 'PENDING' WHERE status = 'SYNCING' AND created_at < $threshold")

        val rsRecovered = supportDb.executeQuery("SELECT status FROM sync_queue WHERE queue_id = 'q-stale'")
        assertTrue(rsRecovered.next())
        assertEquals("PENDING", rsRecovered.getString("status"))
        rsRecovered.close()

        supportDb.close()
    }
}
