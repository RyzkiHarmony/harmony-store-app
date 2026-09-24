package com.harmony.tokoharmony.feature.admin.transaction

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.harmony.tokoharmony.core.common.formatRupiah
import com.harmony.tokoharmony.domain.model.PaymentMethod
import com.harmony.tokoharmony.domain.model.Transaction
import com.harmony.tokoharmony.domain.model.TransactionStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import com.harmony.tokoharmony.domain.usecase.transaction.TransactionDateFilter
import com.harmony.tokoharmony.domain.usecase.transaction.TransactionPaymentFilter
import com.harmony.tokoharmony.domain.usecase.transaction.TransactionStatusFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTransactionHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: AdminTransactionHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAnyFilterActive = uiState.dateFilter != TransactionDateFilter.ALL ||
            uiState.statusFilter != TransactionStatusFilter.ALL ||
            uiState.paymentFilter != TransactionPaymentFilter.ALL

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Transaksi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Section
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Date Filters
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Waktu:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilterChip(
                            selected = uiState.dateFilter == TransactionDateFilter.ALL,
                            onClick = { viewModel.onDateFilterChanged(TransactionDateFilter.ALL) },
                            label = { Text("Semua") }
                        )
                        FilterChip(
                            selected = uiState.dateFilter == TransactionDateFilter.TODAY,
                            onClick = { viewModel.onDateFilterChanged(TransactionDateFilter.TODAY) },
                            label = { Text("Hari Ini") }
                        )
                        FilterChip(
                            selected = uiState.dateFilter == TransactionDateFilter.LAST_7_DAYS,
                            onClick = { viewModel.onDateFilterChanged(TransactionDateFilter.LAST_7_DAYS) },
                            label = { Text("7 Hari") }
                        )
                        FilterChip(
                            selected = uiState.dateFilter == TransactionDateFilter.THIS_MONTH,
                            onClick = { viewModel.onDateFilterChanged(TransactionDateFilter.THIS_MONTH) },
                            label = { Text("Bulan Ini") }
                        )
                    }

                    // Status & Payment Filters
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilterChip(
                            selected = uiState.statusFilter == TransactionStatusFilter.ALL,
                            onClick = { viewModel.onStatusFilterChanged(TransactionStatusFilter.ALL) },
                            label = { Text("Semua") }
                        )
                        FilterChip(
                            selected = uiState.statusFilter == TransactionStatusFilter.COMPLETED,
                            onClick = { viewModel.onStatusFilterChanged(TransactionStatusFilter.COMPLETED) },
                            label = { Text("Selesai") }
                        )
                        FilterChip(
                            selected = uiState.statusFilter == TransactionStatusFilter.CANCELLED,
                            onClick = { viewModel.onStatusFilterChanged(TransactionStatusFilter.CANCELLED) },
                            label = { Text("Dibatalkan") }
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Metode:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilterChip(
                            selected = uiState.paymentFilter == TransactionPaymentFilter.ALL,
                            onClick = { viewModel.onPaymentFilterChanged(TransactionPaymentFilter.ALL) },
                            label = { Text("Semua") }
                        )
                        FilterChip(
                            selected = uiState.paymentFilter == TransactionPaymentFilter.CASH,
                            onClick = { viewModel.onPaymentFilterChanged(TransactionPaymentFilter.CASH) },
                            label = { Text("Tunai") }
                        )
                        FilterChip(
                            selected = uiState.paymentFilter == TransactionPaymentFilter.QRIS,
                            onClick = { viewModel.onPaymentFilterChanged(TransactionPaymentFilter.QRIS) },
                            label = { Text("QRIS") }
                        )
                    }

                    // Status Bar / Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Menampilkan ${uiState.transactions.size} transaksi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (isAnyFilterActive) {
                            TextButton(onClick = { viewModel.resetFilters() }) {
                                Text("Reset Filter", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.transactions.isEmpty()) {
                    Text(
                        text = if (isAnyFilterActive) "Tidak ada transaksi yang cocok dengan filter." else "Belum ada riwayat transaksi.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(uiState.transactions, key = { it.transactionId }) { tx ->
                            AdminTransactionItem(
                                transaction = tx,
                                onClick = { onNavigateToDetail(tx.transactionId) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminTransactionItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val isCompleted = transaction.transactionStatus == TransactionStatus.COMPLETED
    val statusColor = if (isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    val statusBg = if (isCompleted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer
    val statusText = if (isCompleted) "COMPLETED" else "CANCELLED"

    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    val dateStr = dateFormat.format(Date(transaction.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (isCompleted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = statusColor
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.transactionNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatRupiah(transaction.totalAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val methodStr = if (transaction.paymentMethod == PaymentMethod.CASH) "Tunai" else "QRIS"
                    Text(
                        text = "$methodStr • $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
