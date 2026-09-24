# Google Apps Script Sync Gateway Deployment Guide

This directory contains the Google Apps Script Web App source code, configuration manifest, and local test suite for synchronizing the **Toko Harmony Android POS** Room database with **Google Sheets**.

---

## Architecture Overview

```text
┌─────────────────────────┐
│     Android POS App     │
│   (Room SQLite DB)      │
└────────────┬────────────┘
             │ WorkManager / HTTPS POST & GET
             ▼
┌─────────────────────────┐
│ Google Apps Script App  │
│  (Code.js / Web App)    │
└────────────┬────────────┘
             │ Append / Upsert by ID (LockService)
             ▼
┌─────────────────────────┐
│      Google Sheets      │
│   (10 Tabbed Sheets)    │
└─────────────────────────┘
```

- **Operational Source of Truth**: Local Android Room DB.
- **Remote Target**: Google Sheets (Cloud Backup / Synchronization target).
- **Transport**: JSON over HTTPS via Google Apps Script Web App.

---

## File Structure

- `Code.js`: Main Apps Script source code containing `doPost(e)` (batch sync), `doGet(e)` (bootstrap export, setup, health check), idempotency logic, and DTO row mappers.
- `appsscript.json`: Manifest configuration (time zone, V8 engine runtime, and webapp permissions).
- `test_local.js`: Offline Node.js test script to verify all gateway logic, duplicate checks, error classification, and DTO mappings without requiring a live Google account.
- `README.md`: This deployment documentation.

---

## Required Google Sheets Structure (10 Tabs)

The gateway targets **1 Google Spreadsheet** containing **10 sheet tabs** with the following header rows (or you can use the automatic setup endpoint `GET ?action=setup`):

| # | Sheet Name | Column Headers (Row 1) |
|---|---|---|
| 1 | `Users` | `user_id`, `display_name`, `role`, `pin_hash`, `is_active`, `created_at`, `updated_at` |
| 2 | `Categories` | `category_id`, `name`, `is_active`, `created_at`, `updated_at` |
| 3 | `Products` | `product_id`, `category_id`, `name`, `barcode`, `product_kind`, `pricing_method`, `quantity_type`, `selling_unit`, `stock_unit`, `purchase_unit`, `purchase_conversion_factor`, `current_price`, `minimum_stock`, `is_active`, `created_at`, `updated_at` |
| 4 | `PriceHistory` | `history_id`, `product_id`, `old_price`, `new_price`, `changed_at`, `changed_by` |
| 5 | `Transactions` | `transaction_id`, `transaction_number`, `transaction_status`, `payment_status`, `payment_method`, `total_amount`, `amount_received`, `change_amount`, `created_at`, `completed_at`, `cancelled_at`, `created_by`, `cancelled_by`, `cancellation_reason` |
| 6 | `TransactionItems`| `item_id`, `transaction_id`, `product_id`, `product_name_snapshot`, `quantity`, `unit_price`, `subtotal`, `selling_unit` |
| 7 | `StockIns` | `stock_in_id`, `product_id`, `purchase_quantity`, `purchase_unit`, `conversion_factor`, `stock_quantity`, `note`, `created_at`, `created_by` |
| 8 | `StockAdjustments`| `adjustment_id`, `product_id`, `system_quantity`, `physical_quantity`, `difference`, `reason`, `note`, `created_at`, `created_by` |
| 9 | `StockMovements` | `movement_id`, `product_id`, `movement_type`, `quantity_delta`, `reference_type`, `reference_id`, `reason`, `created_at`, `created_by` |
| 10| `DigitalTransactions` | `digital_transaction_id`, `transaction_item_id`, `provider_name`, `provider_reference`, `target_account`, `status`, `created_at` |

---

## Step-by-Step Manual Deployment Guide

### Step 1: Create a Google Spreadsheet
1. Open [Google Sheets](https://sheets.new) in your web browser.
2. Name the spreadsheet: **"Toko Harmony POS Database"** (or your preferred name).
3. Copy the **Spreadsheet ID** from the browser URL:
   `https://docs.google.com/spreadsheets/d/`**`<YOUR_SPREADSHEET_ID>`**`/edit`

### Step 2: Open Apps Script Editor
- **Option A (Container-Bound - Recommended)**:
  In your newly created Google Sheet, click **Extensions > Apps Script**.
- **Option B (Standalone)**:
  Go to [script.google.com](https://script.google.com) and create a New Project.

### Step 3: Add Code and Manifest
1. In the Apps Script editor, open `Code.gs` (or rename to `Code.js`).
2. Replace all existing text with the full content of `scripts/google_apps_script/Code.js`.
3. In Project Settings (gear icon on the left menu), check **"Show 'appsscript.json' manifest file in editor"**.
4. Switch back to the editor, open `appsscript.json`, and replace with the contents of `scripts/google_apps_script/appsscript.json`.
5. Click **Save Project** (floppy disk icon).

### Step 4: Configure Script Properties (Secrets & Config)
1. In the Apps Script left menu, click **Project Settings** (gear icon).
2. Scroll to **Script Properties** and click **Add script property**.
3. Add the following properties:
   - `SPREADSHEET_ID`: (Optional if container-bound; Required if standalone) Enter `<YOUR_SPREADSHEET_ID>`.
   - `SYNC_TOKEN`: Enter a secure random string (e.g., `harmony-secret-key-2026`).
4. Click **Save script properties**.

### Step 5: Deploy as Web App
1. At the top right of the Apps Script editor, click **Deploy > New deployment**.
2. Click the gear icon next to "Select type" and select **Web app**.
3. Fill in the deployment configuration:
   - **Description**: `Toko Harmony Sync Gateway v1`
   - **Execute as**: `Me (your-email@gmail.com)`
   - **Who has access**: `Anyone` *(Note: Access is authorized via the `SYNC_TOKEN` parameter/body)*
4. Click **Deploy**.
5. When prompted, click **Authorize access**, choose your Google account, click **Advanced**, and click **Go to Toko Harmony POS (unsafe)**.
6. Copy the generated **Web App URL** (`https://script.google.com/macros/s/.../exec`).

### Step 6: Initialize Spreadsheet Tabs (One-Time Setup)
Open your browser and navigate to:
```text
https://script.google.com/macros/s/.../exec?action=setup&token=<YOUR_SYNC_TOKEN>
```
You should see:
```json
{"status":"SUCCESS","message":"All 10 sheets and headers initialized successfully","timestamp":...}
```
Check your Google Spreadsheet; all 10 tabs with bold header columns will now be created automatically.

### Step 7: Configure Android App
1. Open the **Toko Harmony Android App**.
2. Navigate to **Menu Admin > Sinkronisasi & Backup > Pengaturan Endpoint**.
3. Enter:
   - **Server URL**: `https://script.google.com/macros/s/.../exec`
   - **Sync Token**: `<YOUR_SYNC_TOKEN>`
4. Click **Simpan Pengaturan**.
5. Click **Sinkronkan Sekarang** to run a sync, or **Pulihkan Data (Bootstrap)** to test snapshot restore.

---

## Endpoint Contract Summary

### 1. `POST /exec` (Batch Synchronization)
- **Content-Type**: `application/json`
- **Request Body**:
  ```json
  {
    "syncId": "sync-uuid",
    "deviceId": "device-uuid",
    "syncToken": "YOUR_SYNC_TOKEN",
    "timestamp": 1700000000000,
    "operations": [
      {
        "queueId": "q-1",
        "entityType": "PRODUCT",
        "entityId": "prod-001",
        "operation": "CREATE",
        "payload": { ... }
      }
    ]
  }
  ```
- **Response**:
  ```json
  {
    "syncId": "sync-uuid",
    "status": "SUCCESS",
    "results": [
      {
        "queueId": "q-1",
        "status": "SUCCESS",
        "error": null,
        "retryable": false
      }
    ]
  }
  ```

### 2. `GET /exec?action=bootstrap` (Bootstrap Snapshot)
- **Query Params**: `action=bootstrap&token=YOUR_SYNC_TOKEN`
- **Response**:
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

## Offline Local Verification
Run the offline test suite using Node.js:
```bash
node scripts/google_apps_script/test_local.js
```
Expected output:
```text
ALL LOCAL APPS SCRIPT TESTS PASSED (7/7)
```
