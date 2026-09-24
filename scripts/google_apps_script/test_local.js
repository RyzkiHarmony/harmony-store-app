/**
 * Local Node.js Test Suite for Toko Harmony Google Apps Script Logic
 *
 * Mocks Google Apps Script environment (SpreadsheetApp, LockService, PropertiesService, ContentService)
 * and verifies:
 * 1. Sheet setup and header initialization for all 10 tabs
 * 2. Idempotent CREATE (duplicate suppression)
 * 3. In-place UPDATE and CANCEL
 * 4. Error classification (validation error is non-retryable)
 * 5. Full bootstrap snapshot generation with camelCase DTOs
 * 6. Auth token verification
 */

const assert = require('assert');

// Mock in-memory Spreadsheet environment
class MockSheet {
  constructor(name) {
    this.name = name;
    this.rows = []; // 2D array [row][col]
  }

  appendRow(row) {
    this.rows.push([...row]);
  }

  getLastRow() {
    return this.rows.length;
  }

  getDataRange() {
    return {
      getValues: () => this.rows.map(r => [...r])
    };
  }

  getRange(rowIndex, colIndex, numRows, numCols) {
    return {
      setValues: (newValues) => {
        for (let r = 0; r < numRows; r++) {
          const targetRow = rowIndex - 1 + r;
          for (let c = 0; c < numCols; c++) {
            this.rows[targetRow][colIndex - 1 + c] = newValues[r][c];
          }
        }
      }
    };
  }
}

class MockSpreadsheet {
  constructor() {
    this.sheets = {};
  }

  getSheetByName(name) {
    return this.sheets[name] || null;
  }

  insertSheet(name) {
    const sheet = new MockSheet(name);
    this.sheets[name] = sheet;
    return sheet;
  }
}

const mockSpreadsheetInstance = new MockSpreadsheet();

// Mock GAS Globals
global.SpreadsheetApp = {
  getActiveSpreadsheet: () => mockSpreadsheetInstance,
  openById: (id) => mockSpreadsheetInstance
};

global.LockService = {
  getScriptLock: () => ({
    tryLock: (timeout) => true,
    releaseLock: () => {}
  })
};

global.PropertiesService = {
  getScriptProperties: () => ({
    getProperty: (key) => {
      if (key === 'SYNC_TOKEN') return 'test-secret-token';
      if (key === 'SPREADSHEET_ID') return 'mock-sheet-id';
      return null;
    }
  })
};

global.ContentService = {
  MimeType: { JSON: 'application/json' },
  createTextOutput: (text) => ({
    mimeType: 'application/json',
    content: text,
    setMimeType: function() { return this; }
  })
};

// Load Code.js
const code = require('./Code.js');

console.log('--- STARTING LOCAL APPS SCRIPT LOGIC TESTS ---');

// Test 1: Setup Sheets
console.log('Test 1: Auto-initialize all 10 tabs and headers...');
code.setupSheets();
const tabKeys = Object.keys(code.HEADERS);
assert.strictEqual(tabKeys.length, 10, 'Must define exactly 10 sheet tabs');
for (const tab of tabKeys) {
  const s = mockSpreadsheetInstance.getSheetByName(tab);
  assert(s !== null, `Tab ${tab} must exist`);
  assert.strictEqual(s.rows.length, 1, `Tab ${tab} must have 1 header row`);
  assert.deepStrictEqual(s.rows[0], code.HEADERS[tab], `Headers in ${tab} must match HEADERS constant`);
}
console.log('✓ Tab initialization passed.');

// Test 2: Batch POST with Valid CREATE Operations
console.log('Test 2: Batch POST with 10 entity CREATE operations...');
const batchPayload = {
  syncId: 'batch-test-001',
  deviceId: 'device-test-001',
  syncToken: 'test-secret-token',
  timestamp: 1700000000000,
  operations: [
    {
      queueId: 'q-user-1',
      entityType: 'USER',
      entityId: 'usr-001',
      operation: 'CREATE',
      payload: {
        userId: 'usr-001',
        displayName: 'Owner',
        role: 'ADMIN',
        pinHash: '1234',
        isActive: true,
        createdAt: 1700000000000,
        updatedAt: 1700000000000
      }
    },
    {
      queueId: 'q-cat-1',
      entityType: 'CATEGORY',
      entityId: 'cat-001',
      operation: 'CREATE',
      payload: {
        categoryId: 'cat-001',
        name: 'Sembako',
        isActive: true,
        createdAt: 1700000000000,
        updatedAt: 1700000000000
      }
    },
    {
      queueId: 'q-prod-1',
      entityType: 'PRODUCT',
      entityId: 'prod-001',
      operation: 'CREATE',
      payload: {
        productId: 'prod-001',
        categoryId: 'cat-001',
        name: 'Beras Premium 5kg',
        barcode: '8991234567890',
        productKind: 'PHYSICAL',
        pricingMethod: 'PER_UNIT',
        quantityType: 'COUNT',
        sellingUnit: 'sak',
        stockUnit: 'sak',
        purchaseUnit: 'Karton',
        purchaseConversionFactor: 4,
        currentPrice: 75000,
        minimumStock: 5,
        isActive: true,
        createdAt: 1700000000000,
        updatedAt: 1700000000000
      }
    },
    {
      queueId: 'q-price-1',
      entityType: 'PRICE_HISTORY',
      entityId: 'ph-001',
      operation: 'CREATE',
      payload: {
        historyId: 'ph-001',
        productId: 'prod-001',
        oldPrice: 70000,
        newPrice: 75000,
        changedAt: 1700000000000,
        changedBy: 'usr-001'
      }
    },
    {
      queueId: 'q-tx-1',
      entityType: 'TRANSACTION',
      entityId: 'tx-001',
      operation: 'CREATE',
      payload: {
        transactionId: 'tx-001',
        transactionNumber: 'TRX-20260924-001',
        transactionStatus: 'COMPLETED',
        paymentStatus: 'PAID',
        paymentMethod: 'CASH',
        totalAmount: 75000,
        amountReceived: 100000,
        changeAmount: 25000,
        createdAt: 1700000000000,
        completedAt: 1700000000000,
        cancelledAt: null,
        createdBy: 'usr-001',
        cancelledBy: null,
        cancellationReason: null
      }
    },
    {
      queueId: 'q-item-1',
      entityType: 'TRANSACTION_ITEM',
      entityId: 'item-001',
      operation: 'CREATE',
      payload: {
        itemId: 'item-001',
        transactionId: 'tx-001',
        productId: 'prod-001',
        productNameSnapshot: 'Beras Premium 5kg',
        quantity: 1,
        unitPrice: 75000,
        subtotal: 75000,
        sellingUnit: 'sak'
      }
    },
    {
      queueId: 'q-stockin-1',
      entityType: 'STOCK_IN',
      entityId: 'si-001',
      operation: 'CREATE',
      payload: {
        stockInId: 'si-001',
        productId: 'prod-001',
        purchaseQuantity: 5,
        purchaseUnit: 'Karton',
        conversionFactor: 4,
        stockQuantity: 20,
        note: 'Beli dari agen',
        createdAt: 1700000000000,
        createdBy: 'usr-001'
      }
    },
    {
      queueId: 'q-adj-1',
      entityType: 'STOCK_ADJUSTMENT',
      entityId: 'adj-001',
      operation: 'CREATE',
      payload: {
        adjustmentId: 'adj-001',
        productId: 'prod-001',
        systemQuantity: 20,
        physicalQuantity: 19,
        difference: -1,
        reason: 'Kemasan rusak',
        note: null,
        createdAt: 1700000000000,
        createdBy: 'usr-001'
      }
    },
    {
      queueId: 'q-mov-1',
      entityType: 'STOCK_MOVEMENT',
      entityId: 'mov-001',
      operation: 'CREATE',
      payload: {
        movementId: 'mov-001',
        productId: 'prod-001',
        movementType: 'SALE',
        quantityDelta: -1,
        referenceType: 'TRANSACTION',
        referenceId: 'tx-001',
        reason: 'Penjualan TRX-20260924-001',
        createdAt: 1700000000000,
        createdBy: 'usr-001'
      }
    },
    {
      queueId: 'q-dt-1',
      entityType: 'DIGITAL_TRANSACTION',
      entityId: 'dt-001',
      operation: 'CREATE',
      payload: {
        digitalTransactionId: 'dt-001',
        transactionItemId: 'item-001',
        providerName: 'DigiProvider',
        providerReference: 'REF123',
        targetAccount: '08123456789',
        status: 'SUCCESS',
        createdAt: 1700000000000
      }
    }
  ]
};

const postResult = code.doPost({
  postData: { contents: JSON.stringify(batchPayload) }
});
const postResObj = JSON.parse(postResult.content);
assert.strictEqual(postResObj.status, 'SUCCESS', 'POST must succeed');
assert.strictEqual(postResObj.results.length, 10, 'All 10 operations must have result');
for (const res of postResObj.results) {
  assert.strictEqual(res.status, 'SUCCESS', `Operation ${res.queueId} must be SUCCESS`);
}
console.log('✓ All 10 entity CREATE operations succeeded.');

// Test 3: Idempotency (Re-running same CREATE does not duplicate rows)
console.log('Test 3: Idempotency verification (Duplicate CREATE suppression)...');
const prodSheet = mockSpreadsheetInstance.getSheetByName('Products');
assert.strictEqual(prodSheet.rows.length, 2, 'Products sheet must have exactly 1 header + 1 data row');

const retryResult = code.doPost({
  postData: { contents: JSON.stringify(batchPayload) }
});
const retryResObj = JSON.parse(retryResult.content);
assert.strictEqual(retryResObj.status, 'SUCCESS');
assert.strictEqual(prodSheet.rows.length, 2, 'Products sheet MUST NOT add duplicate row for same productId');
console.log('✓ Idempotent duplicate check verified.');

// Test 4: UPDATE and CANCEL in place
console.log('Test 4: In-place UPDATE and CANCEL...');
const updatePayload = {
  syncId: 'batch-test-002',
  syncToken: 'test-secret-token',
  operations: [
    {
      queueId: 'q-prod-update',
      entityType: 'PRODUCT',
      entityId: 'prod-001',
      operation: 'UPDATE',
      payload: {
        productId: 'prod-001',
        categoryId: 'cat-001',
        name: 'Beras Premium 5kg (Updated Name)',
        barcode: '8991234567890',
        productKind: 'PHYSICAL',
        pricingMethod: 'PER_UNIT',
        quantityType: 'COUNT',
        sellingUnit: 'sak',
        stockUnit: 'sak',
        purchaseUnit: 'Karton',
        purchaseConversionFactor: 4,
        currentPrice: 80000,
        minimumStock: 10,
        isActive: true,
        createdAt: 1700000000000,
        updatedAt: 1700000050000
      }
    },
    {
      queueId: 'q-tx-cancel',
      entityType: 'TRANSACTION',
      entityId: 'tx-001',
      operation: 'CANCEL',
      payload: {
        transactionId: 'tx-001',
        transactionNumber: 'TRX-20260924-001',
        transactionStatus: 'CANCELLED',
        paymentStatus: 'PAID',
        paymentMethod: 'CASH',
        totalAmount: 75000,
        amountReceived: 100000,
        changeAmount: 25000,
        createdAt: 1700000000000,
        completedAt: 1700000000000,
        cancelledAt: 1700000060000,
        createdBy: 'usr-001',
        cancelledBy: 'usr-001',
        cancellationReason: 'Salah beli'
      }
    }
  ]
};

const updateRes = code.doPost({
  postData: { contents: JSON.stringify(updatePayload) }
});
const updateResObj = JSON.parse(updateRes.content);
assert.strictEqual(updateResObj.status, 'SUCCESS');
assert.strictEqual(prodSheet.rows.length, 2, 'Products sheet still has 2 rows');
assert.strictEqual(prodSheet.rows[1][2], 'Beras Premium 5kg (Updated Name)', 'Product name must be updated in place');
assert.strictEqual(prodSheet.rows[1][11], 80000, 'Product price must be updated in place');

const txSheet = mockSpreadsheetInstance.getSheetByName('Transactions');
assert.strictEqual(txSheet.rows[1][2], 'CANCELLED', 'Transaction status must be CANCELLED');
assert.strictEqual(txSheet.rows[1][13], 'Salah beli', 'Cancellation reason must be updated');
console.log('✓ In-place UPDATE and CANCEL verified.');

// Test 5: Validation Error (Non-retryable classification)
console.log('Test 5: Error classification (Malformed entity / invalid operation)...');
const errorPayload = {
  syncId: 'batch-error-001',
  syncToken: 'test-secret-token',
  operations: [
    {
      queueId: 'q-err-1',
      entityType: 'INVALID_TYPE',
      entityId: 'inv-001',
      operation: 'CREATE',
      payload: {}
    }
  ]
};
const errRes = code.doPost({
  postData: { contents: JSON.stringify(errorPayload) }
});
const errResObj = JSON.parse(errRes.content);
assert.strictEqual(errResObj.results[0].status, 'FAILED');
assert.strictEqual(errResObj.results[0].retryable, false, 'Validation error must be non-retryable');
console.log('✓ Error classification verified.');

// Test 6: Auth Token Verification
console.log('Test 6: Authentication validation...');
const unauthRes = code.doPost({
  postData: { contents: JSON.stringify({ syncToken: 'wrong-token', operations: [] }) }
});
const unauthResObj = JSON.parse(unauthRes.content);
assert.strictEqual(unauthResObj.status, 'ERROR');
assert(unauthResObj.error.includes('Unauthorized'), 'Must reject invalid token');
console.log('✓ Auth token check verified.');

// Test 7: Bootstrap Snapshot Generation
console.log('Test 7: Bootstrap snapshot generation in dependency-safe order...');
const bootstrapRes = code.doGet({
  parameter: { action: 'bootstrap', token: 'test-secret-token' }
});
const bootstrapObj = JSON.parse(bootstrapRes.content);
assert.strictEqual(bootstrapObj.status, 'SUCCESS');
assert(bootstrapObj.data.users.length === 1, 'Bootstrap data.users must contain 1 user');
assert(bootstrapObj.data.products.length === 1, 'Bootstrap data.products must contain 1 product');
assert(bootstrapObj.data.transactions.length === 1, 'Bootstrap data.transactions must contain 1 transaction');
assert(bootstrapObj.data.digitalTransactions.length === 1, 'Bootstrap data.digitalTransactions must contain 1 digital transaction');

// Verify camelCase DTO keys
const userDto = bootstrapObj.data.users[0];
assert.strictEqual(userDto.userId, 'usr-001');
assert.strictEqual(userDto.displayName, 'Owner');
assert.strictEqual(userDto.role, 'ADMIN');

const prodDto = bootstrapObj.data.products[0];
assert.strictEqual(prodDto.productId, 'prod-001');
assert.strictEqual(prodDto.currentPrice, 80000);
assert.strictEqual(prodDto.purchaseConversionFactor, 4);

const dtDto = bootstrapObj.data.digitalTransactions[0];
assert.strictEqual(dtDto.digitalTransactionId, 'dt-001');
assert.strictEqual(dtDto.providerName, 'DigiProvider');
assert.strictEqual(dtDto.targetAccount, '08123456789');

console.log('✓ Bootstrap snapshot generation verified with valid camelCase DTO keys.');

console.log('\n========================================');
console.log('ALL LOCAL APPS SCRIPT TESTS PASSED (7/7)');
console.log('========================================\n');
