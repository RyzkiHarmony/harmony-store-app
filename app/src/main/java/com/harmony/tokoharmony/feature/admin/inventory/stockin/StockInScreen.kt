package com.harmony.tokoharmony.feature.admin.inventory.stockin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.SyncAlt
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.ui.theme.EmeraldOnPrimary
import com.harmony.tokoharmony.ui.theme.EmeraldPrimary
import com.harmony.tokoharmony.ui.theme.MintOnSecondary
import com.harmony.tokoharmony.ui.theme.MintSecondary
import com.harmony.tokoharmony.ui.theme.MintSecondaryContainer
import com.harmony.tokoharmony.ui.theme.OnMintSecondaryContainer
import com.harmony.tokoharmony.ui.theme.OnSurfaceDark
import com.harmony.tokoharmony.ui.theme.SurfaceBackground
import com.harmony.tokoharmony.ui.theme.SurfaceContainer
import com.harmony.tokoharmony.ui.theme.SurfaceContainerLow
import com.harmony.tokoharmony.ui.theme.SurfaceLowest
import com.harmony.tokoharmony.ui.theme.TextOnSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockInScreen(
    onNavigateBack: () -> Unit,
    viewModel: StockInViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Penerimaan Barang Masuk",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceDark
                            )
                        }
                        Text(
                            text = "Mode Orang Tua / Inventori Gudang & Toko",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = EmeraldPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceLowest,
                    titleContentColor = OnSurfaceDark
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = EmeraldPrimary
                    )
                } else if (uiState.products.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada produk fisik aktif.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "Pilih produk fisik untuk mencatat barang masuk:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(uiState.products, key = { it.productId }) { product ->
                            val currentStock = uiState.stockMap[product.productId] ?: 0L
                            StockInProductCard(
                                product = product,
                                currentStock = currentStock,
                                onClick = { viewModel.selectProduct(product) }
                            )
                        }
                    }
                }

                // Dialog for recording stock in (Stitch Modal Formulation)
                uiState.selectedProduct?.let { product ->
                    val isGram = product.quantityType == QuantityType.GRAM
                    val baseUnit = if (isGram) "gram" else product.stockUnit
                    val currentStock = uiState.stockMap[product.productId] ?: 0L

                    AlertDialog(
                        onDismissRequest = { viewModel.clearSelectedProduct() },
                        containerColor = SurfaceLowest,
                        shape = RoundedCornerShape(20.dp),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = EmeraldPrimary
                                )
                                Text(
                                    text = "Barang Masuk: ${product.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceDark
                                )
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Selected Product Stock Strip
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SurfaceContainerLow,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Stok Saat Ini:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "$currentStock $baseUnit",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = uiState.purchaseQuantityInput,
                                        onValueChange = { viewModel.onPurchaseQuantityChanged(it) },
                                        label = { Text("Jumlah Beli") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmeraldPrimary
                                        )
                                    )

                                    OutlinedTextField(
                                        value = uiState.purchaseUnitInput,
                                        onValueChange = { viewModel.onPurchaseUnitChanged(it) },
                                        label = { Text("Satuan Beli") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmeraldPrimary
                                        )
                                    )
                                }

                                OutlinedTextField(
                                    value = uiState.conversionFactorInput,
                                    onValueChange = { viewModel.onConversionFactorChanged(it) },
                                    label = { Text("Isi per Satuan Beli ($baseUnit)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary
                                    )
                                )

                                OutlinedTextField(
                                    value = uiState.noteInput,
                                    onValueChange = { viewModel.onNoteChanged(it) },
                                    label = { Text("Catatan / Faktur (Opsional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary
                                    )
                                )

                                HorizontalDivider(color = SurfaceContainer)

                                // Kalkulasi Otomatis Highlight Box (Stitch Format)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MintSecondaryContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Calculate,
                                                    contentDescription = null,
                                                    tint = MintSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Kalkulasi Mutasi Masuk",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = OnMintSecondaryContainer
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MintSecondary
                                            ) {
                                                Text(
                                                    text = "Otomatis",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MintOnSecondary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        val purchaseQty = uiState.purchaseQuantity
                                        val convFactor = uiState.conversionFactor
                                        val unitName = uiState.purchaseUnitInput.ifBlank { "Satuan" }
                                        Text(
                                            text = "$purchaseQty $unitName × $convFactor $baseUnit = +${uiState.totalStockQuantity} $baseUnit",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MintSecondary
                                        )
                                        Text(
                                            text = "Stok Akhir: ${currentStock + uiState.totalStockQuantity} $baseUnit",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = OnMintSecondaryContainer
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.saveStockIn() },
                                enabled = !uiState.isSubmitting && uiState.purchaseQuantity > 0 && uiState.conversionFactor > 0,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = EmeraldOnPrimary
                                )
                            ) {
                                if (uiState.isSubmitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = EmeraldOnPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("SIMPAN BARANG MASUK", fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { viewModel.clearSelectedProduct() },
                                enabled = !uiState.isSubmitting,
                                colors = ButtonDefaults.textButtonColors(contentColor = TextOnSurfaceVariant)
                            ) {
                                Text("BATAL", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StockInProductCard(
    product: Product,
    currentStock: Long,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceContainerLow
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = EmeraldPrimary
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )
                val unit = if (product.quantityType == QuantityType.GRAM) "gram" else product.stockUnit
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Stok saat ini:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currentStock $unit",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EmeraldPrimary
            ) {
                Text(
                    text = "+ TAMBAH",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldOnPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 11.sp
                )
            }
        }
    }
}
