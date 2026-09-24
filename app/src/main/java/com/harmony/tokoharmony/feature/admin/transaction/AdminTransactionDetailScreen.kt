package com.harmony.tokoharmony.feature.admin.transaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.harmony.tokoharmony.core.common.formatRupiah
import com.harmony.tokoharmony.domain.model.PaymentMethod
import com.harmony.tokoharmony.domain.model.TransactionItem
import com.harmony.tokoharmony.domain.model.TransactionStatus
import com.harmony.tokoharmony.ui.theme.CoralError
import com.harmony.tokoharmony.ui.theme.CoralOnError
import com.harmony.tokoharmony.ui.theme.EmeraldPrimary
import com.harmony.tokoharmony.ui.theme.OutlineBorder
import com.harmony.tokoharmony.ui.theme.OutlineVariant
import com.harmony.tokoharmony.ui.theme.StatusErrorBg
import com.harmony.tokoharmony.ui.theme.StatusErrorText
import com.harmony.tokoharmony.ui.theme.StatusSuccessBg
import com.harmony.tokoharmony.ui.theme.StatusSuccessText
import com.harmony.tokoharmony.ui.theme.SurfaceBackground
import com.harmony.tokoharmony.ui.theme.SurfaceLow
import com.harmony.tokoharmony.ui.theme.SurfaceLowest
import com.harmony.tokoharmony.ui.theme.TextOnSurface
import com.harmony.tokoharmony.ui.theme.TextOnSurfaceVariant
import com.harmony.tokoharmony.ui.theme.TextPrimary
import com.harmony.tokoharmony.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTransactionDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminTransactionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = SurfaceBackground,
        topBar = {
            TopAppBar(
                title = { Text("Detail Transaksi", fontWeight = FontWeight.Bold, color = TextOnSurface) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextOnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLowest)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = EmeraldPrimary
                )
            } else {
                val tx = uiState.transaction
                if (tx == null) {
                    Text(
                        text = "Transaksi tidak ditemukan.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextOnSurfaceVariant
                    )
                } else {
                    val scrollState = rememberScrollState()
                    val isCompleted = tx.transactionStatus == TransactionStatus.COMPLETED
                    val isCancelled = tx.transactionStatus == TransactionStatus.CANCELLED

                    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
                    val dateStr = dateFormat.format(Date(tx.createdAt))

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                            border = BorderStroke(1.dp, OutlineVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tx.transactionNumber,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = TextOnSurface
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isCompleted) StatusSuccessBg else StatusErrorBg
                                    ) {
                                        Text(
                                            text = if (isCompleted) "COMPLETED" else "CANCELLED",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCompleted) StatusSuccessText else StatusErrorText,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Waktu: $dateStr",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextOnSurfaceVariant
                                )

                                Text(
                                    text = "Kasir: ${tx.createdBy}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextOnSurfaceVariant
                                )
                            }
                        }

                        // Cancellation Info if Cancelled
                        if (isCancelled) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "Informasi Pembatalan Transaksi",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    tx.cancelledAt?.let {
                                        Text(
                                            "Dibatalkan pada: ${dateFormat.format(Date(it))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    tx.cancelledBy?.let {
                                        Text(
                                            "Dibatalkan oleh: $it",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    tx.cancellationReason?.let {
                                        Text(
                                            "Alasan: $it",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }

                        // Items Breakdown Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                            border = BorderStroke(1.dp, OutlineVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "Daftar Item (${uiState.items.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextOnSurface
                                )

                                Divider(color = OutlineVariant)

                                uiState.items.forEach { item ->
                                    TransactionItemRow(item)
                                }

                                Divider(color = OutlineVariant)

                                // Summary
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Transaksi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextOnSurface)
                                    Text(
                                        formatRupiah(tx.totalAmount),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Metode Pembayaran", color = TextOnSurfaceVariant)
                                    Text(
                                        if (tx.paymentMethod == PaymentMethod.CASH) "Tunai" else "QRIS",
                                        fontWeight = FontWeight.Bold,
                                        color = TextOnSurface
                                    )
                                }

                                if (tx.paymentMethod == PaymentMethod.CASH) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Uang Diterima", color = TextOnSurfaceVariant)
                                        Text(formatRupiah(tx.amountReceived ?: 0L), color = TextOnSurface)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Kembalian", color = StatusSuccessText, fontWeight = FontWeight.Bold)
                                        Text(
                                            formatRupiah(tx.changeAmount),
                                            fontWeight = FontWeight.Bold,
                                            color = StatusSuccessText
                                        )
                                    }
                                }
                            }
                        }

                        // Cancel Button (Only if COMPLETED)
                        if (isCompleted) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CoralError,
                                    contentColor = CoralOnError
                                )
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    "BATALKAN TRANSAKSI",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Cancellation Dialog
            if (showCancelDialog) {
                var expandedReason by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { showCancelDialog = false },
                    containerColor = SurfaceLowest,
                    shape = RoundedCornerShape(20.dp),
                    title = {
                        Text("Batalkan Transaksi?", fontWeight = FontWeight.Bold, color = TextPrimary)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Pembatalan transaksi akan mengembalikan stok fisik produk (SALE_REVERSAL) dan menandai transaksi ini sebagai CANCELLED. Aksi ini tidak dapat dibatalkan.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )

                            ExposedDropdownMenuBox(
                                expanded = expandedReason,
                                onExpandedChange = { expandedReason = !expandedReason }
                            ) {
                                OutlinedTextField(
                                    value = uiState.cancellationReasonInput,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Alasan Pembatalan") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedReason) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary,
                                        focusedLabelColor = EmeraldPrimary,
                                        cursorColor = EmeraldPrimary,
                                        unfocusedContainerColor = SurfaceLow,
                                        focusedContainerColor = SurfaceLowest
                                    ),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = expandedReason,
                                    onDismissRequest = { expandedReason = false }
                                ) {
                                    CANCELLATION_REASONS.forEach { reason ->
                                        DropdownMenuItem(
                                            text = { Text(reason) },
                                            onClick = {
                                                viewModel.onReasonSelected(reason)
                                                expandedReason = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = uiState.noteInput,
                                onValueChange = { viewModel.onNoteChanged(it) },
                                label = { Text("Catatan Tambahan (Opsional)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    focusedLabelColor = EmeraldPrimary,
                                    cursorColor = EmeraldPrimary,
                                    unfocusedContainerColor = SurfaceLow,
                                    focusedContainerColor = SurfaceLowest
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showCancelDialog = false
                                viewModel.cancelTransaction()
                            },
                            shape = RoundedCornerShape(10.dp),
                            enabled = !uiState.isCancelling,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoralError,
                                contentColor = CoralOnError
                            )
                        ) {
                            if (uiState.isCancelling) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CoralOnError)
                            } else {
                                Text("YA, BATALKAN", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showCancelDialog = false },
                            shape = RoundedCornerShape(10.dp),
                            enabled = !uiState.isCancelling,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = BorderStroke(1.dp, OutlineBorder)
                        ) {
                            Text("KEMBALI")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TransactionItemRow(item: TransactionItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.productNameSnapshot,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            val qtyStr = if (item.sellingUnit.equals("kg", ignoreCase = true) || item.quantity >= 500 && item.quantity % 500 == 0L && item.sellingUnit.equals("gram", ignoreCase = true)) {
                val kg = item.quantity / 1000.0
                "${kg.toString().replace(".0", "")} kg × ${formatRupiah(item.unitPrice)}"
            } else {
                "${item.quantity} ${item.sellingUnit} × ${formatRupiah(item.unitPrice)}"
            }
            Text(
                text = qtyStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = formatRupiah(item.subtotal),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
