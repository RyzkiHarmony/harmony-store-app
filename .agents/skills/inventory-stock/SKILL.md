---
name: inventory-stock
description: Implements stock initialization, stock-in, stock-opname/adjustment, stock calculations, conversion factors, sale movements, and reversals using the Toko Kasir stock ledger. Use when changing inventory or physical stock behavior.
---
# Inventory & Stock Ledger Skill

## Reference

Read:
- `AGENTS.md`
- `docs/BUSINESS_RULES.md`
- `docs/ARCHITECTURE.md`
- relevant `docs/UX_SPEC.md` screens

## Source of truth

`StockMovement` is the source of truth.

Current stock is conceptually:

```text
INITIAL_STOCK
+ STOCK_IN
- SALE
+ SALE_REVERSAL
± ADJUSTMENT
```

Do not create a second mutable stock truth unless it is explicitly documented as a cache/derived value.

## Stock units

Store operational stock in the base stock unit.

Example:
```text
purchase: 4 DUS
conversion factor: 40 PCS/DUS
stock-in: +160 PCS
```

The conversion factor used for that stock-in must be preserved with the stock-in record/movement context so historical reconstruction remains correct.

## Initial stock

Product creation should support explicit initial stock setup.
Record initial stock as `INITIAL_STOCK` movement rather than directly setting a mutable stock field.

## Stock in

Record:
- purchase quantity
- purchase unit
- conversion factor
- resulting stock quantity
- product
- creator/time
- note when applicable

Validate that the conversion factor is valid and the resulting stock quantity is deterministic.

## Stock adjustment / opname

User enters:
- system quantity
- physical quantity
- reason
- optional note

App calculates:
```text
difference = physical - system
```

Do not ask the parent to manually calculate or enter the delta as the primary input.

Adjustment reason is mandatory, such as:
- damaged
- lost
- expired
- recording error
- stock opname
- other

## Sales

A completed physical-product sale creates a negative `SALE` movement.
A completed sale must not push stock below zero.

Cancelling a completed sale creates a corresponding `SALE_REVERSAL` movement.

Digital products never produce physical stock movements.

## Physical vs shelf stock

Do not model warehouse-to-shelf transfers in MVP.
The ledger tracks total store stock across storage/display.

## Testing focus

Test:
- initial stock
- stock-in conversion
- stock calculation
- adjustment difference
- zero/negative boundary conditions
- negative stock rejection
- sale reversal
- mixed physical/digital transactions
- auditability of movements
