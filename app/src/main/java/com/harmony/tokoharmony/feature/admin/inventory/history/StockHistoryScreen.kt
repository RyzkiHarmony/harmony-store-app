package com.harmony.tokoharmony.feature.admin.inventory.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.harmony.tokoharmony.domain.model.MovementType
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.StockMovement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: StockHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Mutasi Stok", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.movements.isEmpty()) {
                Text(
                    text = "Belum ada riwayat pergerakan stok.",
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

                    items(uiState.movements, key = { it.movementId }) { movement ->
                        val product = uiState.productsMap[movement.productId]
                        val productName = product?.name ?: "Produk (${movement.productId.take(8)})"
                        val isGram = product?.quantityType == QuantityType.GRAM
                        val unit = if (isGram) "gram" else (product?.stockUnit ?: "pcs")

                        StockMovementCard(movement, productName, unit)
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StockMovementCard(
    movement: StockMovement,
    productName: String,
    unit: String
) {
    val isPositive = movement.quantityDelta >= 0
    val deltaColor = if (isPositive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    val deltaBg = if (isPositive) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer
    val deltaPrefix = if (isPositive) "+" else ""

    val typeLabel = when (movement.movementType) {
        MovementType.INITIAL_STOCK -> "STOK AWAL"
        MovementType.STOCK_IN -> "BARANG MASUK"
        MovementType.SALE -> "PENJUALAN"
        MovementType.SALE_REVERSAL -> "PEMBATALAN JUAL"
        MovementType.ADJUSTMENT -> "KOREKSI / OPNAME"
    }

    val typeBg = when (movement.movementType) {
        MovementType.INITIAL_STOCK -> MaterialTheme.colorScheme.primaryContainer
        MovementType.STOCK_IN -> MaterialTheme.colorScheme.secondaryContainer
        MovementType.SALE -> MaterialTheme.colorScheme.surfaceVariant
        MovementType.SALE_REVERSAL -> Color(0xFFFFF3E0)
        MovementType.ADJUSTMENT -> MaterialTheme.colorScheme.tertiaryContainer
    }

    val typeColor = when (movement.movementType) {
        MovementType.INITIAL_STOCK -> MaterialTheme.colorScheme.onPrimaryContainer
        MovementType.STOCK_IN -> MaterialTheme.colorScheme.onSecondaryContainer
        MovementType.SALE -> MaterialTheme.colorScheme.onSurfaceVariant
        MovementType.SALE_REVERSAL -> Color(0xFFE65100)
        MovementType.ADJUSTMENT -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    val dateStr = dateFormat.format(Date(movement.createdAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = typeBg
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = typeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = deltaBg
                ) {
                    Text(
                        text = "$deltaPrefix${movement.quantityDelta} $unit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = deltaColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (!movement.reason.isNullOrBlank()) {
                Text(
                    text = movement.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
