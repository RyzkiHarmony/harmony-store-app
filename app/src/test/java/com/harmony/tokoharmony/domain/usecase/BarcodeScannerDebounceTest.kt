package com.harmony.tokoharmony.domain.usecase

import com.harmony.tokoharmony.domain.usecase.scanner.BarcodeDebouncer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BarcodeScannerDebounceTest {

    private lateinit var debouncer: BarcodeDebouncer

    @Before
    fun setup() {
        debouncer = BarcodeDebouncer(debounceWindowMs = 800L)
    }

    @Test
    fun scanner_debouncesSameBarcodeWithin800ms() {
        val baseTime = 1000000L

        // First scan at t=0 -> allowed
        assertTrue(debouncer.canProcess("8991234567", baseTime))

        // Immediate repeat at t=100ms -> debounced / ignored
        assertFalse(debouncer.canProcess("8991234567", baseTime + 100L))

        // Repeat at t=500ms -> debounced / ignored
        assertFalse(debouncer.canProcess("8991234567", baseTime + 500L))

        // Repeat at t=799ms -> debounced / ignored
        assertFalse(debouncer.canProcess("8991234567", baseTime + 799L))
    }

    @Test
    fun scanner_acceptsDifferentBarcodeImmediately() {
        val baseTime = 1000000L

        // Scan barcode A at t=0 -> allowed
        assertTrue(debouncer.canProcess("8991111111", baseTime))

        // Scan barcode B at t=50ms -> allowed immediately (different barcode)
        assertTrue(debouncer.canProcess("8992222222", baseTime + 50L))
    }

    @Test
    fun scanner_acceptsSameBarcodeAfterDebounceWindowExpires() {
        val baseTime = 1000000L

        // First scan at t=0 -> allowed
        assertTrue(debouncer.canProcess("8991234567", baseTime))

        // Repeat scan at t=801ms -> allowed (window has passed)
        assertTrue(debouncer.canProcess("8991234567", baseTime + 801L))

        // Repeat scan at t=900ms -> debounced (relative to the t=801ms scan)
        assertFalse(debouncer.canProcess("8991234567", baseTime + 900L))

        // Repeat scan at t=1650ms -> allowed (801 + 800 = 1601 < 1650)
        assertTrue(debouncer.canProcess("8991234567", baseTime + 1650L))
    }

    @Test
    fun scanner_resetClearsDebounceState() {
        val baseTime = 1000000L

        assertTrue(debouncer.canProcess("8991234567", baseTime))
        assertFalse(debouncer.canProcess("8991234567", baseTime + 100L))

        debouncer.reset()

        // After reset, same barcode is allowed immediately
        assertTrue(debouncer.canProcess("8991234567", baseTime + 100L))
    }

    @Test
    fun blankBarcode_isRejected() {
        assertFalse(debouncer.canProcess(""))
        assertFalse(debouncer.canProcess("   "))
    }
}
