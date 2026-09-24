package com.stockpilot.app.data.models

import com.google.gson.annotations.SerializedName

// --- Auth & User Models ---

data class LoginRequest(
    @SerializedName("username_or_email") val usernameOrEmail: String,
    @SerializedName("password") val password: String
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("user") val user: User
)

data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("email") val email: String,
    @SerializedName("username") val username: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("role") val role: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("created_at") val createdAt: String? = null
)

data class UserCreate(
    @SerializedName("email") val email: String,
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("role") val role: String = "Staff"
)

// --- Category Models ---

data class Category(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null
)

data class CategoryCreate(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null
)

// --- Product & Variant Models ---

data class Variant(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("product_id") val productId: Int = 0,
    @SerializedName("product_name") val productName: String? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("sku") val sku: String? = null,
    @SerializedName("color") val color: String,
    @SerializedName("size") val size: String,
    @SerializedName("cost_price") val costPrice: Double,
    @SerializedName("selling_price") val sellingPrice: Double,
    @SerializedName("stock_quantity") val stockQuantity: Int = 0,
    @SerializedName("min_stock_alert") val minStockAlert: Int = 5,
    @SerializedName("created_at") val createdAt: String? = null
)

data class VariantCreate(
    @SerializedName("sku") val sku: String? = null,
    @SerializedName("color") val color: String,
    @SerializedName("size") val size: String,
    @SerializedName("cost_price") val costPrice: Double,
    @SerializedName("selling_price") val sellingPrice: Double,
    @SerializedName("stock_quantity") val stockQuantity: Int = 0,
    @SerializedName("min_stock_alert") val minStockAlert: Int = 5
)

data class Product(
    @SerializedName("id") val id: Int,
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("variants") val variants: List<Variant> = emptyList(),
    @SerializedName("created_at") val createdAt: String? = null
)

data class ProductCreate(
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("variants") val variants: List<VariantCreate> = emptyList()
)

// --- Stock Movement Audit ---

data class StockMovement(
    @SerializedName("id") val id: Int,
    @SerializedName("variant_id") val variantId: Int,
    @SerializedName("sku") val sku: String? = null,
    @SerializedName("product_name") val productName: String? = null,
    @SerializedName("color") val color: String? = null,
    @SerializedName("size") val size: String? = null,
    @SerializedName("movement_type") val movementType: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("previous_stock") val previousStock: Int,
    @SerializedName("new_stock") val newStock: Int,
    @SerializedName("reference_id") val referenceId: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("created_by_name") val createdByName: String? = null
)

// --- Supplier Models ---

data class Supplier(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String,
    @SerializedName("contact_person") val contactPerson: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("address") val address: String? = null
)

data class SupplierCreate(
    @SerializedName("name") val name: String,
    @SerializedName("contact_person") val contactPerson: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("address") val address: String? = null
)

// --- Purchase (Stock In) Models ---

data class PurchaseItemCreate(
    @SerializedName("variant_id") val variantId: Int,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("unit_cost") val unitCost: Double
)

data class PurchaseCreate(
    @SerializedName("supplier_id") val supplierId: Int? = null,
    @SerializedName("supplier_name") val supplierName: String? = null,
    @SerializedName("purchase_date") val purchaseDate: String? = null,
    @SerializedName("payment_status") val paymentStatus: String = "Paid",
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("items") val items: List<PurchaseItemCreate>
)

data class PurchaseItem(
    @SerializedName("id") val id: Int,
    @SerializedName("variant_id") val variantId: Int,
    @SerializedName("product_name") val productName: String? = null,
    @SerializedName("color") val color: String? = null,
    @SerializedName("size") val size: String? = null,
    @SerializedName("sku") val sku: String? = null,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("unit_cost") val unitCost: Double,
    @SerializedName("total_cost") val totalCost: Double
)

data class Purchase(
    @SerializedName("id") val id: Int,
    @SerializedName("purchase_number") val purchaseNumber: String,
    @SerializedName("supplier_id") val supplierId: Int? = null,
    @SerializedName("supplier_name") val supplierName: String? = null,
    @SerializedName("purchase_date") val purchaseDate: String,
    @SerializedName("status") val status: String,
    @SerializedName("payment_status") val paymentStatus: String,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("items") val items: List<PurchaseItem> = emptyList(),
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("created_by_name") val createdByName: String? = null
)

// --- Sale (POS Checkout) Models ---

data class SaleItemCreate(
    @SerializedName("variant_id") val variantId: Int,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("unit_price") val unitPrice: Double? = null
)

data class SaleCreate(
    @SerializedName("customer_name") val customerName: String? = "Walk-in Customer",
    @SerializedName("customer_phone") val customerPhone: String? = null,
    @SerializedName("sale_date") val saleDate: String? = null,
    @SerializedName("payment_method") val paymentMethod: String = "Cash",
    @SerializedName("discount") val discount: Double = 0.0,
    @SerializedName("tax") val tax: Double = 0.0,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("items") val items: List<SaleItemCreate>
)

data class SaleItem(
    @SerializedName("id") val id: Int,
    @SerializedName("variant_id") val variantId: Int,
    @SerializedName("product_name") val productName: String? = null,
    @SerializedName("color") val color: String? = null,
    @SerializedName("size") val size: String? = null,
    @SerializedName("sku") val sku: String? = null,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("unit_cost") val unitCost: Double,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("total_cogs") val totalCogs: Double,
    @SerializedName("total_price") val totalPrice: Double
)

data class Sale(
    @SerializedName("id") val id: Int,
    @SerializedName("invoice_number") val invoiceNumber: String,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("customer_phone") val customerPhone: String? = null,
    @SerializedName("sale_date") val saleDate: String,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("status") val status: String,
    @SerializedName("subtotal") val subtotal: Double,
    @SerializedName("discount") val discount: Double,
    @SerializedName("tax") val tax: Double,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("total_cogs") val totalCogs: Double,
    @SerializedName("gross_profit") val grossProfit: Double,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("items") val items: List<SaleItem> = emptyList(),
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("created_by_name") val createdByName: String? = null
)

// --- Expense Models ---

data class ExpenseCreate(
    @SerializedName("category") val category: String,
    @SerializedName("description") val description: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("expense_date") val expenseDate: String? = null
)

data class Expense(
    @SerializedName("id") val id: Int,
    @SerializedName("category") val category: String,
    @SerializedName("description") val description: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("expense_date") val expenseDate: String,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("created_by_name") val createdByName: String? = null
)

// --- Dashboard & Analytics Models ---

data class DashboardKPIs(
    @SerializedName("total_sales_revenue") val totalSalesRevenue: Double,
    @SerializedName("total_cogs") val totalCogs: Double,
    @SerializedName("gross_profit") val grossProfit: Double,
    @SerializedName("total_expenses") val totalExpenses: Double,
    @SerializedName("net_profit") val netProfit: Double,
    @SerializedName("current_stock_valuation") val currentStockValuation: Double,
    @SerializedName("low_stock_items_count") val lowStockItemsCount: Int
)

data class TrendChartPoint(
    @SerializedName("date") val date: String,
    @SerializedName("sales") val sales: Double,
    @SerializedName("gross_profit") val grossProfit: Double,
    @SerializedName("net_profit") val netProfit: Double
)

data class CategorySalesPoint(
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("sales") val sales: Double,
    @SerializedName("quantity_sold") val quantitySold: Int
)

data class LowStockItem(
    @SerializedName("variant_id") val variantId: Int,
    @SerializedName("product_name") val productName: String,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("sku") val sku: String,
    @SerializedName("color") val color: String,
    @SerializedName("size") val size: String,
    @SerializedName("stock_quantity") val stockQuantity: Int,
    @SerializedName("min_stock_alert") val minStockAlert: Int,
    @SerializedName("status_badge") val statusBadge: String
)
