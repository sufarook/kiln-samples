package io.github.sufarook.kiln.sample.pos

import io.github.sufarook.kiln.runtime.KilnDriver

data class CategorySales(val categoryName: String, val totalRevenue: Double, val orderCount: Long)
data class DailySales(val date: String, val totalRevenue: Double, val orderCount: Long)
data class TopProduct(val productName: String, val totalQty: Long, val totalRevenue: Double)
data class PaymentSummary(val method: String, val totalAmount: Double, val count: Long)

class PosReports(private val driver: KilnDriver) {

    fun dailySales(from: String, to: String): List<DailySales> =
        driver.executeQuery(
            identifier = null,
            sql = """
                SELECT o.created_at, SUM(o.total_amount), COUNT(*)
                FROM orders o
                WHERE o.status = 'COMPLETED' AND o.created_at BETWEEN ? AND ?
                GROUP BY o.created_at
                ORDER BY o.created_at DESC
            """.trimIndent(),
            mapper = { cursor ->
                buildList {
                    while (cursor.next()) {
                        add(
                            DailySales(
                                date = cursor.getString(0) ?: "",
                                totalRevenue = cursor.getDouble(1) ?: 0.0,
                                orderCount = cursor.getLong(2) ?: 0
                            )
                        )
                    }
                }
            },
            parameters = 2,
            binders = {
                bindString(0, from)
                bindString(1, to)
            }
        )

    fun revenueByCategory(): List<CategorySales> =
        driver.executeQuery(
            identifier = null,
            sql = """
                SELECT c.name, SUM(oi.unit_price * oi.quantity), COUNT(DISTINCT oi.order_id)
                FROM order_items oi
                INNER JOIN products p ON oi.product_id = p.id
                INNER JOIN categories c ON p.category_id = c.id
                INNER JOIN orders o ON oi.order_id = o.id
                WHERE o.status = 'COMPLETED'
                GROUP BY c.name
                ORDER BY SUM(oi.unit_price * oi.quantity) DESC
            """.trimIndent(),
            mapper = { cursor ->
                buildList {
                    while (cursor.next()) {
                        add(
                            CategorySales(
                                categoryName = cursor.getString(0) ?: "",
                                totalRevenue = cursor.getDouble(1) ?: 0.0,
                                orderCount = cursor.getLong(2) ?: 0
                            )
                        )
                    }
                }
            },
            parameters = 0
        )

    fun topSellingProducts(limit: Int): List<TopProduct> =
        driver.executeQuery(
            identifier = null,
            sql = """
                SELECT p.name, SUM(oi.quantity), SUM(oi.unit_price * oi.quantity)
                FROM order_items oi
                INNER JOIN products p ON oi.product_id = p.id
                INNER JOIN orders o ON oi.order_id = o.id
                WHERE o.status = 'COMPLETED'
                GROUP BY p.id, p.name
                ORDER BY SUM(oi.quantity) DESC
                LIMIT ?
            """.trimIndent(),
            mapper = { cursor ->
                buildList {
                    while (cursor.next()) {
                        add(
                            TopProduct(
                                productName = cursor.getString(0) ?: "",
                                totalQty = cursor.getLong(1) ?: 0,
                                totalRevenue = cursor.getDouble(2) ?: 0.0
                            )
                        )
                    }
                }
            },
            parameters = 1,
            binders = { bindLong(0, limit.toLong()) }
        )

    fun paymentBreakdown(): List<PaymentSummary> =
        driver.executeQuery(
            identifier = null,
            sql = """
                SELECT p.method, SUM(p.amount), COUNT(*)
                FROM payments p
                INNER JOIN orders o ON p.order_id = o.id
                WHERE o.status IN ('COMPLETED', 'REFUNDED')
                GROUP BY p.method
                ORDER BY SUM(p.amount) DESC
            """.trimIndent(),
            mapper = { cursor ->
                buildList {
                    while (cursor.next()) {
                        add(
                            PaymentSummary(
                                method = cursor.getString(0) ?: "",
                                totalAmount = cursor.getDouble(1) ?: 0.0,
                                count = cursor.getLong(2) ?: 0
                            )
                        )
                    }
                }
            },
            parameters = 0
        )

    fun totalRevenue(): Double =
        driver.executeQuery(
            identifier = null,
            sql = "SELECT COALESCE(SUM(total_amount), 0.0) FROM orders WHERE status = 'COMPLETED'",
            mapper = { cursor ->
                cursor.next()
                cursor.getDouble(0) ?: 0.0
            },
            parameters = 0
        )

    fun averageOrderValue(): Double =
        driver.executeQuery(
            identifier = null,
            sql = "SELECT COALESCE(AVG(total_amount), 0.0) FROM orders WHERE status = 'COMPLETED'",
            mapper = { cursor ->
                cursor.next()
                cursor.getDouble(0) ?: 0.0
            },
            parameters = 0
        )
}
