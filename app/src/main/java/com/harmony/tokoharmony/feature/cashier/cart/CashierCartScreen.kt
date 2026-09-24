package com.harmony.tokoharmony.feature.cashier.cart

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.harmony.tokoharmony.core.common.formatRupiah
import com.harmony.tokoharmony.domain.model.DigitalTransaction
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.TransactionItem
import com.harmony.tokoharmony.feature.cashier.digital.DigitalTransactionDialog
import com.harmony.tokoharmony.feature.cashier.scanner.BarcodeScannerNavigationEvent
import com.harmony.tokoharmony.feature.cashier.scanner.BarcodeScannerViewModel
import com.harmony.tokoharmony.feature.cashier.weight.WeightSelectionDialog
import com.harmony.tokoharmony.ui.theme.AmberOnTertiaryFixed
import com.harmony.tokoharmony.ui.theme.AmberTertiaryFixed
import com.harmony.tokoharmony.ui.theme.CoralError
import com.harmony.tokoharmony.ui.theme.CoralErrorContainer
import com.harmony.tokoharmony.ui.theme.EmeraldOnPrimary
import com.harmony.tokoharmony.ui.theme.EmeraldOnPrimaryContainer
import com.harmony.tokoharmony.ui.theme.EmeraldPrimary
import com.harmony.tokoharmony.ui.theme.EmeraldPrimaryContainer
import com.harmony.tokoharmony.ui.theme.EmeraldPrimaryFixed
import com.harmony.tokoharmony.ui.theme.EmeraldPrimaryFixedDim
import com.harmony.tokoharmony.ui.theme.MintOnSecondaryContainer
import com.harmony.tokoharmony.ui.theme.MintSecondary
import com.harmony.tokoharmony.ui.theme.MintSecondaryContainer
import com.harmony.tokoharmony.ui.theme.OutlineBorder
import com.harmony.tokoharmony.ui.theme.OutlineVariant
import com.harmony.tokoharmony.ui.theme.SurfaceBackground
import com.harmony.tokoharmony.ui.theme.SurfaceContainer
import com.harmony.tokoharmony.ui.theme.SurfaceContainerHigh
import com.harmony.tokoharmony.ui.theme.SurfaceContainerHighest
import com.harmony.tokoharmony.ui.theme.SurfaceLow
import com.harmony.tokoharmony.ui.theme.SurfaceLowest
import com.harmony.tokoharmony.ui.theme.TextOnSurface
import com.harmony.tokoharmony.ui.theme.TextOnSurfaceVariant
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierCartScreen(
    viewModel: CashierCartViewModel,
    scannerViewModel: BarcodeScannerViewModel = hiltViewModel(),
    onNavigateToSearch: () -> Unit,
    onNavigateToScanner: () -> Unit = {},
    onNavigateToRegisterProduct: (barcode: String) -> Unit = {},
    onNavigateToAdminAuth: () -> Unit,
    onNavigateToPayment: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scannerUiState by scannerViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isScannerActive by rememberSaveable { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            isScannerActive = true
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(scannerUiState.errorMessage) {
        scannerUiState.errorMessage?.let { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
            scannerViewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(Unit) {
        scannerViewModel.navEvents.collect { event ->
            when (event) {
                is BarcodeScannerNavigationEvent.ProductAdded -> {
                    // Persistent inline scanner: continuous scanning without closing camera!
                    // Item is atomically added to draft cart in Room DB and Flow updates UI immediately.
                }
                is BarcodeScannerNavigationEvent.NavigateToRegisterProduct -> {
                    onNavigateToRegisterProduct(event.barcode)
                }
            }
        }
    }

    if (uiState.weightEditItem != null) {
        val (product, item) = uiState.weightEditItem!!
        WeightSelectionDialog(
            productName = product.name,
            pricePerKg = product.currentPrice,
            initialGrams = item.quantity,
            onConfirm = { newGrams ->
                viewModel.saveWeightEdit(newGrams)
            },
            onDismiss = {
                viewModel.dismissWeightEditDialog()
            }
        )
    }

    if (uiState.digitalEditItem != null) {
        val (product, item) = uiState.digitalEditItem!!
        val existingDt = uiState.digitalTransactionsMap[item.itemId]
        DigitalTransactionDialog(
            productName = product.name,
            defaultNominal = product.currentPrice,
            existingTransaction = existingDt,
            onConfirm = { serviceType, customerNumber, nominal, providerRef, status ->
                viewModel.saveDigitalTransaction(serviceType, customerNumber, nominal, providerRef, status)
            },
            onDismiss = {
                viewModel.dismissDigitalEditDialog()
            }
        )
    }

    // Weight Selection Dialog from Barcode Scanner
    if (scannerUiState.selectedWeightProduct != null) {
        val product = scannerUiState.selectedWeightProduct!!
        WeightSelectionDialog(
            productName = product.name,
            pricePerKg = product.currentPrice,
            initialGrams = 1000L,
            onConfirm = { grams ->
                scannerViewModel.onWeightConfirmed(grams)
            },
            onDismiss = {
                scannerViewModel.dismissWeightDialog()
            }
        )
    }

    // Stitch Unknown Barcode Modal Dialog
    if (scannerUiState.unknownBarcode != null) {
        val barcode = scannerUiState.unknownBarcode!!
        AlertDialog(
            onDismissRequest = { scannerViewModel.dismissUnknownBarcodeDialog() },
            containerColor = SurfaceLowest,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Surface(
                    shape = CircleShape,
                    color = CoralErrorContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = CoralError,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Barcode Belum Terdaftar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextOnSurface,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = barcode,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = EmeraldPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Text(
                        text = "Produk belum terdaftar di toko. Ingin daftarkan produk baru?",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { scannerViewModel.onRegisterNewProduct() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = EmeraldOnPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Daftarkan Produk Baru", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { scannerViewModel.dismissUnknownBarcodeDialog() }) {
                    Text("Abaikan", color = TextOnSurfaceVariant, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    Scaffold(
        containerColor = SurfaceBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceLowest,
                            modifier = Modifier.size(36.dp),
                            shadowElevation = 1.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Toko Harmony",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextOnSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = EmeraldPrimaryContainer
                                ) {
                                    Text(
                                        text = "KASIR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldOnPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MintSecondary.copy(alpha = pulseAlpha))
                                )
                                Text(
                                    text = "Lokal Siap • Sync Aktif",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextOnSurfaceVariant
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceLowest,
                    titleContentColor = TextOnSurface,
                    actionIconContentColor = TextOnSurfaceVariant
                ),
                actions = {
                    if (uiState.cart?.items?.isNotEmpty() == true) {
                        IconButton(onClick = { viewModel.clearCart() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Kosongkan Keranjang",
                                tint = CoralError
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToAdminAuth) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Mode Orang Tua"
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Stitch Operational Bottom Checkout Dock
            Surface(
                color = SurfaceLowest,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Calculation Summary Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "TOTAL BELANJA",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextOnSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            val itemCount = uiState.cart?.itemCount ?: 0
                            val productCount = uiState.cart?.items?.size ?: 0
                            Text(
                                text = "Subtotal $productCount Produk ($itemCount Item)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextOnSurfaceVariant
                            )
                        }

                        Text(
                            text = formatRupiah(uiState.cart?.totalAmount ?: 0L),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset button
                        Button(
                            onClick = { viewModel.clearCart() },
                            modifier = Modifier
                                .height(52.dp)
                                .width(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceLow,
                                contentColor = TextOnSurfaceVariant
                            ),
                            enabled = uiState.cart?.items?.isNotEmpty() == true
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset Keranjang",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Primary Checkout Trigger Button
                        Button(
                            onClick = onNavigateToPayment,
                            enabled = uiState.cart != null && !uiState.cart!!.isEmpty,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = EmeraldOnPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
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
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "PEMBAYARAN",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = formatRupiah(uiState.cart?.totalAmount ?: 0L),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = EmeraldPrimary
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Active Transaction & Items Badge
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceLow
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Receipt,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = uiState.cart?.transaction?.transactionNumber ?: "TRX-DRAFT",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextOnSurface,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MintSecondaryContainer
                                        ) {
                                            Text(
                                                text = "DRAFT",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MintOnSecondaryContainer,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                val prodCount = uiState.cart?.items?.size ?: 0
                                val itemCount = uiState.cart?.itemCount ?: 0
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldPrimaryContainer.copy(alpha = 0.10f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingBag,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "$prodCount Produk ($itemCount Item)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Search & Scan Action Bar
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Search Input trigger
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clickable { onNavigateToSearch() },
                                    shape = RoundedCornerShape(12.dp),
                                    color = SurfaceLowest,
                                    shadowElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = TextOnSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Cari nama atau barcode...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextOnSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                // Toggle Persistent Scanner Trigger
                                Button(
                                    onClick = {
                                        if (!isScannerActive) {
                                            if (hasCameraPermission) {
                                                isScannerActive = true
                                            } else {
                                                permissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        } else {
                                            isScannerActive = false
                                        }
                                    },
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isScannerActive) CoralErrorContainer else EmeraldPrimary,
                                        contentColor = if (isScannerActive) CoralError else EmeraldOnPrimary
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isScannerActive) Icons.Default.Close else Icons.Default.QrCodeScanner,
                                        contentDescription = if (isScannerActive) "Tutup Scanner" else "Scan Barcode",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isScannerActive) "Tutup Scanner" else "Scan Barcode",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 3. Persistent Inline Barcode Scanner Preview
                        if (isScannerActive) {
                            item {
                                InlineBarcodeScannerView(
                                    viewModel = scannerViewModel,
                                    hasCameraPermission = hasCameraPermission,
                                    onRequestCameraPermission = {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                )
                            }
                        }

                        // 3. Quick Category Pill Chips
                        item {
                            val categories = listOf("Semua", "🌾 Sembako", "🍜 Mie & Instan", "🥤 Minuman", "⚖️ Timbang (Kg)", "🧂 Bumbu")
                            val catScrollState = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(catScrollState),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categories.forEachIndexed { index, cat ->
                                    val isSelected = index == 0
                                    Surface(
                                        modifier = Modifier.clickable { onNavigateToSearch() },
                                        shape = RoundedCornerShape(50),
                                        color = if (isSelected) EmeraldPrimary else SurfaceLow,
                                        shadowElevation = if (isSelected) 1.dp else 0.dp
                                    ) {
                                        Text(
                                            text = cat,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) EmeraldOnPrimary else TextOnSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Cart Items Section Header
                        item {
                            Text(
                                text = "Keranjang Kasir",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextOnSurface,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // If empty, show modern empty state
                        if (uiState.cart == null || uiState.cart!!.isEmpty) {
                            item {
                                EmptyCartView(
                                    onNavigateToScanner = {
                                        if (!isScannerActive) {
                                            if (hasCameraPermission) {
                                                isScannerActive = true
                                            } else {
                                                permissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        }
                                    },
                                    onNavigateToSearch = onNavigateToSearch
                                )
                            }
                        } else {
                            items(
                                items = uiState.cart!!.items,
                                key = { it.itemId }
                            ) { item ->
                                val product = uiState.productsMap[item.productId]
                                val isDigital = product?.productKind == ProductKind.DIGITAL
                                val dt = uiState.digitalTransactionsMap[item.itemId]
                                CartItemCard(
                                    item = item,
                                    isDigital = isDigital,
                                    digitalTransaction = dt,
                                    onIncrement = { viewModel.incrementQuantity(item) },
                                    onDecrement = { viewModel.decrementQuantity(item) },
                                    onRemove = { viewModel.removeItem(item) },
                                    onEditWeight = { viewModel.openWeightEditDialog(item) },
                                    onEditDigital = { viewModel.openDigitalEditDialog(item) }
                                )
                            }
                        }

                        // Spacing at bottom to ensure items aren't obscured by dock
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
fun CartItemCard(
    item: TransactionItem,
    isDigital: Boolean = false,
    digitalTransaction: DigitalTransaction? = null,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
    onEditWeight: () -> Unit,
    onEditDigital: () -> Unit = {}
) {
    val isWeighted = item.sellingUnit.equals("kg", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Item Icon Container
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceLow,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when {
                                    isDigital -> Icons.Default.PhoneAndroid
                                    isWeighted -> Icons.Default.Scale
                                    else -> Icons.Default.ShoppingBag
                                },
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.productNameSnapshot,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextOnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Badges & unit info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SurfaceContainer
                            ) {
                                Text(
                                    text = item.sellingUnit,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextOnSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (isDigital) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MintSecondaryContainer
                                ) {
                                    Text(
                                        text = "DIGITAL",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MintOnSecondaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (isWeighted) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AmberTertiaryFixed
                                ) {
                                    Text(
                                        text = "TIMBANG",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberOnTertiaryFixed,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Gram or Unit calculation string
                        val calculationString = if (isWeighted) {
                            val kgString = if (item.quantity % 1000L == 0L) {
                                "${item.quantity / 1000L} kg"
                            } else {
                                String.format("%.1f kg", item.quantity / 1000.0).replace('.', ',')
                            }
                            "$kgString (${item.quantity} gr) × ${formatRupiah(item.unitPrice)}/kg"
                        } else {
                            "${item.quantity} × ${formatRupiah(item.unitPrice)}"
                        }

                        Text(
                            text = calculationString,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextOnSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        if (isDigital && digitalTransaction != null) {
                            Text(
                                text = "${digitalTransaction.serviceType}: ${digitalTransaction.customerNumber} (${digitalTransaction.status})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                // Subtotal
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Subtotal",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextOnSurfaceVariant
                    )
                    Text(
                        text = formatRupiah(item.subtotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
            }

            // Stepper Controls & Delete Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = SurfaceLow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delete Button
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CoralErrorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = CoralError,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Stepper / Action Controls
                    if (isDigital) {
                        Button(
                            onClick = onEditDigital,
                            shape = RoundedCornerShape(8.dp),
                            colors = if (digitalTransaction != null) {
                                ButtonDefaults.buttonColors(containerColor = SurfaceContainerHigh, contentColor = TextOnSurface)
                            } else {
                                ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = EmeraldOnPrimary)
                            },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (digitalTransaction != null) "Ubah Data Digital" else "Catat Data Digital",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (isWeighted) {
                        FilledTonalButton(
                            onClick = onEditWeight,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SurfaceContainerHighest,
                                contentColor = TextOnSurface
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp), tint = AmberTertiaryFixed)
                            Spacer(modifier = Modifier.width(6.dp))
                            val kgLabel = if (item.quantity % 1000L == 0L) {
                                "${item.quantity / 1000L} kg"
                            } else {
                                String.format("%.1f kg", item.quantity / 1000.0).replace('.', ',')
                            }
                            Text("⚖️ Ubah Berat ($kgLabel)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Minus Button
                            Surface(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { onDecrement() },
                                shape = CircleShape,
                                color = SurfaceContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Kurang",
                                        tint = TextOnSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Quantity Display
                            Text(
                                text = "${item.quantity}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextOnSurface,
                                modifier = Modifier.width(28.dp),
                                textAlign = TextAlign.Center
                            )

                            // Plus Button
                            Surface(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { onIncrement() },
                                shape = CircleShape,
                                color = EmeraldPrimary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Tambah",
                                        tint = EmeraldOnPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCartView(
    onNavigateToScanner: () -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = SurfaceLow,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = EmeraldPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Keranjang Masih Kosong",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextOnSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Pindai barcode atau cari produk untuk mulai transaksi",
                style = MaterialTheme.typography.bodySmall,
                color = TextOnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onNavigateToScanner,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Barcode", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onNavigateToSearch,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = EmeraldOnPrimary
                    )
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cari Produk", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun InlineBarcodeScannerView(
    viewModel: BarcodeScannerViewModel,
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    // Auto-clear last scanned feedback after 2.5s
    LaunchedEffect(uiState.lastScannedProductName) {
        if (uiState.lastScannedProductName != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearFeedback()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasCameraPermission) {
                val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

                DisposableEffect(lifecycleOwner) {
                    onDispose {
                        try {
                            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                            cameraProvider.unbindAll()
                        } catch (e: Exception) {
                            // ignore
                        }
                        cameraExecutor.shutdown()
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            val barcodeScanner = BarcodeScanning.getClient()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                barcode.rawValue?.let { value ->
                                                    viewModel.onBarcodeDetected(value)
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Laser Scanner Animation
                val infiniteTransition = rememberInfiniteTransition(label = "inline_scanner")
                val scanOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "inlineScanOffset"
                )

                // Viewfinder Reticle Overlay with Neon Accents
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(190.dp, 110.dp)
                    ) {
                        // Corner brackets
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .size(20.dp)
                                .border(BorderStroke(3.dp, EmeraldPrimaryFixedDim), RoundedCornerShape(topStart = 6.dp))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(20.dp)
                                .border(BorderStroke(3.dp, EmeraldPrimaryFixedDim), RoundedCornerShape(topEnd = 6.dp))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .size(20.dp)
                                .border(BorderStroke(3.dp, EmeraldPrimaryFixedDim), RoundedCornerShape(bottomStart = 6.dp))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(20.dp)
                                .border(BorderStroke(3.dp, EmeraldPrimaryFixedDim), RoundedCornerShape(bottomEnd = 6.dp))
                        )

                        // Animated Glowing Laser Line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .align(Alignment.TopCenter)
                                .padding(top = (108 * scanOffset).dp)
                                .background(EmeraldPrimaryFixed)
                        )
                    }
                }

                // Top Feedback Pill on Scan Success
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                ) {
                    AnimatedVisibility(
                        visible = uiState.lastScannedProductName != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            color = EmeraldPrimary,
                            shape = RoundedCornerShape(50),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldOnPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "✓ ${uiState.lastScannedProductName} masuk keranjang",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldOnPrimary
                                )
                            }
                        }
                    }
                }

                // Instruction Bottom Pill
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = "Arahkan barcode ke kotak pemindai • Multi-scan aktif",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Permission Required Inline View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = CoralError
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Izin Kamera Diperlukan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Aktifkan kamera untuk memindai barcode secara langsung",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRequestCameraPermission,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = EmeraldOnPrimary
                        )
                    ) {
                        Text("Berikan Izin Kamera", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
