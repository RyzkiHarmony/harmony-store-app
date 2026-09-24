package com.harmony.tokoharmony.feature.cashier.digital

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.harmony.tokoharmony.domain.model.DigitalTransaction
import com.harmony.tokoharmony.ui.theme.EmeraldOnPrimary
import com.harmony.tokoharmony.ui.theme.EmeraldPrimary
import com.harmony.tokoharmony.ui.theme.OutlineBorder
import com.harmony.tokoharmony.ui.theme.StatusErrorBg
import com.harmony.tokoharmony.ui.theme.StatusErrorText
import com.harmony.tokoharmony.ui.theme.StatusSuccessBg
import com.harmony.tokoharmony.ui.theme.StatusSuccessText
import com.harmony.tokoharmony.ui.theme.StatusWarningBg
import com.harmony.tokoharmony.ui.theme.StatusWarningText
import com.harmony.tokoharmony.ui.theme.SurfaceLow
import com.harmony.tokoharmony.ui.theme.SurfaceLowest
import com.harmony.tokoharmony.ui.theme.TextPrimary
import com.harmony.tokoharmony.ui.theme.TextSecondary

@Composable
fun DigitalTransactionDialog(
    productName: String,
    defaultNominal: Long,
    existingTransaction: DigitalTransaction? = null,
    onConfirm: (serviceType: String, customerNumber: String, nominal: Long, providerRef: String?, status: String) -> Unit,
    onDismiss: () -> Unit
) {
    var serviceType by remember {
        mutableStateOf(existingTransaction?.serviceType ?: detectServiceType(productName))
    }
    var customerNumber by remember {
        mutableStateOf(existingTransaction?.customerNumber ?: "")
    }
    var nominalText by remember {
        mutableStateOf((existingTransaction?.nominal ?: defaultNominal).toString())
    }
    var providerRef by remember {
        mutableStateOf(existingTransaction?.providerReference ?: "")
    }
    var status by remember {
        mutableStateOf(existingTransaction?.status ?: "SUCCESS")
    }

    var customerNumberError by remember { mutableStateOf<String?>(null) }
    var serviceTypeError by remember { mutableStateOf<String?>(null) }

    val commonServices = listOf("Pulsa", "Paket Data", "Token PLN", "PDAM", "Voucher Game", "Lainnya")
    val statuses = listOf("SUCCESS", "PENDING", "FAILED")

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = EmeraldPrimary,
        focusedLabelColor = EmeraldPrimary,
        cursorColor = EmeraldPrimary,
        unfocusedContainerColor = SurfaceLow,
        focusedContainerColor = SurfaceLowest
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceLowest,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = "Pencatatan Transaksi Digital",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = productName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Service Type Quick Selector
                Text(
                    text = "Jenis Layanan:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonServices.take(3).forEach { service ->
                        FilterChip(
                            selected = serviceType.equals(service, ignoreCase = true),
                            onClick = { serviceType = service },
                            label = { Text(service, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = EmeraldOnPrimary
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonServices.drop(3).forEach { service ->
                        FilterChip(
                            selected = serviceType.equals(service, ignoreCase = true),
                            onClick = { serviceType = service },
                            label = { Text(service, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = EmeraldOnPrimary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = serviceType,
                    onValueChange = {
                        serviceType = it
                        if (it.isNotBlank()) serviceTypeError = null
                    },
                    label = { Text("Jenis Layanan / Provider *") },
                    isError = serviceTypeError != null,
                    supportingText = serviceTypeError?.let { { Text(it) } },
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Customer Number
                OutlinedTextField(
                    value = customerNumber,
                    onValueChange = {
                        customerNumber = it
                        if (it.isNotBlank()) customerNumberError = null
                    },
                    label = { Text("Nomor Tujuan / No Pelanggan *") },
                    placeholder = { Text("contoh: 081234567890 atau ID Meter") },
                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = customerNumberError != null,
                    supportingText = customerNumberError?.let { { Text(it) } },
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Nominal Face Value
                OutlinedTextField(
                    value = nominalText,
                    onValueChange = { nominalText = it.filter { char -> char.isDigit() } },
                    label = { Text("Nominal Layanan (Rp)") },
                    leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Provider Reference / SN
                OutlinedTextField(
                    value = providerRef,
                    onValueChange = { providerRef = it },
                    label = { Text("No Referensi / SN Agen (Opsional)") },
                    placeholder = { Text("No ref dari aplikasi agen") },
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Status Selector
                Text(
                    text = "Status Transaksi Agen:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    statuses.forEach { st ->
                        val chipColors = when (st) {
                            "SUCCESS" -> FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StatusSuccessBg,
                                selectedLabelColor = StatusSuccessText
                            )
                            "PENDING" -> FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StatusWarningBg,
                                selectedLabelColor = StatusWarningText
                            )
                            else -> FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StatusErrorBg,
                                selectedLabelColor = StatusErrorText
                            )
                        }
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st, fontWeight = if (status == st) FontWeight.Bold else FontWeight.Normal) },
                            colors = chipColors
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var hasError = false
                    if (serviceType.isBlank()) {
                        serviceTypeError = "Jenis layanan wajib diisi"
                        hasError = true
                    }
                    if (customerNumber.isBlank()) {
                        customerNumberError = "Nomor pelanggan wajib diisi"
                        hasError = true
                    }
                    if (!hasError) {
                        val parsedNominal = nominalText.toLongOrNull() ?: defaultNominal
                        onConfirm(
                            serviceType.trim(),
                            customerNumber.trim(),
                            parsedNominal,
                            providerRef.trim().ifBlank { null },
                            status
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = EmeraldOnPrimary
                )
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = BorderStroke(1.dp, OutlineBorder)
            ) {
                Text("Batal")
            }
        }
    )
}

private fun detectServiceType(productName: String): String {
    val lower = productName.lowercase()
    return when {
        lower.contains("pln") || lower.contains("token") || lower.contains("listrik") -> "Token PLN"
        lower.contains("paket") || lower.contains("data") || lower.contains("kuota") || lower.contains("gb") -> "Paket Data"
        lower.contains("pdam") || lower.contains("air") -> "PDAM"
        lower.contains("voucher") || lower.contains("game") -> "Voucher Game"
        lower.contains("pulsa") -> "Pulsa"
        else -> "Produk Digital"
    }
}
