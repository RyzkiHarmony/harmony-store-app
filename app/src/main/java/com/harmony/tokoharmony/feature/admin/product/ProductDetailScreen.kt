package com.harmony.tokoharmony.feature.admin.product

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.harmony.tokoharmony.core.common.Formatters
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.ui.theme.CoralError
import com.harmony.tokoharmony.ui.theme.CoralOnError
import com.harmony.tokoharmony.ui.theme.EmeraldOnPrimary
import com.harmony.tokoharmony.ui.theme.EmeraldPrimary
import com.harmony.tokoharmony.ui.theme.EmeraldPrimaryContainer
import com.harmony.tokoharmony.ui.theme.OutlineBorder
import com.harmony.tokoharmony.ui.theme.OutlineVariant
import com.harmony.tokoharmony.ui.theme.SurfaceBackground
import com.harmony.tokoharmony.ui.theme.SurfaceContainerLow
import com.harmony.tokoharmony.ui.theme.SurfaceLowest
import com.harmony.tokoharmony.ui.theme.TextOnSurface
import com.harmony.tokoharmony.ui.theme.TextOnSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditProduct: (String) -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val product = uiState.product

    if (uiState.isPriceChangeDialogVisible && product != null) {
        PriceChangeDialog(
            productName = product.name,
            currentPrice = product.currentPrice,
            sellingUnit = product.sellingUnit,
            onDismiss = { viewModel.dismissPriceChangeDialog() },
            onConfirmPriceChange = { newPrice -> viewModel.changePrice(newPrice) }
        )
    }

    if (uiState.isDeactivateDialogVisible && product != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeactivateDialog() },
            containerColor = SurfaceLowest,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (product.isActive) "Nonaktifkan Produk?" else "Aktifkan Produk Kembali?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextOnSurface
                )
            },
            text = {
                Text(
                    text = if (product.isActive) {
                        "Produk '${product.name}' tidak akan muncul dalam transaksi baru kasir, namun data historis transaksi lama tetap terjaga."
                    } else {
                        "Produk '${product.name}' akan kembali aktif dan dapat dipilih dalam transaksi kasir."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextOnSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.toggleProductActiveStatus() },
                    shape = RoundedCornerShape(10.dp),
                    colors = if (product.isActive) {
                        ButtonDefaults.buttonColors(containerColor = CoralError, contentColor = CoralOnError)
                    } else {
                        ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = EmeraldOnPrimary)
                    }
                ) {
                    Text(if (product.isActive) "Ya, Nonaktifkan" else "Ya, Aktifkan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissDeactivateDialog() },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextOnSurfaceVariant)
                ) {
                    Text("Batal", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    Scaffold(
        containerColor = SurfaceBackground,
        topBar = {
            TopAppBar(
                title = { Text("Detail Produk", fontWeight = FontWeight.Bold, color = TextOnSurface) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextOnSurface
                        )
                    }
                },
                actions = {
                    if (product != null) {
                        IconButton(onClick = { onNavigateToEditProduct(product.productId) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Data Produk",
                                tint = EmeraldPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceLowest,
                    titleContentColor = TextOnSurface
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && product == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        } else if (product == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "Produk tidak ditemukan.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CoralError
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    // Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextOnSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    StatusBadge(isActive = product.isActive)
                                    StockLevelBadge(stockInfo = uiState.stockInfo)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Kategori: ${uiState.category?.name ?: "Umum"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextOnSurfaceVariant
                            )

                            if (!product.barcode.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = EmeraldPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Barcode: ${product.barcode}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OutlineBorder
                                    )
                                }
                            }
                        }
                    }
                }

                // Price Section Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Harga Master Aktif",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextOnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${Formatters.formatRupiah(product.currentPrice)} / ${product.sellingUnit}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.showPriceChangeDialog() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = EmeraldOnPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PriceChange,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ubah Harga Master", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Physical Stock Status Card
                if (product.productKind == ProductKind.PHYSICAL && uiState.stockInfo != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Status Stok Fisik Toko",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextOnSurface
                                        )
                                    }
                                    StockLevelBadge(stockInfo = uiState.stockInfo)
                                }

                                HorizontalDivider(color = SurfaceContainerLow)

                                SpecRow(
                                    label = "Stok Fisik Saat Ini",
                                    value = "${uiState.stockInfo?.currentStock ?: 0L} ${product.stockUnit}"
                                )
                                if (product.minimumStock != null) {
                                    SpecRow(
                                        label = "Peringatan Stok Minimum",
                                        value = "${product.minimumStock} ${product.stockUnit}"
                                    )
                                }
                            }
                        }
                    }
                }

                // Specifications
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Spesifikasi Produk",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextOnSurface
                            )

                            HorizontalDivider(color = SurfaceContainerLow)

                            SpecRow("Jenis Produk", if (product.productKind == ProductKind.PHYSICAL) "Fisik" else "Digital")
                            SpecRow("Metode Harga", if (product.pricingMethod == PricingMethod.PER_UNIT) "Per Satuan Unit" else "Per Kilogram (Timbangan)")
                            SpecRow("Tipe Kuantitas", product.quantityType.name)
                            SpecRow("Satuan Jual", product.sellingUnit)
                            SpecRow("Satuan Stok", product.stockUnit)

                            if (!product.purchaseUnit.isNullOrBlank()) {
                                SpecRow("Satuan Beli", product.purchaseUnit)
                                SpecRow("Konversi Beli", "${product.purchaseConversionFactor ?: 1} ${product.stockUnit} / ${product.purchaseUnit}")
                            }

                            if (product.minimumStock != null) {
                                SpecRow("Stok Minimum", "${product.minimumStock} ${product.stockUnit}")
                            }

                            SpecRow("Dibuat Pada", Formatters.formatDateTime(product.createdAt))
                            SpecRow("Terakhir Diubah", Formatters.formatDateTime(product.updatedAt))
                        }
                    }
                }

                // Price History Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = EmeraldPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Riwayat Perubahan Harga",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextOnSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (uiState.priceHistories.isEmpty()) {
                                Text(
                                    text = "Belum ada riwayat perubahan harga master.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OutlineBorder
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    uiState.priceHistories.forEach { history ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = Formatters.formatRupiah(history.oldPrice),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = OutlineBorder
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = OutlineBorder
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = Formatters.formatRupiah(history.newPrice),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldPrimary
                                                )
                                            }

                                            Text(
                                                text = Formatters.formatDateTime(history.changedAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = OutlineBorder
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Deactivate / Activate Action
                item {
                    OutlinedButton(
                        onClick = { viewModel.showDeactivateDialog() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = if (product.isActive) {
                            ButtonDefaults.outlinedButtonColors(contentColor = CoralError)
                        } else {
                            ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary)
                        }
                    ) {
                        Icon(
                            imageVector = if (product.isActive) Icons.Default.DeleteOutline else Icons.Default.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (product.isActive) "Nonaktifkan Produk" else "Aktifkan Produk Kembali",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextOnSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextOnSurface
        )
    }
}
