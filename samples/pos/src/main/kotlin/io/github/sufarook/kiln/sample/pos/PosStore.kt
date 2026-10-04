package io.github.sufarook.kiln.sample.pos

import io.github.sufarook.kiln.runtime.KilnDriver
import io.github.sufarook.kiln.runtime.between
import io.github.sufarook.kiln.runtime.eq
import io.github.sufarook.kiln.runtime.gte
import io.github.sufarook.kiln.runtime.inList
import io.github.sufarook.kiln.runtime.isNotNull
import io.github.sufarook.kiln.runtime.isNull
import io.github.sufarook.kiln.runtime.like
import io.github.sufarook.kiln.runtime.lt
import io.github.sufarook.kiln.runtime.lte
import io.github.sufarook.kiln.runtime.neq
import io.github.sufarook.kiln.runtime.not
import io.github.sufarook.kiln.runtime.notInList
import io.github.sufarook.kiln.runtime.withTransaction
import io.github.sufarook.kiln.runtime.asc
import io.github.sufarook.kiln.runtime.desc
import kotlinx.coroutines.flow.Flow

class PosStore(val driver: KilnDriver) {

    private val categories = CategoryRepository(driver)
    private val products = ProductRepository(driver)
    private val customers = CustomerRepository(driver)
    private val orders = OrderRepository(driver)
    private val orderItems = OrderItemRepository(driver)
    private val payments = PaymentRepository(driver)

    init {
        KilnSchema.createAll(driver)
    }

    // ── Categories ───────────────────────────────────────────────────────────

    fun observeCategories(): Flow<List<Category>> = categories.observeAll()

    suspend fun addCategory(name: String): Category {
        val cat = Category(name = name)
        categories.insert(cat)
        return categories.findWhere { CategoryColumns.name eq name }.first()
    }

    suspend fun allCategories(): List<Category> = categories.findAll()

    // ── Products (CRUD + search + filters) ───────────────────────────────────

    fun observeProducts(): Flow<List<Product>> = products.observeAll()

    fun observeProductsByCategory(categoryId: Long): Flow<List<Product>> =
        products.observeByCategory(categoryId)

    suspend fun addProduct(
        name: String,
        sku: String,
        price: Double,
        categoryId: Long,
        stockQty: Int
    ) {
        products.insert(
            Product(
                name = name,
                sku = sku,
                price = price,
                categoryId = categoryId,
                stockQty = stockQty
            )
        )
    }

    suspend fun updateProduct(product: Product) {
        products.update(product)
    }

    suspend fun deleteProduct(id: Long) {
        products.delete(id)
    }

    suspend fun findProduct(id: Long): Product? = products.findById(id)

    suspend fun searchProducts(query: String): List<Product> =
        products.findWhere { ProductColumns.name like "%$query%" }

    suspend fun activeProducts(): List<Product> =
        products.findWhere { ProductColumns.isActive eq true }

    suspend fun lowStockProducts(threshold: Int): List<Product> =
        products.findWhere {
            (ProductColumns.stockQty lt threshold) and (ProductColumns.isActive eq true)
        }

    suspend fun productsByCategoryIds(categoryIds: List<Long>): List<Product> =
        products.findWhere { ProductColumns.categoryId inList categoryIds }

    suspend fun productsNotInCategories(excludeIds: List<Long>): List<Product> =
        products.findWhere { ProductColumns.categoryId notInList excludeIds }

    suspend fun productsByPriceRange(low: Double, high: Double): List<Product> =
        products.findWhere { ProductColumns.price.between(low, high) }

    suspend fun topProductsByPrice(limit: Long): List<Product> =
        products.findWhere(
            orderBy = listOf(ProductColumns.price.desc()),
            limit = limit
        ) { ProductColumns.isActive eq true }

    suspend fun productsPaginated(page: Int, pageSize: Int): List<Product> =
        products.findWhere(
            orderBy = listOf(ProductColumns.name.asc()),
            limit = pageSize.toLong(),
            offset = (page * pageSize).toLong()
        ) { ProductColumns.isActive eq true }

    suspend fun productCount(): Long = products.count()

    suspend fun activeProductCount(): Long =
        products.count { ProductColumns.isActive eq true }

    // ── Customers ────────────────────────────────────────────────────────────

    fun observeCustomers(): Flow<List<Customer>> = customers.observeAll()

    suspend fun addCustomer(name: String, phone: String?, email: String?) {
        customers.insert(Customer(name = name, phone = phone, email = email))
    }

    suspend fun updateCustomer(customer: Customer) {
        customers.update(customer)
    }

    suspend fun findCustomer(id: Long): Customer? = customers.findById(id)

    suspend fun searchCustomers(query: String): List<Customer> =
        customers.findWhere {
            (CustomerColumns.name like "%$query%") or
                (CustomerColumns.phone like "%$query%")
        }

    suspend fun customersWithEmail(): List<Customer> =
        customers.findWhere { CustomerColumns.email.isNotNull() }

    suspend fun customersWithoutEmail(): List<Customer> =
        customers.findWhere { CustomerColumns.email.isNull() }

    suspend fun loyalCustomers(minPoints: Int): List<Customer> =
        customers.findWhere(
            orderBy = listOf(CustomerColumns.loyaltyPoints.desc())
        ) { CustomerColumns.loyaltyPoints gte minPoints }

    // ── Orders (transactional, with relations) ───────────────────────────────

    fun observeOrders(): Flow<List<Order>> = orders.observeAll()

    fun observeOrdersByStatus(status: OrderStatus): Flow<List<Order>> =
        orders.observeWhere { OrderColumns.status eq status }

    suspend fun createOrder(
        customerId: Long?,
        items: List<Pair<Product, Int>>,
        paymentMethod: PaymentMethod,
        note: String = "",
        now: String
    ): Long {
        val totalAmount = items.sumOf { (product, qty) -> product.price * qty }

        driver.withTransaction {
            orders.insert(
                Order(
                    customerId = customerId,
                    status = OrderStatus.COMPLETED,
                    totalAmount = totalAmount,
                    createdAt = now,
                    note = note
                )
            )
            val orderId = orders.findAll().last().id

            items.forEach { (product, qty) ->
                orderItems.insert(
                    OrderItem(
                        orderId = orderId,
                        productId = product.id,
                        quantity = qty,
                        unitPrice = product.price
                    )
                )
                products.update(product.copy(stockQty = product.stockQty - qty))
            }

            payments.insert(
                Payment(
                    orderId = orderId,
                    method = paymentMethod,
                    amount = totalAmount,
                    paidAt = now
                )
            )

            if (customerId != null) {
                val customer = customers.findById(customerId)
                if (customer != null) {
                    val points = (totalAmount / 10).toInt()
                    customers.update(
                        customer.copy(loyaltyPoints = customer.loyaltyPoints + points)
                    )
                }
            }
        }

        return orders.findAll().last().id
    }

    suspend fun cancelOrder(orderId: Long) {
        val order = orders.findById(orderId) ?: return
        if (order.status != OrderStatus.PENDING && order.status != OrderStatus.COMPLETED) return

        driver.withTransaction {
            val items = orderItems.findByOrder(orderId)
            items.forEach { item ->
                val product = products.findById(item.productId)
                if (product != null) {
                    products.update(product.copy(stockQty = product.stockQty + item.quantity))
                }
            }
            orders.update(order.copy(status = OrderStatus.CANCELLED))
        }
    }

    suspend fun refundOrder(orderId: Long) {
        val order = orders.findById(orderId) ?: return
        if (order.status != OrderStatus.COMPLETED) return
        orders.update(order.copy(status = OrderStatus.REFUNDED))
    }

    suspend fun findOrder(id: Long): Order? = orders.findById(id)

    suspend fun orderItems(orderId: Long): List<OrderItem> =
        orderItems.findByOrder(orderId)

    suspend fun paymentsForOrder(orderId: Long): List<Payment> =
        payments.findByOrder(orderId)

    suspend fun walkInOrders(): List<Order> =
        orders.findWhere { OrderColumns.customerId.isNull() }

    suspend fun registeredCustomerOrders(): List<Order> =
        orders.findWhere { OrderColumns.customerId.isNotNull() }

    suspend fun ordersByDateRange(from: String, to: String): List<Order> =
        orders.findWhere(
            orderBy = listOf(OrderColumns.createdAt.desc())
        ) { OrderColumns.createdAt.between(from, to) }

    suspend fun highValueOrders(minAmount: Double): List<Order> =
        orders.findWhere { OrderColumns.totalAmount gte minAmount }

    suspend fun nonCancelledOrders(): List<Order> =
        orders.findWhere { OrderColumns.status neq OrderStatus.CANCELLED }

    suspend fun completedOrRefundedOrders(): List<Order> =
        orders.findWhere {
            (OrderColumns.status eq OrderStatus.COMPLETED) or
                (OrderColumns.status eq OrderStatus.REFUNDED)
        }

    suspend fun notPendingOrders(): List<Order> =
        orders.findWhere { not(OrderColumns.status eq OrderStatus.PENDING) }

    suspend fun recentOrders(limit: Long): List<Order> =
        orders.findWhere(
            orderBy = listOf(OrderColumns.createdAt.desc()),
            limit = limit
        ) { OrderColumns.status eq OrderStatus.COMPLETED }

    suspend fun ordersPaginated(page: Int, pageSize: Int): List<Order> =
        orders.findWhere(
            orderBy = listOf(OrderColumns.createdAt.desc()),
            limit = pageSize.toLong(),
            offset = (page * pageSize).toLong()
        ) { OrderColumns.status neq OrderStatus.CANCELLED }

    suspend fun orderCount(): Long = orders.count()

    suspend fun completedOrderCount(): Long =
        orders.count { OrderColumns.status eq OrderStatus.COMPLETED }

    suspend fun deleteCancelledOrders() {
        val cancelled = orders.findWhere { OrderColumns.status eq OrderStatus.CANCELLED }
        driver.withTransaction {
            cancelled.forEach { order ->
                orderItems.deleteByOrder(order.id)
                payments.deleteByOrder(order.id)
                orders.delete(order.id)
            }
        }
    }

    suspend fun deleteOrderItemsByProduct(productId: Long) {
        orderItems.deleteByProduct(productId)
    }

    // ── Bulk operations ──────────────────────────────────────────────────────

    suspend fun seedCategories(names: List<String>) {
        categories.insertAll(names.map { Category(name = it) })
    }

    suspend fun seedProducts(productList: List<Product>) {
        products.insertAll(productList)
    }

    // ── Seed data ────────────────────────────────────────────────────────────

    suspend fun seedIfEmpty() {
        if (categories.count() > 0L) return

        seedCategories(listOf("Beverages", "Snacks", "Dairy", "Bakery", "Produce"))

        val cats = categories.findAll()
        val catMap = cats.associateBy { it.name }

        val productList = listOf(
            Product(name = "Espresso", sku = "BEV-001", price = 3.50, categoryId = catMap["Beverages"]!!.id, stockQty = 100),
            Product(name = "Latte", sku = "BEV-002", price = 4.50, categoryId = catMap["Beverages"]!!.id, stockQty = 80),
            Product(name = "Orange Juice", sku = "BEV-003", price = 2.50, categoryId = catMap["Beverages"]!!.id, stockQty = 60),
            Product(name = "Chips", sku = "SNK-001", price = 1.99, categoryId = catMap["Snacks"]!!.id, stockQty = 200),
            Product(name = "Granola Bar", sku = "SNK-002", price = 2.49, categoryId = catMap["Snacks"]!!.id, stockQty = 150),
            Product(name = "Whole Milk", sku = "DRY-001", price = 3.99, categoryId = catMap["Dairy"]!!.id, stockQty = 40),
            Product(name = "Yogurt", sku = "DRY-002", price = 1.49, categoryId = catMap["Dairy"]!!.id, stockQty = 90),
            Product(name = "Sourdough Loaf", sku = "BAK-001", price = 5.99, categoryId = catMap["Bakery"]!!.id, stockQty = 25),
            Product(name = "Croissant", sku = "BAK-002", price = 2.99, categoryId = catMap["Bakery"]!!.id, stockQty = 50),
            Product(name = "Banana", sku = "PRD-001", price = 0.50, categoryId = catMap["Produce"]!!.id, stockQty = 300),
            Product(name = "Apple", sku = "PRD-002", price = 0.75, categoryId = catMap["Produce"]!!.id, stockQty = 250),
            Product(name = "Discontinued Tea", sku = "BEV-099", price = 1.00, categoryId = catMap["Beverages"]!!.id, stockQty = 0, isActive = false)
        )
        seedProducts(productList)

        addCustomer("Alice Johnson", "+1-555-0101", "alice@example.com")
        addCustomer("Bob Smith", "+1-555-0102", null)
        addCustomer("Walk-in", null, null)
    }
}
