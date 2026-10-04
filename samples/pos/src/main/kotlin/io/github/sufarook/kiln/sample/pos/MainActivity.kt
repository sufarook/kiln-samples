package io.github.sufarook.kiln.sample.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as PosApp
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PosScreen(app.store, app.reports)
                }
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    PRODUCTS("Products", Icons.Default.Inventory),
    CUSTOMERS("Customers", Icons.Default.People),
    ORDERS("Orders", Icons.Default.Receipt),
    REPORTS("Reports", Icons.Default.Assessment)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(store: PosStore, reports: PosReports) {
    var selectedTab by remember { mutableStateOf(Tab.PRODUCTS) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { store.seedIfEmpty() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kiln POS") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                Tab.PRODUCTS -> ProductsTab(store)
                Tab.CUSTOMERS -> CustomersTab(store)
                Tab.ORDERS -> OrdersTab(store)
                Tab.REPORTS -> ReportsTab(store, reports)
            }
        }
    }
}

// ── Products Tab ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductsTab(store: PosStore) {
    val scope = rememberCoroutineScope()
    val categories by store.observeCategories().collectAsState(initial = emptyList())
    val allProducts by store.observeProducts().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var filterCategoryId by remember { mutableLongStateOf(0L) }
    var showAddDialog by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Product>?>(null) }

    val displayProducts = when {
        searchResults != null -> searchResults!!
        filterCategoryId != 0L -> allProducts.filter { it.categoryId == filterCategoryId }
        else -> allProducts
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query
                if (query.isBlank()) {
                    searchResults = null
                } else {
                    scope.launch { searchResults = store.searchProducts(query) }
                }
            },
            label = { Text("Search products") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(onClick = { filterCategoryId = 0L }, label = { Text("All") })
            categories.forEach { cat ->
                AssistChip(
                    onClick = { filterCategoryId = cat.id },
                    label = { Text(cat.name) }
                )
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(displayProducts, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    category = categories.find { it.id == product.categoryId },
                    onToggleActive = {
                        scope.launch {
                            store.updateProduct(product.copy(isActive = !product.isActive))
                        }
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddProductDialog(
            categories = categories,
            onDismiss = { showAddDialog = false },
            onAdd = { name, sku, price, catId, qty ->
                scope.launch {
                    store.addProduct(name, sku, price, catId, qty)
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
fun ProductCard(product: Product, category: Category?, onToggleActive: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onToggleActive),
        colors = CardDefaults.cardColors(
            containerColor = if (product.isActive)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name + if (!product.isActive) " (inactive)" else "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${category?.name ?: "?"} · ${product.sku} · Stock: ${product.stockQty}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "$${String.format("%.2f", product.price)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AddProductDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAdd: (name: String, sku: String, price: Double, categoryId: Long, stockQty: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stockQty by remember { mutableStateOf("") }
    var selectedCatId by remember(categories) {
        mutableLongStateOf(categories.firstOrNull()?.id ?: 0L)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = stockQty, onValueChange = { stockQty = it }, label = { Text("Stock Qty") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Text("Category:", style = MaterialTheme.typography.labelMedium)
                categories.forEach { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCatId = cat.id }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (cat.id == selectedCatId) "[x] " else "[ ] ")
                        Text(cat.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val p = price.toDoubleOrNull() ?: return@TextButton
                    val q = stockQty.toIntOrNull() ?: return@TextButton
                    if (name.isBlank() || sku.isBlank() || selectedCatId == 0L) return@TextButton
                    onAdd(name, sku, p, selectedCatId, q)
                }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Customers Tab ────────────────────────────────────────────────────────────

@Composable
fun CustomersTab(store: PosStore) {
    val scope = rememberCoroutineScope()
    val customers by store.observeCustomers().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Customer>?>(null) }

    val displayCustomers = searchResults ?: customers

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query
                if (query.isBlank()) {
                    searchResults = null
                } else {
                    scope.launch { searchResults = store.searchCustomers(query) }
                }
            },
            label = { Text("Search customers") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = {
                scope.launch { searchResults = store.customersWithEmail() }
                searchQuery = "filter: has email"
            }) { Text("With email") }
            TextButton(onClick = {
                scope.launch { searchResults = store.customersWithoutEmail() }
                searchQuery = "filter: no email"
            }) { Text("No email") }
            TextButton(onClick = {
                scope.launch { searchResults = store.loyalCustomers(10) }
                searchQuery = "filter: loyal (10+ pts)"
            }) { Text("Loyal") }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(displayCustomers, key = { it.id }) { customer ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(customer.name, fontWeight = FontWeight.Medium)
                            Text(
                                listOfNotNull(customer.phone, customer.email).joinToString(" · ").ifEmpty { "No contact info" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "${customer.loyaltyPoints} pts",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.End)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add customer")
        }
    }

    if (showAddDialog) {
        AddCustomerDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, phone, email ->
                scope.launch {
                    store.addCustomer(name, phone, email)
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, phone: String?, email: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Customer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone (optional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (optional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) return@TextButton
                    onAdd(name, phone.ifBlank { null }, email.ifBlank { null })
                }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Orders Tab ───────────────────────────────────────────────────────────────

@Composable
fun OrdersTab(store: PosStore) {
    val scope = rememberCoroutineScope()
    val orders by store.observeOrders().collectAsState(initial = emptyList())
    val customers by store.observeCustomers().collectAsState(initial = emptyList())
    val products by store.observeProducts().collectAsState(initial = emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableStateOf<OrderStatus?>(null) }

    val filteredOrders by if (statusFilter != null) {
        store.observeOrdersByStatus(statusFilter!!)
    } else {
        store.observeOrders()
    }.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(onClick = { statusFilter = null }, label = { Text("All") })
            OrderStatus.entries.forEach { status ->
                AssistChip(
                    onClick = { statusFilter = status },
                    label = { Text(status.name) }
                )
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredOrders.sortedByDescending { it.createdAt }, key = { it.id }) { order ->
                val customer = customers.find { it.id == order.customerId }
                OrderCard(
                    order = order,
                    customerName = customer?.name ?: "Walk-in",
                    onCancel = { scope.launch { store.cancelOrder(order.id) } },
                    onRefund = { scope.launch { store.refundOrder(order.id) } }
                )
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.End)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "New order")
        }
    }

    if (showCreateDialog) {
        CreateOrderDialog(
            customers = customers,
            products = products.filter { it.isActive && it.stockQty > 0 },
            onDismiss = { showCreateDialog = false },
            onCreate = { customerId, items, method ->
                scope.launch {
                    store.createOrder(
                        customerId = customerId,
                        items = items,
                        paymentMethod = method,
                        now = LocalDate.now().toString()
                    )
                    showCreateDialog = false
                }
            }
        )
    }
}

@Composable
fun OrderCard(
    order: Order,
    customerName: String,
    onCancel: () -> Unit,
    onRefund: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Order #${order.id}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "$customerName · ${order.createdAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (order.note.isNotBlank()) {
                        Text(
                            order.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$${String.format("%.2f", order.totalAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        order.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (order.status) {
                            OrderStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
                            OrderStatus.REFUNDED -> MaterialTheme.colorScheme.tertiary
                            OrderStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            if (order.status == OrderStatus.COMPLETED) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onRefund) { Text("Refund") }
                }
            }
        }
    }
}

@Composable
fun CreateOrderDialog(
    customers: List<Customer>,
    products: List<Product>,
    onDismiss: () -> Unit,
    onCreate: (customerId: Long?, items: List<Pair<Product, Int>>, method: PaymentMethod) -> Unit
) {
    var selectedCustomerId by remember { mutableStateOf<Long?>(null) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    val cart = remember { mutableMapOf<Long, Int>() }
    var cartVersion by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Order") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Customer:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedCustomerId = null }
                        .padding(vertical = 2.dp)
                ) {
                    Text(if (selectedCustomerId == null) "(x) " else "( ) ")
                    Text("Walk-in")
                }
                customers.forEach { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCustomerId = c.id }
                            .padding(vertical = 2.dp)
                    ) {
                        Text(if (selectedCustomerId == c.id) "(x) " else "( ) ")
                        Text(c.name)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Payment:", style = MaterialTheme.typography.labelMedium)
                PaymentMethod.entries.forEach { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMethod = m }
                            .padding(vertical = 2.dp)
                    ) {
                        Text(if (selectedMethod == m) "(x) " else "( ) ")
                        Text(m.name)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Products:", style = MaterialTheme.typography.labelMedium)
                @Suppress("UNUSED_VARIABLE")
                val trigger = cartVersion
                products.forEach { product ->
                    val qty = cart[product.id] ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "$${String.format("%.2f", product.price)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = {
                            if (qty > 0) { cart[product.id] = qty - 1; cartVersion++ }
                        }) { Text("-") }
                        Text("$qty", modifier = Modifier.width(24.dp))
                        TextButton(onClick = {
                            if (qty < product.stockQty) { cart[product.id] = qty + 1; cartVersion++ }
                        }) { Text("+") }
                    }
                }

                val total = products.sumOf { (cart[it.id] ?: 0) * it.price }
                Text(
                    "Total: $${String.format("%.2f", total)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val items = cart.filter { it.value > 0 }.map { (productId, qty) ->
                        products.first { it.id == productId } to qty
                    }
                    if (items.isEmpty()) return@TextButton
                    onCreate(selectedCustomerId, items, selectedMethod)
                }
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Reports Tab ──────────────────────────────────────────────────────────────

@Composable
fun ReportsTab(store: PosStore, reports: PosReports) {
    val scope = rememberCoroutineScope()
    var totalRevenue by remember { mutableStateOf(0.0) }
    var avgOrderValue by remember { mutableStateOf(0.0) }
    var orderCount by remember { mutableStateOf(0L) }
    var completedCount by remember { mutableStateOf(0L) }
    var productCount by remember { mutableStateOf(0L) }
    var activeProductCount by remember { mutableStateOf(0L) }
    var categorySales by remember { mutableStateOf<List<CategorySales>>(emptyList()) }
    var topProducts by remember { mutableStateOf<List<TopProduct>>(emptyList()) }
    var paymentBreakdown by remember { mutableStateOf<List<PaymentSummary>>(emptyList()) }
    var lowStock by remember { mutableStateOf<List<Product>>(emptyList()) }

    LaunchedEffect(Unit) {
        totalRevenue = reports.totalRevenue()
        avgOrderValue = reports.averageOrderValue()
        orderCount = store.orderCount()
        completedCount = store.completedOrderCount()
        productCount = store.productCount()
        activeProductCount = store.activeProductCount()
        categorySales = reports.revenueByCategory()
        topProducts = reports.topSellingProducts(5)
        paymentBreakdown = reports.paymentBreakdown()
        lowStock = store.lowStockProducts(10)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineSmall)

        // KPI cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiCard("Revenue", "$${String.format("%.2f", totalRevenue)}", Modifier.weight(1f))
            KpiCard("Avg Order", "$${String.format("%.2f", avgOrderValue)}", Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiCard("Orders", "$orderCount ($completedCount completed)", Modifier.weight(1f))
            KpiCard("Products", "$activeProductCount / $productCount active", Modifier.weight(1f))
        }

        // Revenue by category (from raw JOIN query)
        if (categorySales.isNotEmpty()) {
            Text("Revenue by Category", style = MaterialTheme.typography.titleMedium)
            categorySales.forEach { cs ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(cs.categoryName)
                    Text("$${String.format("%.2f", cs.totalRevenue)} (${cs.orderCount} orders)")
                }
            }
        }

        // Top products (from raw aggregate query)
        if (topProducts.isNotEmpty()) {
            Text("Top Products", style = MaterialTheme.typography.titleMedium)
            topProducts.forEach { tp ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(tp.productName)
                    Text("${tp.totalQty} sold · $${String.format("%.2f", tp.totalRevenue)}")
                }
            }
        }

        // Payment breakdown (from raw GROUP BY query)
        if (paymentBreakdown.isNotEmpty()) {
            Text("Payment Methods", style = MaterialTheme.typography.titleMedium)
            paymentBreakdown.forEach { ps ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(ps.method)
                    Text("$${String.format("%.2f", ps.totalAmount)} (${ps.count}x)")
                }
            }
        }

        // Low stock alert (from findWhere with compound predicate)
        if (lowStock.isNotEmpty()) {
            Text("Low Stock Alert", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            lowStock.forEach { p ->
                Text("${p.name} (${p.sku}): ${p.stockQty} remaining")
            }
        }

        TextButton(onClick = {
            scope.launch {
                totalRevenue = reports.totalRevenue()
                avgOrderValue = reports.averageOrderValue()
                orderCount = store.orderCount()
                completedCount = store.completedOrderCount()
                productCount = store.productCount()
                activeProductCount = store.activeProductCount()
                categorySales = reports.revenueByCategory()
                topProducts = reports.topSellingProducts(5)
                paymentBreakdown = reports.paymentBreakdown()
                lowStock = store.lowStockProducts(10)
            }
        }) { Text("Refresh") }
    }
}

@Composable
fun KpiCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}
