---
name: cashier-transaction
description: Implements and verifies cashier flows including barcode scanning, manual product search, unknown barcode registration, persistent draft carts, price snapshots, cash payment, QRIS confirmation, transaction completion, and cancellation semantics. Use when modifying cashier or transaction behavior.
---
# Cashier & Transaction Skill

## Reference

Read:
- `AGENTS.md`
- `docs/BUSINESS_RULES.md`
- `docs/UX_SPEC.md`
- relevant parts of `docs/ARCHITECTURE.md`

## Core flow

```text
Scan/Search
  -> Product lookup
  -> Add to persistent DRAFT cart
  -> Edit quantity/remove
  -> Payment
  -> Validate
  -> Atomic completion
  -> Success
  -> New transaction
```

## Product lookup

1. Barcode scan is primary for barcoded products.
2. Manual search is always available.
3. Unknown barcode must not discard the current cart.
4. Registration requires admin authorization for official price/product setup.
5. After registration, return to the same draft cart and auto-add the new product when possible.
6. Debounce repeated scans.

## Cart rules

- Draft cart survives navigation and recomposition.
- Repeated scans aggregate the product quantity.
- Unit-product quantity is integer count.
- Weight-product quantity is integer grams.
- MVP weight options are 0.5 kg increments.
- Capture the current master price when a product is first added to the cart.
- Later price changes do not silently change that cart line.
- Transaction item must preserve product-name and price snapshots.

## Payment rules

### Cash

```text
change = amountReceived - totalAmount
```

Reject completion when `amountReceived < totalAmount`.

### QRIS

Selecting QRIS is not proof of payment.
The flow must require explicit cashier confirmation after actual payment verification.
Only then transition payment to `PAID` and allow completion.

## Completion

Completion must atomically:
1. validate cart
2. validate product/activity state
3. validate stock for physical products
4. validate payment
5. create/complete transaction
6. persist transaction items
7. create physical `SALE` movements
8. enqueue sync operations

Any failure must roll back the local transaction.

## Cancellation

Completed transactions are not deleted.
Admin cancellation should:
- preserve original transaction data
- mark transaction `CANCELLED`
- record cancellation metadata/reason
- create `SALE_REVERSAL` movements for physical items
- enqueue the relevant sync update

## Testing focus

Test:
- cart persistence
- aggregation
- price snapshot
- weighted subtotal
- cash validation/change
- QRIS confirmation requirement
- negative-stock prevention
- atomic completion
- cancellation/reversal
- mixed physical + digital transaction behavior
