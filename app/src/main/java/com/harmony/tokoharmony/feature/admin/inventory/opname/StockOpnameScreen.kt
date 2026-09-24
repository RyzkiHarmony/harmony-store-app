package com.harmony.tokoharmony.feature.admin.inventory.opname

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VerifiedUser
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
import com.harmony.tokoharmony.ui.theme.CoralError
import com.harmony.tokoharmony.ui.theme.EmeraldOnPrimary
import com.harmony.tokoharmony.ui.theme.EmeraldPrimary
import com.harmony.tokoharmony.ui.theme.MintSecondary
import com.harmony.tokoharmony.ui.theme.MintSecondaryContainer
import com.harmony.tokoharmony.ui.theme.OnMintSecondaryContainer
import com.harmony.tokoharmony.ui.theme.OnSurfaceDark
import com.harmony.tokoharmony.ui.theme.SurfaceBackground
import com.harmony.tokoharmony.ui.theme.SurfaceContainer
import com.harmony.tokoharmony.ui.theme.SurfaceContainerHigh
import com.harmony.tokoharmony.ui.theme.SurfaceContainerLow
import com.harmony.tokoharmony.ui.theme.SurfaceLowest
import com.harmony.tokoharmony.ui.theme.TextOnSurfaceVariant

private val COMMON_REASONS = listOf(
    "Barang Rusak / Bocor",
    "Kedaluwarsa (Expired)",
    "Hilang / Selisih Kasir",
    "Salah Catat Masuk",
    "Stock Opname Rutin"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockOpnameScreen(
    onNavigateBack: () -> Unit,
    viewModel: StockOpnameViewModel = hiltViewModel()
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
                                text = "Stock Opname & Koreksi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceDark
                            )
                        }
                        Text(
                            text = "Mode Orang Tua / Admin",
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
                                text = "Pilih produk fisik untuk koreksi stok aktual:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(uiState.products, key = { it.productId }) { product ->
                            val currentStock = uiState.stockMap[product.productId] ?: 0L
                            StockOpnameProductCard(
                                product = product,
                                currentStock = currentStock,
                                onClick = { viewModel.selectProduct(product) }
                            )
                        }
                    }
                }

                // Dialog for Stock Opname & Adjustment (Stitch Formulation)
                uiState.selectedProduct?.let { product ->
                    val isGram = product.quantityType == QuantityType.GRAM
                    val unitLabel = if (isGram) "gram" else product.stockUnit
                    val diff = uiState.difference
                    val currentPhysical = uiState.physicalStockInput.toLongOrNull() ?: uiState.systemStock

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
                                    imageVector = Icons.Default.Balance,
                                    contentDescription = null,
                                    tint = EmeraldPrimary
                                )
                                Text(
                                    text = "Opname: ${product.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceDark
                                )
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // System Stock Box
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = SurfaceContainerLow,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Stok Sistem:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${uiState.systemStock} $unitLabel",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary
                                        )
                                    }
                                }

                                // Physical Stock Input with Quick Stepper
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Stok Fisik Aktual",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnSurfaceDark
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = SurfaceContainerHigh,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clickable {
                                                    val step = if (isGram) 500L else 1L
                                                    val next = (currentPhysical - step).coerceAtLeast(0L)
                                                    viewModel.onPhysicalStockChanged(next.toString())
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Remove,
                                                    contentDescription = "Kurang",
                                                    tint = OnSurfaceDark
                                                )
                                            }
                                        }

                                        OutlinedTextField(
                                            value = uiState.physicalStockInput,
                                            onValueChange = { viewModel.onPhysicalStockChanged(it) },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(10.dp),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = EmeraldPrimary
                                            )
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = SurfaceContainerHigh,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clickable {
                                                    val step = if (isGram) 500L else 1L
                                                    val next = currentPhysical + step
                                                    viewModel.onPhysicalStockChanged(next.toString())
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Tambah",
                                                    tint = OnSurfaceDark
                                                )
                                            }
                                        }
                                    }
                                }

                                // Calculated Difference Card (BR-010)
                                if (diff != null) {
                                    val (diffBg, diffColor, diffTitle, diffSubtitle) = when {
                                        diff > 0 -> Quadruple(
                                            MintSecondaryContainer.copy(alpha = 0.6f),
                                            MintSecondary,
                                            "+$diff $unitLabel (Surplus Fisik)",
                                            "Stok aktual di toko lebih banyak dari sistem"
                                        )
                                        diff < 0 -> Quadruple(
                                            MaterialTheme.colorScheme.errorContainer,
                                            CoralError,
                                            "$diff $unitLabel (Defisit Fisik)",
                                            "Stok aktual di toko berkurang / hilang / rusak"
                                        )
                                        else -> Quadruple(
                                            SurfaceContainerLow,
                                            EmeraldPrimary,
                                            "0 $unitLabel (Sesuai)",
                                            "Stok fisik persis sesuai dengan sistem"
                                        )
                                    }

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = diffBg,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Selisih:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = diffColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = diffTitle,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = diffColor
                                                )
                                            }
                                            Text(
                                                text = diffSubtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = diffColor.copy(alpha = 0.8f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = SurfaceContainer)

                                // Reasons Selection List (Stitch Radio Chips)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Alasan Penyesuaian",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnSurfaceDark
                                    )
                                    COMMON_REASONS.forEach { reason ->
                                        val isSelected = uiState.selectedReason == reason
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) EmeraldPrimary.copy(alpha = 0.12f) else SurfaceContainerLow,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.onReasonSelected(reason) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = null,
                                                    tint = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = reason,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) EmeraldPrimary else OnSurfaceDark
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = uiState.noteInput,
                                    onValueChange = { viewModel.onNoteChanged(it) },
                                    label = { Text("Catatan Auditor (Opsional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary
                                    )
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.saveAdjustment() },
                                enabled = !uiState.isSubmitting && uiState.physicalStock != null,
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
                                    Text("SIMPAN KOREKSI", fontWeight = FontWeight.Bold)
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

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun StockOpnameProductCard(
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
                        text = "Stok sistem:",
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
                    text = "KOREKSI",
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
