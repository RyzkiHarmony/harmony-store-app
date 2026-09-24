package com.harmony.tokoharmony.feature.admin.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToInitialStock: () -> Unit,
    onNavigateToStockIn: () -> Unit,
    onNavigateToStockOpname: () -> Unit,
    onNavigateToStockHistory: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToSync: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mode Orang Tua", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Home"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Manajemen Master",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                AdminMenuItem(
                    title = "Produk & Harga",
                    subtitle = "Kelola data produk, satuan, dan master harga",
                    icon = Icons.Default.ShoppingBag,
                    onClick = onNavigateToProducts,
                    isEnabled = true
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Manajemen Stok Fisik",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                AdminMenuItem(
                    title = "Stok Awal (Initial Stock)",
                    subtitle = "Penetapan saldo awal stok fisik produk",
                    icon = Icons.Default.Inventory,
                    onClick = onNavigateToInitialStock,
                    isEnabled = true
                )
            }

            item {
                AdminMenuItem(
                    title = "Barang Masuk (Stock In)",
                    subtitle = "Pencatatan pembelian barang dari supplier",
                    icon = Icons.Default.MoveToInbox,
                    onClick = onNavigateToStockIn,
                    isEnabled = true
                )
            }

            item {
                AdminMenuItem(
                    title = "Stock Opname & Koreksi",
                    subtitle = "Penyesuaian fisik vs sistem dengan alasan",
                    icon = Icons.Default.Assessment,
                    onClick = onNavigateToStockOpname,
                    isEnabled = true
                )
            }

            item {
                AdminMenuItem(
                    title = "Riwayat Mutasi Stok",
                    subtitle = "Audit buku besar mutasi keluar masuk stok fisik",
                    icon = Icons.Default.History,
                    onClick = onNavigateToStockHistory,
                    isEnabled = true
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Transaksi & Sinkronisasi",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                AdminMenuItem(
                    title = "Riwayat Transaksi & Pembatalan",
                    subtitle = "Audit riwayat transaksi dan pembatalan resmi",
                    icon = Icons.Default.History,
                    onClick = onNavigateToTransactions,
                    isEnabled = true
                )
            }

            item {
                AdminMenuItem(
                    title = "Sinkronisasi Google Sheets",
                    subtitle = "Status antrean, sinkronisasi & pemulihan cadangan",
                    icon = Icons.Default.CloudSync,
                    onClick = onNavigateToSync,
                    isEnabled = true
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun AdminMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isEnabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = if (isEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.outline
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                )
            }

            if (isEnabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
