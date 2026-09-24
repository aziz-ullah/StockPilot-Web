package com.stockpilot.app.data.api

import com.stockpilot.app.data.models.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface StockPilotApiService {

    // --- Authentication & Users ---
    @POST("auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<TokenResponse>

    @GET("auth/me")
    suspend fun getMe(): Response<User>

    @GET("auth/users")
    suspend fun listUsers(): Response<List<User>>

    @POST("auth/users")
    suspend fun createUser(@Body userCreate: UserCreate): Response<User>

    @PUT("auth/users/{user_id}")
    suspend fun updateUser(
        @Path("user_id") userId: Int,
        @Query("role") role: String? = null,
        @Query("is_active") isActive: Boolean? = null
    ): Response<User>

    // --- Categories ---
    @GET("categories")
    suspend fun getCategories(): Response<List<Category>>

    @POST("categories")
    suspend fun createCategory(@Body category: CategoryCreate): Response<Category>

    // --- Products & Variants ---
    @GET("products")
    suspend fun getProducts(
        @Query("category_id") categoryId: Int? = null,
        @Query("search") search: String? = null
    ): Response<List<Product>>

    @POST("products")
    suspend fun createProduct(@Body product: ProductCreate): Response<Product>

    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: Int): Response<Product>

    @GET("products/variants/all")
    suspend fun getAllVariants(@Query("search") search: String? = null): Response<List<Variant>>

    @GET("products/variants/sku/{sku}")
    suspend fun getVariantBySku(@Path("sku") sku: String): Response<Variant>

    @PUT("products/variants/{id}")
    suspend fun updateVariant(
        @Path("id") id: Int,
        @Body variant: VariantCreate
    ): Response<Variant>

    @POST("products/variants/{id}/adjust-stock")
    suspend fun adjustStock(
        @Path("id") id: Int,
        @Query("adjustment") adjustment: Int,
        @Query("notes") notes: String? = null
    ): Response<Variant>

    @GET("products/stock-movements")
    suspend fun getStockMovements(): Response<List<StockMovement>>

    // --- Suppliers ---
    @GET("purchases/suppliers")
    suspend fun getSuppliers(): Response<List<Supplier>>

    @POST("purchases/suppliers")
    suspend fun createSupplier(@Body supplier: SupplierCreate): Response<Supplier>

    // --- Purchases (Stock In) ---
    @GET("purchases")
    suspend fun getPurchases(): Response<List<Purchase>>

    @POST("purchases")
    suspend fun createPurchase(@Body purchase: PurchaseCreate): Response<Purchase>

    // --- Sales (POS Checkout) ---
    @GET("sales")
    suspend fun getSales(): Response<List<Sale>>

    @POST("sales")
    suspend fun createSale(@Body sale: SaleCreate): Response<Sale>

    @GET("sales/{id}")
    suspend fun getSale(@Path("id") id: Int): Response<Sale>

    @GET("sales/{id}/pdf")
    @Streaming
    suspend fun getSalePdf(@Path("id") id: Int): Response<ResponseBody>

    // --- Expenses ---
    @GET("expenses")
    suspend fun getExpenses(): Response<List<Expense>>

    @POST("expenses")
    suspend fun createExpense(@Body expense: ExpenseCreate): Response<Expense>

    // --- Dashboard & Analytics ---
    @GET("dashboard/kpis")
    suspend fun getDashboardKPIs(): Response<DashboardKPIs>

    @GET("dashboard/sales-trend")
    suspend fun getSalesTrend(@Query("days") days: Int = 30): Response<List<TrendChartPoint>>

    @GET("dashboard/category-sales")
    suspend fun getCategorySales(): Response<List<CategorySalesPoint>>

    @GET("dashboard/low-stock")
    suspend fun getLowStockItems(): Response<List<LowStockItem>>

    // --- Reports & Excel Export ---
    @GET("reports/export/excel")
    @Streaming
    suspend fun exportExcel(): Response<ResponseBody>
}
