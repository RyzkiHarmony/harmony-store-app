# SYNC CONTRACT — Toko Harmony Android POS to Google Sheets Gateway

## 1. Architectural Role & Boundary

The remote synchronization system connects the local Android Room database to Google Sheets using a lightweight Google Apps Script Web App gateway.

```text
┌────────────────────────────────────────────────────────┐
│                   Android POS App                      │
│                                                        │
│  [Room Database] ──(Atomic Writes)──> [SyncQueue]      │
│         ▲                                   │          │
│         │ (Bootstrap/Restore)               ▼          │
│         └─────────────────────────── [WorkManager]     │
└─────────────────────────────────────────────┬──────────┘
                                              │ HTTPS (JSON Batch)
                                              ▼
                             ┌─────────────────────────────────┐
                             │    Apps Script Web App Gateway  │
                             └────────────────┬────────────────┘
                                              │ Append / Upsert by ID
                                              ▼
                             ┌─────────────────────────────────┐
                             │      Google Sheets Backup       │
                             │  (10 Tabs: Products, Tx, etc.) │
                             └─────────────────────────────────┘
```

### Critical Rules:
1. **Local-First & Non-Blocking**: Local transaction completion and cashier workflows never wait for remote network requests and never fail due to network/sync errors.
2. **Append-Only / Upsert by ID**: Remote Google Sheets tabs use immutable UUIDs (`entityId`). Re-transmitting the same entity (retry) never creates duplicate rows.
3. **Apps Script is a Gateway, NOT Business Logic**: Apps Script performs request parsing, duplicate checking by ID, row appending/updating, and response generation. All business rules remain in the Android domain layer.

---

## 2. Syncable Entity Types & Operations

| Entity Type | Local Source Table | Remote Sheet Tab | Supported Operations | Idempotency Key |
| :--- | :--- | :--- | :--- | :--- |
| `USER` | `users` | `Users` | `CREATE`, `UPDATE` | `userId` |
| `CATEGORY` | `categories` | `Categories` | `CREATE`, `UPDATE` | `categoryId` |
| `PRODUCT` | `products` | `Products` | `CREATE`, `UPDATE` | `productId` |
| `PRICE_HISTORY` | `price_history` | `PriceHistory` | `CREATE` | `historyId` |
| `TRANSACTION` | `transactions` | `Transactions` | `CREATE`, `UPDATE`, `CANCEL` | `transactionId` |
| `TRANSACTION_ITEM` | `transaction_items` | `TransactionItems` | `CREATE` | `itemId` |
| `STOCK_IN` | `stock_ins` | `StockIns` | `CREATE` | `stockInId` |
| `STOCK_ADJUSTMENT`| `stock_adjustments` | `StockAdjustments`| `CREATE` | `adjustmentId` |
| `STOCK_MOVEMENT` | `stock_movements` | `StockMovements` | `CREATE` | `movementId` |
| `DIGITAL_TRANSACTION` | `digital_transactions` | `DigitalTransactions` | `CREATE`, `UPDATE` | `digitalTransactionId` |

---

## 3. JSON Payloads by Entity

### 3.1 `USER`
```json
{
  "userId": "usr-admin-001",
  "displayName": "Pemilik Toko",
  "role": "ADMIN",
  "pinHash": "...",
  "isActive": true,
  "createdAt": 1700000000000,
  "updatedAt": 1700000000000
}
```

### 3.2 `CATEGORY`
```json
{
  "categoryId": "cat-001",
  "name": "Sembako",
  "isActive": true,
  "createdAt": 1700000000000,
  "updatedAt": 1700000000000
}
```

### 3.3 `PRODUCT`
```json
{
  "productId": "prod-001",
  "categoryId": "cat-001",
  "name": "Beras Premium 5kg",
  "barcode": "8991234567890",
  "productKind": "PHYSICAL",
  "pricingMethod": "PER_UNIT",
  "quantityType": "COUNT",
  "sellingUnit": "sak",
  "stockUnit": "sak",
  "purchaseUnit": "Karton",
  "purchaseConversionFactor": 4,
  "currentPrice": 75000,
  "minimumStock": 5,
  "isActive": true,
  "createdAt": 1700000000000,
  "updatedAt": 1700000000000
}
```

### 3.4 `PRICE_HISTORY`
```json
{
  "historyId": "hist-001",
  "productId": "prod-001",
  "oldPrice": 70000,
  "newPrice": 75000,
  "changedAt": 1700000000000,
  "changedBy": "usr-admin-001"
}
```

### 3.5 `TRANSACTION`
```json
{
  "transactionId": "tx-001",
  "transactionNumber": "TRX-20260924-001",
  "transactionStatus": "COMPLETED",
  "paymentStatus": "PAID",
  "paymentMethod": "CASH",
  "totalAmount": 75000,
  "amountReceived": 100000,
  "changeAmount": 25000,
  "createdAt": 1700000000000,
  "completedAt": 1700000000000,
  "cancelledAt": null,
  "createdBy": "usr-cashier-001",
  "cancelledBy": null,
  "cancellationReason": null
}
```

### 3.6 `TRANSACTION_ITEM`
```json
{
  "itemId": "item-001",
  "transactionId": "tx-001",
  "productId": "prod-001",
  "productNameSnapshot": "Beras Premium 5kg",
  "quantity": 1,
  "unitPrice": 75000,
  "subtotal": 75000,
  "sellingUnit": "sak"
}
```

### 3.7 `STOCK_IN`
```json
{
  "stockInId": "si-001",
  "productId": "prod-001",
  "purchaseQuantity": 5,
  "purchaseUnit": "Karton",
  "conversionFactor": 4,
  "stockQuantity": 20,
  "note": "Beli dari Agen",
  "createdAt": 1700000000000,
  "createdBy": "usr-admin-001"
}
```

### 3.8 `STOCK_ADJUSTMENT`
```json
{
  "adjustmentId": "adj-001",
  "productId": "prod-001",
  "systemQuantity": 20,
  "physicalQuantity": 19,
  "difference": -1,
  "reason": "Barang rusak",
  "note": "1 sak bocor",
  "createdAt": 1700000000000,
  "createdBy": "usr-admin-001"
}
```

### 3.9 `STOCK_MOVEMENT`
```json
{
  "movementId": "mov-001",
  "productId": "prod-001",
  "movementType": "SALE",
  "quantityDelta": -1,
  "referenceType": "TRANSACTION",
  "referenceId": "tx-001",
  "reason": "Penjualan TRX-20260924-001",
  "createdAt": 1700000000000,
  "createdBy": "usr-cashier-001"
}
```

### 3.10 `DIGITAL_TRANSACTION`
```json
{
  "digitalTransactionId": "dt-001",
  "transactionItemId": "item-001",
  "providerName": "DigiProvider",
  "providerReference": "REF12345",
  "targetAccount": "081234567890",
  "status": "SUCCESS",
  "createdAt": 1700000000000
}
```

---

## 4. API Endpoints Contract

### 4.1 Batch Synchronization (`POST /sync` or `POST ?action=sync`)

#### Request Body
```json
{
  "syncId": "sync-batch-uuid",
  "deviceId": "device-uuid-or-id",
  "timestamp": 1700000000000,
  "operations": [
    {
      "queueId": "queue-001",
      "entityType": "PRODUCT",
      "entityId": "prod-001",
      "operation": "CREATE",
      "payload": { ... }
    },
    {
      "queueId": "queue-002",
      "entityType": "TRANSACTION",
      "entityId": "tx-001",
      "operation": "CREATE",
      "payload": { ... }
    }
  ]
}
```

#### Response Body
```json
{
  "syncId": "sync-batch-uuid",
  "status": "SUCCESS",
  "results": [
    {
      "queueId": "queue-001",
      "status": "SUCCESS",
      "error": null,
      "retryable": false
    },
    {
      "queueId": "queue-002",
      "status": "FAILED",
      "error": "Sheet lock timeout",
      "retryable": true
    }
  ]
}
```

### 4.2 Data Bootstrap / Restore (`GET /bootstrap` or `GET ?action=bootstrap`)

#### Request
`GET https://script.google.com/macros/s/.../exec?action=bootstrap`

#### Response Body
```json
{
  "status": "SUCCESS",
  "timestamp": 1700000000000,
  "data": {
    "users": [ ... ],
    "categories": [ ... ],
    "products": [ ... ],
    "priceHistory": [ ... ],
    "transactions": [ ... ],
    "transactionItems": [ ... ],
    "stockIns": [ ... ],
    "stockAdjustments": [ ... ],
    "stockMovements": [ ... ],
    "digitalTransactions": [ ... ]
  }
}
```

---

## 5. Idempotency & Error Handling Strategy

### 5.1 Remote Idempotency (Google Sheets)
- For every item in a batch, Apps Script checks if `entityId` exists in column A (or designated ID column) of the target sheet:
  - If found for `CREATE`: Treats as already applied, updates fields if appropriate or leaves existing row unchanged, returns `SUCCESS`.
  - If found for `UPDATE`/`CANCEL`: Replaces row data with updated payload, returns `SUCCESS`.
  - If not found for `CREATE`: Appends new row, returns `SUCCESS`.

### 5.2 Local Queue States & Retry Classification
- `PENDING`: Enqueued and waiting for transmission.
- `SYNCING`: Currently in flight. Stale items in `SYNCING` older than 5 minutes (due to process death/crash) are reset to `PENDING` by `ResetStaleSyncingUseCase`.
- `SYNCED`: Successfully processed remotely.
- `FAILED`: Permanent non-retryable failure or maximum retry attempts exceeded.

#### Error Classification:
- **RETRYABLE**: `ConnectException`, `SocketTimeoutException`, `UnknownHostException`, 502/503/504 HTTP status, or response `retryable = true`. WorkManager schedules exponential backoff.
- **NON-RETRYABLE**: 400 Bad Request, malformed JSON payload, invalid entity schema, or explicit permanent rejection from server. Marked as `FAILED` with error reason without infinite retries.

---

## 6. Bootstrap / Restore Dependency Ordering

When restoring a database from Google Sheets backup:
1. `users`
2. `categories`
3. `products`
4. `price_history`
5. `transactions`
6. `transaction_items`
7. `stock_ins`
8. `stock_adjustments`
9. `stock_movements`
10. `digital_transactions`

Foreign key integrity is preserved; stable UUIDs are retained directly.

---

## 7. Security Assumptions & Trade-offs

- **Transport**: Encrypted over standard HTTPS to Google Apps Script.
- **Token/Key**: Optional Web App access token supported via request header / query parameter.
- **Trust Boundary**: Apps Script acts as an open endpoint / shared family store gateway. It does not replace enterprise OAuth2/PostgreSQL backend but is suitable for small family store requirements within the MVP.
