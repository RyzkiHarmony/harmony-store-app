/**
 * Toko Harmony POS - Google Apps Script Web App Gateway
 *
 * Handles:
 * 1. POST (action=sync): Batch synchronization with idempotent upsert/append
 * 2. GET (action=bootstrap): Full data export for bootstrap/restore
 * 3. GET (action=setup): Auto-initialization of sheet tabs and headers
 * 4. GET (action=ping): Health check
 */

const SHEET_NAMES = {
  USERS: 'Users',
  CATEGORIES: 'Categories',
  PRODUCTS: 'Products',
  PRICE_HISTORY: 'PriceHistory',
  TRANSACTIONS: 'Transactions',
  TRANSACTION_ITEMS: 'TransactionItems',
  STOCK_INS: 'StockIns',
  STOCK_ADJUSTMENTS: 'StockAdjustments',
  STOCK_MOVEMENTS: 'StockMovements',
  DIGITAL_TRANSACTIONS: 'DigitalTransactions'
};

const HEADERS = {
  Users: ['user_id', 'display_name', 'role', 'pin_hash', 'is_active', 'created_at', 'updated_at'],
  Categories: ['category_id', 'name', 'is_active', 'created_at', 'updated_at'],
  Products: ['product_id', 'category_id', 'name', 'barcode', 'product_kind', 'pricing_method', 'quantity_type', 'selling_unit', 'stock_unit', 'purchase_unit', 'purchase_conversion_factor', 'current_price', 'minimum_stock', 'is_active', 'created_at', 'updated_at'],
  PriceHistory: ['history_id', 'product_id', 'old_price', 'new_price', 'changed_at', 'changed_by'],
  Transactions: ['transaction_id', 'transaction_number', 'transaction_status', 'payment_status', 'payment_method', 'total_amount', 'amount_received', 'change_amount', 'created_at', 'completed_at', 'cancelled_at', 'created_by', 'cancelled_by', 'cancellation_reason'],
  TransactionItems: ['item_id', 'transaction_id', 'product_id', 'product_name_snapshot', 'quantity', 'unit_price', 'subtotal', 'selling_unit'],
  StockIns: ['stock_in_id', 'product_id', 'purchase_quantity', 'purchase_unit', 'conversion_factor', 'stock_quantity', 'note', 'created_at', 'created_by'],
  StockAdjustments: ['adjustment_id', 'product_id', 'system_quantity', 'physical_quantity', 'difference', 'reason', 'note', 'created_at', 'created_by'],
  StockMovements: ['movement_id', 'product_id', 'movement_type', 'quantity_delta', 'reference_type', 'reference_id', 'reason', 'created_at', 'created_by'],
  DigitalTransactions: ['digital_transaction_id', 'transaction_item_id', 'provider_name', 'provider_reference', 'target_account', 'status', 'created_at']
};

/**
 * Main GET request handler
 */
function doGet(e) {
  const action = (e && e.parameter && e.parameter.action) || 'ping';

  if (!validateAuth(e)) {
    return createJsonResponse({
      status: 'ERROR',
      error: 'Unauthorized: Invalid or missing sync token',
      retryable: false
    });
  }

  if (action === 'ping') {
    return createJsonResponse({
      status: 'SUCCESS',
      message: 'Toko Harmony POS Sync Gateway is running',
      timestamp: new Date().getTime()
    });
  }

  if (action === 'setup') {
    setupSheets();
    return createJsonResponse({
      status: 'SUCCESS',
      message: 'All 10 sheets and headers initialized successfully',
      timestamp: new Date().getTime()
    });
  }

  if (action === 'bootstrap') {
    return handleBootstrap();
  }

  return createJsonResponse({
    status: 'ERROR',
    error: 'Unknown GET action: ' + action,
    retryable: false
  });
}

/**
 * Main POST request handler
 */
function doPost(e) {
  const lock = LockService.getScriptLock();
  try {
    // Acquire script lock to prevent concurrent write collisions (up to 30 seconds wait)
    if (!lock.tryLock(30000)) {
      return createJsonResponse({
        status: 'FAILED',
        error: 'Could not obtain spreadsheet write lock within 30s. Sheet is busy.',
        results: []
      });
    }

    if (!e || !e.postData || !e.postData.contents) {
      return createJsonResponse({
        status: 'ERROR',
        error: 'Empty or invalid POST body',
        results: []
      });
    }

    var body;
    try {
      body = JSON.parse(e.postData.contents);
    } catch (parseErr) {
      return createJsonResponse({
        status: 'ERROR',
        error: 'Malformed JSON payload: ' + parseErr.message,
        results: []
      });
    }

    if (!validateAuth(e, body)) {
      return createJsonResponse({
        status: 'ERROR',
        error: 'Unauthorized: Invalid or missing sync token',
        results: []
      });
    }

    const operations = body.operations || [];
    const results = [];
    const ss = getSpreadsheet();

    for (var i = 0; i < operations.length; i++) {
      const op = operations[i];
      try {
        validateOperationShape(op);
        processOperation(ss, op);
        results.push({
          queueId: op.queueId,
          status: 'SUCCESS',
          error: null,
          retryable: false
        });
      } catch (err) {
        const isValidationErr = err.name === 'ValidationError';
        results.push({
          queueId: op.queueId,
          status: 'FAILED',
          error: err.message || err.toString(),
          retryable: !isValidationErr // Validation errors are non-retryable; spreadsheet/transient errors are retryable
        });
      }
    }

    return createJsonResponse({
      syncId: body.syncId || '',
      status: 'SUCCESS',
      results: results
    });

  } catch (globalErr) {
    return createJsonResponse({
      status: 'FAILED',
      error: globalErr.message || globalErr.toString(),
      results: []
    });
  } finally {
    lock.releaseLock();
  }
}

/**
 * Process a single sync operation idempotently
 */
function processOperation(ss, op) {
  const entityType = op.entityType;
  const entityId = op.entityId;
  const operation = op.operation;
  const payload = op.payload;

  const sheetName = getSheetNameForEntity(entityType);
  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) {
    sheet = ss.insertSheet(sheetName);
    const headers = HEADERS[sheetName] || [];
    if (headers.length > 0) {
      sheet.appendRow(headers);
    }
  }

  const rowValues = mapPayloadToRow(sheetName, payload);
  const existingRowIndex = findRowIndexById(sheet, entityId);

  if (existingRowIndex > 0) {
    // Row exists in remote spreadsheet
    if (operation === 'UPDATE' || operation === 'CANCEL' || operation === 'CREATE') {
      // Upsert: update existing row in place with latest state
      sheet.getRange(existingRowIndex, 1, 1, rowValues.length).setValues([rowValues]);
    }
  } else {
    // Row does not exist: append new row
    sheet.appendRow(rowValues);
  }
}

/**
 * Export full database snapshot in dependency-safe order for bootstrap/restore
 */
function handleBootstrap() {
  const ss = getSpreadsheet();

  const data = {
    users: extractRowsAsDto(ss, SHEET_NAMES.USERS, mapUserRowToDto),
    categories: extractRowsAsDto(ss, SHEET_NAMES.CATEGORIES, mapCategoryRowToDto),
    products: extractRowsAsDto(ss, SHEET_NAMES.PRODUCTS, mapProductRowToDto),
    priceHistory: extractRowsAsDto(ss, SHEET_NAMES.PRICE_HISTORY, mapPriceHistoryRowToDto),
    transactions: extractRowsAsDto(ss, SHEET_NAMES.TRANSACTIONS, mapTransactionRowToDto),
    transactionItems: extractRowsAsDto(ss, SHEET_NAMES.TRANSACTION_ITEMS, mapTransactionItemRowToDto),
    stockIns: extractRowsAsDto(ss, SHEET_NAMES.STOCK_INS, mapStockInRowToDto),
    stockAdjustments: extractRowsAsDto(ss, SHEET_NAMES.STOCK_ADJUSTMENTS, mapStockAdjustmentRowToDto),
    stockMovements: extractRowsAsDto(ss, SHEET_NAMES.STOCK_MOVEMENTS, mapStockMovementRowToDto),
    digitalTransactions: extractRowsAsDto(ss, SHEET_NAMES.DIGITAL_TRANSACTIONS, mapDigitalTransactionRowToDto)
  };

  return createJsonResponse({
    status: 'SUCCESS',
    timestamp: new Date().getTime(),
    data: data
  });
}

/**
 * Extract rows from a sheet and transform them using the DTO mapper
 */
function extractRowsAsDto(ss, sheetName, mapperFn) {
  const sheet = ss.getSheetByName(sheetName);
  if (!sheet) return [];

  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return []; // Only header or empty

  const list = [];
  for (var r = 1; r < values.length; r++) {
    const row = values[r];
    if (!row || !row[0] || String(row[0]).trim() === '') continue; // Skip empty rows
    list.push(mapperFn(row));
  }
  return list;
}

/**
 * Validate request shape for a single operation
 */
function validateOperationShape(op) {
  if (!op) {
    const err = new Error('Missing operation object');
    err.name = 'ValidationError';
    throw err;
  }
  if (!op.queueId) {
    const err = new Error('Missing queueId in operation');
    err.name = 'ValidationError';
    throw err;
  }
  if (!op.entityType) {
    const err = new Error('Missing entityType in operation ' + op.queueId);
    err.name = 'ValidationError';
    throw err;
  }
  if (!op.entityId) {
    const err = new Error('Missing entityId in operation ' + op.queueId);
    err.name = 'ValidationError';
    throw err;
  }
  if (!op.operation || !['CREATE', 'UPDATE', 'CANCEL'].includes(op.operation)) {
    const err = new Error('Invalid operation: ' + op.operation);
    err.name = 'ValidationError';
    throw err;
  }
  if (!op.payload || typeof op.payload !== 'object') {
    const err = new Error('Missing or non-object payload for operation ' + op.queueId);
    err.name = 'ValidationError';
    throw err;
  }
}

/**
 * Validate authentication token if configured in Script Properties
 */
function validateAuth(e, body) {
  var expectedToken = '';
  try {
    expectedToken = PropertiesService.getScriptProperties().getProperty('SYNC_TOKEN') || '';
  } catch (propErr) {
    // If running in local mock environment or properties unavailable, treat as unconfigured
    expectedToken = '';
  }

  if (!expectedToken || expectedToken.trim() === '') {
    // No token configured: allow access (open gateway mode for MVP)
    return true;
  }

  const queryToken = (e && e.parameter && (e.parameter.token || e.parameter.syncToken)) || '';
  const bodyToken = (body && (body.syncToken || body.token)) || '';

  return (queryToken === expectedToken || bodyToken === expectedToken);
}

/**
 * Obtain Spreadsheet instance (supports standalone script with SPREADSHEET_ID or container-bound)
 */
function getSpreadsheet() {
  var customId = '';
  try {
    customId = PropertiesService.getScriptProperties().getProperty('SPREADSHEET_ID') || '';
  } catch (propErr) {
    customId = '';
  }

  if (customId && customId.trim() !== '') {
    return SpreadsheetApp.openById(customId.trim());
  }
  return SpreadsheetApp.getActiveSpreadsheet();
}

/**
 * Initialize all 10 tabs and header rows if they do not exist
 */
function setupSheets() {
  const ss = getSpreadsheet();
  for (var sheetName in HEADERS) {
    var sheet = ss.getSheetByName(sheetName);
    if (!sheet) {
      sheet = ss.insertSheet(sheetName);
    }
    if (sheet.getLastRow() === 0) {
      sheet.appendRow(HEADERS[sheetName]);
    }
  }
}

/**
 * Find 1-indexed row number by entityId in Column A
 */
function findRowIndexById(sheet, entityId) {
  const data = sheet.getDataRange().getValues();
  for (var i = 1; i < data.length; i++) {
    if (String(data[i][0]) === String(entityId)) {
      return i + 1; // 1-indexed for SpreadsheetApp ranges
    }
  }
  return -1;
}

/**
 * Map Entity Type to Sheet Tab Name
 */
function getSheetNameForEntity(entityType) {
  switch (entityType) {
    case 'USER': return SHEET_NAMES.USERS;
    case 'CATEGORY': return SHEET_NAMES.CATEGORIES;
    case 'PRODUCT': return SHEET_NAMES.PRODUCTS;
    case 'PRICE_HISTORY': return SHEET_NAMES.PRICE_HISTORY;
    case 'TRANSACTION': return SHEET_NAMES.TRANSACTIONS;
    case 'TRANSACTION_ITEM': return SHEET_NAMES.TRANSACTION_ITEMS;
    case 'STOCK_IN': return SHEET_NAMES.STOCK_INS;
    case 'STOCK_ADJUSTMENT': return SHEET_NAMES.STOCK_ADJUSTMENTS;
    case 'STOCK_MOVEMENT': return SHEET_NAMES.STOCK_MOVEMENTS;
    case 'DIGITAL_TRANSACTION': return SHEET_NAMES.DIGITAL_TRANSACTIONS;
    default:
      const err = new Error('Unsupported entity type: ' + entityType);
      err.name = 'ValidationError';
      throw err;
  }
}

/**
 * Map incoming JSON payload to Sheet row columns
 */
function mapPayloadToRow(sheetName, p) {
  switch (sheetName) {
    case 'Users':
      return [p.userId, p.displayName, p.role, p.pinHash || '', p.isActive !== false, p.createdAt || 0, p.updatedAt || 0];
    case 'Categories':
      return [p.categoryId, p.name, p.isActive !== false, p.createdAt || 0, p.updatedAt || 0];
    case 'Products':
      return [p.productId, p.categoryId, p.name, p.barcode || '', p.productKind, p.pricingMethod, p.quantityType, p.sellingUnit, p.stockUnit, p.purchaseUnit || '', p.purchaseConversionFactor !== undefined && p.purchaseConversionFactor !== null ? p.purchaseConversionFactor : '', p.currentPrice, p.minimumStock !== undefined && p.minimumStock !== null ? p.minimumStock : '', p.isActive !== false, p.createdAt || 0, p.updatedAt || 0];
    case 'PriceHistory':
      return [p.historyId, p.productId, p.oldPrice, p.newPrice, p.changedAt || 0, p.changedBy || ''];
    case 'Transactions':
      return [p.transactionId, p.transactionNumber, p.transactionStatus, p.paymentStatus, p.paymentMethod || '', p.totalAmount, p.amountReceived !== undefined && p.amountReceived !== null ? p.amountReceived : '', p.changeAmount || 0, p.createdAt || 0, p.completedAt !== undefined && p.completedAt !== null ? p.completedAt : '', p.cancelledAt !== undefined && p.cancelledAt !== null ? p.cancelledAt : '', p.createdBy || '', p.cancelledBy || '', p.cancellationReason || ''];
    case 'TransactionItems':
      return [p.itemId, p.transactionId, p.productId, p.productNameSnapshot, p.quantity, p.unitPrice, p.subtotal, p.sellingUnit];
    case 'StockIns':
      return [p.stockInId, p.productId, p.purchaseQuantity, p.purchaseUnit, p.conversionFactor, p.stockQuantity, p.note || '', p.createdAt || 0, p.createdBy || ''];
    case 'StockAdjustments':
      return [p.adjustmentId, p.productId, p.systemQuantity, p.physicalQuantity, p.difference, p.reason, p.note || '', p.createdAt || 0, p.createdBy || ''];
    case 'StockMovements':
      return [p.movementId, p.productId, p.movementType, p.quantityDelta, p.referenceType || '', p.referenceId || '', p.reason || '', p.createdAt || 0, p.createdBy || ''];
    case 'DigitalTransactions':
      return [p.digitalTransactionId, p.transactionItemId, p.providerName || '', p.providerReference || '', p.targetAccount || '', p.status || 'PENDING', p.createdAt || 0];
    default:
      const err = new Error('Unknown sheet name: ' + sheetName);
      err.name = 'ValidationError';
      throw err;
  }
}

/**
 * DTO Row Mappers for Bootstrap / Restore
 */
function mapUserRowToDto(row) {
  return {
    userId: String(row[0]),
    displayName: String(row[1]),
    role: String(row[2]),
    pinHash: row[3] ? String(row[3]) : null,
    isActive: row[4] === true || String(row[4]).toUpperCase() === 'TRUE',
    createdAt: Number(row[5]) || 0,
    updatedAt: Number(row[6]) || 0
  };
}

function mapCategoryRowToDto(row) {
  return {
    categoryId: String(row[0]),
    name: String(row[1]),
    isActive: row[2] === true || String(row[2]).toUpperCase() === 'TRUE',
    createdAt: Number(row[3]) || 0,
    updatedAt: Number(row[4]) || 0
  };
}

function mapProductRowToDto(row) {
  return {
    productId: String(row[0]),
    categoryId: String(row[1]),
    name: String(row[2]),
    barcode: row[3] ? String(row[3]) : null,
    productKind: String(row[4]),
    pricingMethod: String(row[5]),
    quantityType: String(row[6]),
    sellingUnit: String(row[7]),
    stockUnit: String(row[8]),
    purchaseUnit: row[9] ? String(row[9]) : null,
    purchaseConversionFactor: row[10] !== '' && row[10] !== null && row[10] !== undefined ? Number(row[10]) : null,
    currentPrice: Number(row[11]) || 0,
    minimumStock: row[12] !== '' && row[12] !== null && row[12] !== undefined ? Number(row[12]) : null,
    isActive: row[13] === true || String(row[13]).toUpperCase() === 'TRUE',
    createdAt: Number(row[14]) || 0,
    updatedAt: Number(row[15]) || 0
  };
}

function mapPriceHistoryRowToDto(row) {
  return {
    historyId: String(row[0]),
    productId: String(row[1]),
    oldPrice: Number(row[2]) || 0,
    newPrice: Number(row[3]) || 0,
    changedAt: Number(row[4]) || 0,
    changedBy: String(row[5])
  };
}

function mapTransactionRowToDto(row) {
  return {
    transactionId: String(row[0]),
    transactionNumber: String(row[1]),
    transactionStatus: String(row[2]),
    paymentStatus: String(row[3]),
    paymentMethod: row[4] ? String(row[4]) : null,
    totalAmount: Number(row[5]) || 0,
    amountReceived: row[6] !== '' && row[6] !== null && row[6] !== undefined ? Number(row[6]) : null,
    changeAmount: Number(row[7]) || 0,
    createdAt: Number(row[8]) || 0,
    completedAt: row[9] !== '' && row[9] !== null && row[9] !== undefined ? Number(row[9]) : null,
    cancelledAt: row[10] !== '' && row[10] !== null && row[10] !== undefined ? Number(row[10]) : null,
    createdBy: String(row[11]),
    cancelledBy: row[12] ? String(row[12]) : null,
    cancellationReason: row[13] ? String(row[13]) : null
  };
}

function mapTransactionItemRowToDto(row) {
  return {
    itemId: String(row[0]),
    transactionId: String(row[1]),
    productId: String(row[2]),
    productNameSnapshot: String(row[3]),
    quantity: Number(row[4]) || 0,
    unitPrice: Number(row[5]) || 0,
    subtotal: Number(row[6]) || 0,
    sellingUnit: String(row[7])
  };
}

function mapStockInRowToDto(row) {
  return {
    stockInId: String(row[0]),
    productId: String(row[1]),
    purchaseQuantity: Number(row[2]) || 0,
    purchaseUnit: String(row[3]),
    conversionFactor: Number(row[4]) || 1,
    stockQuantity: Number(row[5]) || 0,
    note: row[6] ? String(row[6]) : null,
    createdAt: Number(row[7]) || 0,
    createdBy: String(row[8])
  };
}

function mapStockAdjustmentRowToDto(row) {
  return {
    adjustmentId: String(row[0]),
    productId: String(row[1]),
    systemQuantity: Number(row[2]) || 0,
    physicalQuantity: Number(row[3]) || 0,
    difference: Number(row[4]) || 0,
    reason: String(row[5]),
    note: row[6] ? String(row[6]) : null,
    createdAt: Number(row[7]) || 0,
    createdBy: String(row[8])
  };
}

function mapStockMovementRowToDto(row) {
  return {
    movementId: String(row[0]),
    productId: String(row[1]),
    movementType: String(row[2]),
    quantityDelta: Number(row[3]) || 0,
    referenceType: row[4] ? String(row[4]) : null,
    referenceId: row[5] ? String(row[5]) : null,
    reason: row[6] ? String(row[6]) : null,
    createdAt: Number(row[7]) || 0,
    createdBy: String(row[8])
  };
}

function mapDigitalTransactionRowToDto(row) {
  return {
    digitalTransactionId: String(row[0]),
    transactionItemId: String(row[1]),
    providerName: String(row[2]),
    providerReference: row[3] ? String(row[3]) : null,
    targetAccount: String(row[4]),
    status: String(row[5]),
    createdAt: Number(row[6]) || 0
  };
}

/**
 * Output helper for ContentService JSON
 */
function createJsonResponse(obj) {
  if (typeof ContentService !== 'undefined' && ContentService.createTextOutput) {
    return ContentService.createTextOutput(JSON.stringify(obj))
      .setMimeType(ContentService.MimeType.JSON);
  }
  // Fallback for local unit test environments
  return {
    mimeType: 'application/json',
    content: JSON.stringify(obj)
  };
}

// Export for local Node.js testing if running outside Google Apps Script
if (typeof module !== 'undefined' && module.exports) {
  module.exports = {
    SHEET_NAMES: SHEET_NAMES,
    HEADERS: HEADERS,
    doGet: doGet,
    doPost: doPost,
    processOperation: processOperation,
    handleBootstrap: handleBootstrap,
    setupSheets: setupSheets,
    findRowIndexById: findRowIndexById,
    mapPayloadToRow: mapPayloadToRow,
    mapUserRowToDto: mapUserRowToDto,
    mapCategoryRowToDto: mapCategoryRowToDto,
    mapProductRowToDto: mapProductRowToDto,
    mapPriceHistoryRowToDto: mapPriceHistoryRowToDto,
    mapTransactionRowToDto: mapTransactionRowToDto,
    mapTransactionItemRowToDto: mapTransactionItemRowToDto,
    mapStockInRowToDto: mapStockInRowToDto,
    mapStockAdjustmentRowToDto: mapStockAdjustmentRowToDto,
    mapStockMovementRowToDto: mapStockMovementRowToDto,
    mapDigitalTransactionRowToDto: mapDigitalTransactionRowToDto,
    validateOperationShape: validateOperationShape,
    validateAuth: validateAuth,
    createJsonResponse: createJsonResponse
  };
}
