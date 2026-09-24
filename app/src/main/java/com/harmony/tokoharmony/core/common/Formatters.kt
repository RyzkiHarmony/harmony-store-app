package com.harmony.tokoharmony.core.common

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    private val numberFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

    fun formatRupiah(amount: Long): String {
        return "Rp " + numberFormat.format(amount)
    }

    fun formatNumber(number: Long): String {
        return numberFormat.format(number)
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        return sdf.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        return sdf.format(Date(timestamp))
    }
}

fun formatRupiah(amount: Long): String = Formatters.formatRupiah(amount)
