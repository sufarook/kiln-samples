# Point of Sale (POS) sample

A realistic Android POS application that exercises **every** Kiln CRUD
operation, query DSL operator, and runtime feature. Use this as a reference for
what Kiln can do — and where you need raw SQL.

## Run

```sh
./gradlew :pos:installDebug
```

## Entities

Six tables model a complete POS domain:

| Entity | Key | Notable features |
|---|---|---|
| `Category` | auto-increment | `@Column(unique = true)` |
| `Product` | auto-increment | `@Relation` to Category, `@Column(index)`, Boolean column |
| `Customer` | auto-increment | nullable `phone`/`email`, `Int` loyalty points |
| `Order` | auto-increment | nullable `@Relation` to Customer (walk-ins), `OrderStatus` enum |
| `OrderItem` | **composite** (`orderId` + `productId`) | `@Relation(cascade = true)` on both, junction table |
| `Payment` | auto-increment | `PaymentMethod` enum, `@Relation` to Order |

## Kiln operations demonstrated

### Schema

| Operation | Where |
|---|---|
| `KilnSchema.createAll(driver)` | `PosStore.init` — creates + auto-migrates all 6 tables in one call |

### CRUD (CrudRepository)

| Operation | Where |
|---|---|
| `insert(entity)` | Every `add*` method in `PosStore` |
| `insertAll(list)` | `seedProducts()`, `seedCategories()` — batch insert in a single transaction |
| `update(entity)` | Stock adjustment, order status change, loyalty points, toggle `isActive` |
| `delete(id)` | `deleteProduct()` |
| `findById(id)` | `findProduct()`, `findCustomer()`, `findOrder()`, stock lookup during cancel |
| `findAll()` | `allCategories()`, getting last-inserted ID |
| `observeAll()` | `observeProducts()`, `observeCustomers()`, `observeOrders()`, `observeCategories()` |

### Query DSL (findWhere / observeWhere)

| Operator | Where |
|---|---|
| `eq` | `searchProducts` (by category), `activeProducts`, orders by status |
| `neq` | `nonCancelledOrders`, paginated orders |
| `gt` / `gte` | `loyalCustomers` (points >= threshold), `highValueOrders` |
| `lt` / `lte` | `lowStockProducts` (stock < threshold) |
| `like` | `searchProducts` (name pattern), `searchCustomers` (name or phone) |
| `isNull` | `walkInOrders` (no customer), `customersWithoutEmail` |
| `isNotNull` | `registeredCustomerOrders`, `customersWithEmail` |
| `inList` | `productsByCategoryIds` |
| `notInList` | `productsNotInCategories` |
| `between` | `productsByPriceRange`, `ordersByDateRange` |
| `and` | `lowStockProducts` (stock < N AND isActive) |
| `or` | `searchCustomers` (name OR phone), `completedOrRefundedOrders` |
| `not` | `notPendingOrders` |

### Ordering and pagination

| Feature | Where |
|---|---|
| `orderBy` + `asc()` | `productsPaginated` — alphabetical product list |
| `orderBy` + `desc()` | `topProductsByPrice`, `recentOrders`, `loyalCustomers`, `ordersByDateRange` |
| `limit` | `topProductsByPrice`, `recentOrders`, paginated queries |
| `offset` | `productsPaginated`, `ordersPaginated` — cursor-free pagination |

### Counting

| Operation | Where |
|---|---|
| `count()` | `productCount()`, `orderCount()` |
| `count { predicate }` | `activeProductCount()`, `completedOrderCount()` |

### Reactive observation

| Operation | Where |
|---|---|
| `observeAll()` | Products, customers, orders, categories — drives Compose `collectAsState` |
| `observeWhere { ... }` | `observeOrdersByStatus` — reactive filtered list |
| `observeByCategory(id)` | `observeProductsByCategory` — `@Relation` reactive helper |

### @Relation helpers

| Helper | Where |
|---|---|
| `findByCategory(id)` | Product → Category relation |
| `observeByCategory(id)` | Reactive product-by-category |
| `findByOrder(orderId)` | OrderItem → Order, Payment → Order |
| `findByProduct(productId)` | OrderItem → Product |
| `deleteByOrder(orderId)` | Cascade cleanup of order items and payments |
| `deleteByProduct(productId)` | Remove order items for a product |

### Transactions

| Scenario | Where |
|---|---|
| Multi-table atomic write | `createOrder` — inserts order, items, payment, updates stock and loyalty in one transaction |
| Atomic cancel with rollback | `cancelOrder` — restores stock for each item, updates order status |
| Batch cleanup | `deleteCancelledOrders` — deletes items, payments, and orders together |

### Raw KilnDriver (escape hatch)

`PosReports` uses `driver.executeQuery()` for operations Kiln cannot generate:

| Report | SQL features used |
|---|---|
| `dailySales()` | `SUM`, `COUNT`, `GROUP BY`, `BETWEEN`, parameterized |
| `revenueByCategory()` | `SUM`, `COUNT(DISTINCT ...)`, triple `INNER JOIN`, `GROUP BY` |
| `topSellingProducts()` | `SUM`, `INNER JOIN`, `GROUP BY`, `ORDER BY aggregate`, `LIMIT` |
| `paymentBreakdown()` | `SUM`, `COUNT`, `INNER JOIN`, `GROUP BY`, `IN (...)` |
| `totalRevenue()` | `COALESCE`, `SUM` — scalar aggregate |
| `averageOrderValue()` | `COALESCE`, `AVG` — scalar aggregate |

## Schema evolution lifecycle

Tested every migration path Kiln supports by evolving the `Product` entity on a
device with existing orders and stock data:

| Step | Change | Result |
|---|---|---|
| Add column | Added `description: String = ""` to `Product` | Fast path (`ALTER TABLE ADD COLUMN`). Existing rows get default `""`. All stock counts, orders, payments intact. |
| Rename column | `stock_qty` → `stock_count` via `@Column(migrateFrom = "stock_qty")` | Slow path (12-step table recreation). Stock values preserved — Espresso stayed at 98, Latte at 79. |
| Add table | Created `Discount` entity | New table created alongside existing ones. Zero impact on other tables. |
| Remove column | Deleted `description` property | Slow path recreation drops the column. All other data intact. |

### Not supported

- **Table rename** — `@DbEntity` has no `migrateFrom` equivalent. Changing
  `tableName` creates a new empty table; the old one stays orphaned in SQLite
  with all its data. Workaround: keep the `tableName` stable and rename only the
  Kotlin class.

All changes above were applied incrementally (rebuild → install → verify) on a
device with live data from completed orders and stock adjustments, confirming
zero data loss at every step.

## What Kiln handles vs. what needs raw SQL

### Kiln handles

- All single-table CRUD (insert, update, delete, find, observe)
- Batch insert (`insertAll`)
- Type-safe filtered queries with 14 DSL operators
- Reactive observation with automatic invalidation
- Pagination (orderBy + limit + offset)
- Counting with optional predicates
- Relation-based helpers (findBy/observeBy/deleteBy parent)
- Composite primary keys with generated key types
- Enum columns (stored as TEXT, round-tripped automatically)
- Nullable columns and nullable foreign keys
- Boolean, Int, Long, Double, String, ByteArray columns
- Schema auto-migration (add columns, rename via `migrateFrom`)
- Transactions with deferred notifications
- One-call schema setup (`KilnSchema.createAll`)

### Needs raw KilnDriver

| Gap | Workaround |
|---|---|
| Aggregate functions (`SUM`, `AVG`, `MIN`, `MAX`) | `driver.executeQuery()` with raw SQL |
| `JOIN` across tables | `driver.executeQuery()` with raw SQL |
| `GROUP BY` / `HAVING` | `driver.executeQuery()` with raw SQL |
| Upsert (`INSERT OR REPLACE`) | `findById` + `insert`/`update` in a transaction |
| Partial update (single column) | Read full entity, copy with change, `update` |
| Batch update by predicate | `findWhere` + loop `update` in a transaction |
| Batch delete by entity list | Loop `delete(id)` in a transaction |
| `EXISTS` / subqueries | `driver.executeQuery()` with raw SQL |
| `findFirst` / `findOne` | `findWhere(limit = 1) { ... }.firstOrNull()` |
| `DISTINCT` | `findAll().distinct()` or raw SQL |
| Computed / virtual columns | Not supported — compute in Kotlin |
| Full-text search (FTS) | `driver.execute()` to create FTS table, `driver.executeQuery()` to search |

## Layout

```
src/main/kotlin/.../pos/
├── Category.kt          Entity — @Column(unique)
├── Product.kt           Entity — @Relation, @Column(index), Boolean
├── Customer.kt          Entity — nullable columns, Int type
├── Order.kt             Entity — enum column, nullable @Relation
├── OrderItem.kt         Entity — composite PK, junction table, cascade
├── Payment.kt           Entity — enum column, @Relation
├── PosStore.kt          All generated-repository operations
├── PosReports.kt        Raw KilnDriver aggregate queries
├── PosApp.kt            Application — driver + store setup
└── MainActivity.kt      Compose UI — Products, Customers, Orders, Reports
```
