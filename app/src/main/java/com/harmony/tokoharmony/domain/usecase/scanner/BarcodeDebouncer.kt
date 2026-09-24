package com.harmony.tokoharmony.domain.usecase.scanner

class BarcodeDebouncer(
    private val debounceWindowMs: Long = 800L
) {
    private var lastBarcode: String? = null
    private var lastTimestamp: Long = 0L

    @Synchronized
    fun canProcess(barcode: String, currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        if (barcode.isBlank()) return false

        if (lastBarcode == barcode && (currentTimeMs - lastTimestamp) < debounceWindowMs) {
            return false
        }

        lastBarcode = barcode
        lastTimestamp = currentTimeMs
        return true
    }

    @Synchronized
    fun reset() {
        lastBarcode = null
        lastTimestamp = 0L
    }
}
