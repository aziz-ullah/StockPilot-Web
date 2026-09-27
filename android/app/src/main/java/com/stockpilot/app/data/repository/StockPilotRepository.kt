package com.stockpilot.app.data.repository

import com.google.gson.Gson
import com.stockpilot.app.data.api.NetworkClient
import com.stockpilot.app.data.api.StockPilotApiService
import com.stockpilot.app.data.local.TokenManager
import com.stockpilot.app.data.models.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.InputStream

class StockPilotRepository(val tokenManager: TokenManager) {

    private val api: StockPilotApiService
        get() = NetworkClient.getApiService(tokenManager)

    private val gson = Gson()

    private suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Response body was empty"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network connection failure"))
        }
    }

    private fun parseErrorMessage(errorBody: ResponseBody?): String {
        if (errorBody == null) return "Unknown server error"
        return try {
            val errorString = errorBody.string()
            val map = gson.fromJson(errorString, Map::class.java)
            val detail = map["detail"]
            detail?.toString() ?: errorString
        } catch (e: Exception) {
            "An error occurred while connecting to the server."
        }
    }

    // --- Auth & Users ---
    suspend fun login(usernameOrEmail: String, password: String): Result<TokenResponse> {
        val result = safeApiCall { api.login(LoginRequest(usernameOrEmail, password)) }
        if (result.isSuccess) {
            val tokenResponse = result.getOrNull()!!
            tokenManager.saveAuth(
                token = tokenResponse.accessToken,
                userId = tokenResponse.user.id,
                role = tokenResponse.user.role,
                name = tokenResponse.user.fullName,
                email = tokenResponse.user.email
            )
        }
        return result
    }

    suspend fun getMe(): Result<User> = safeApiCall { api.getMe() }
    suspend fun listUsers(): Result<List<User>> = safeApiCall { api.listUsers() }
    suspend fun createUser(userCreate: UserCreate): Result<User> = safeApiCall { api.createUser(userCreate) }
    suspend fun updateUser(userId: Int, role: String?, isActive: Boolean?): Result<User> =
        safeApiCall { api.updateUser(userId, role, isActive) }

    fun logout() {
        tokenManager.clearAuth()
    }

    // --- Categories ---
    suspend fun getCategories(): Result<List<Category>> = safeApiCall { api.getCategories() }
    suspend fun createCategory(name: String, description: String?): Result<Category> =
        safeApiCall { api.createCategory(CategoryCreate(name, description)) }

    // --- Products & Variants ---
    suspend fun getProducts(categoryId: Int? = null, search: String? = null): Result<List<Product>> =
        safeApiCall { api.getProducts(categoryId, search) }

    suspend fun createProduct(productCreate: ProductCreate): Result<Product> =
        safeApiCall { api.createProduct(productCreate) }

    suspend fun getProduct(id: Int): Result<Product> = safeApiCall { api.getProduct(id) }

    suspend fun getAllVariants(search: String? = null): Result<List<Variant>> =
        safeApiCall { api.getAllVariants(search) }

    suspend fun getVariantBySku(sku: String): Result<Variant> =
        safeApiCall { api.getVariantBySku(sku) }

    suspend fun updateVariant(id: Int, variantCreate: VariantCreate): Result<Variant> =
        safeApiCall { api.updateVariant(id, variantCreate) }

    suspend fun adjustStock(id: Int, adjustment: Int, notes: String?): Result<Variant> =
        safeApiCall { api.adjustStock(id, adjustment, notes) }

    suspend fun getStockMovements(): Result<List<StockMovement>> =
        safeApiCall { api.getStockMovements() }

    // --- Suppliers ---
    suspend fun getSuppliers(): Result<List<Supplier>> = safeApiCall { api.getSuppliers() }
    suspend fun createSupplier(supplierCreate: SupplierCreate): Result<Supplier> =
        safeApiCall { api.createSupplier(supplierCreate) }

    // --- Purchases ---
    suspend fun getPurchases(): Result<List<Purchase>> = safeApiCall { api.getPurchases() }
    suspend fun createPurchase(purchaseCreate: PurchaseCreate): Result<Purchase> =
        safeApiCall { api.createPurchase(purchaseCreate) }

    // --- Sales (POS) ---
    suspend fun getSales(): Result<List<Sale>> = safeApiCall { api.getSales() }
    suspend fun createSale(saleCreate: SaleCreate): Result<Sale> = safeApiCall { api.createSale(saleCreate) }
    suspend fun getSale(id: Int): Result<Sale> = safeApiCall { api.getSale(id) }
    suspend fun getSalePdfStream(id: Int): Result<InputStream> {
        return try {
            val response = api.getSalePdf(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.byteStream())
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Expenses ---
    suspend fun getExpenses(): Result<List<Expense>> = safeApiCall { api.getExpenses() }
    suspend fun createExpense(expenseCreate: ExpenseCreate): Result<Expense> =
        safeApiCall { api.createExpense(expenseCreate) }

    // --- Dashboard & KPIs ---
    suspend fun getDashboardKPIs(): Result<DashboardKPIs> = safeApiCall { api.getDashboardKPIs() }
    suspend fun getSalesTrend(days: Int = 30): Result<List<TrendChartPoint>> =
        safeApiCall { api.getSalesTrend(days) }
    suspend fun getCategorySales(): Result<List<CategorySalesPoint>> =
        safeApiCall { api.getCategorySales() }
    suspend fun getLowStockItems(): Result<List<LowStockItem>> =
        safeApiCall { api.getLowStockItems() }

    // --- Excel Export & Import ---
    suspend fun exportExcelStream(): Result<InputStream> {
        return try {
            val response = api.exportExcel()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.byteStream())
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExcelTemplateStream(): Result<InputStream> {
        return try {
            val response = api.getExcelTemplate()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.byteStream())
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importExcelFile(fileBytes: ByteArray, filename: String): Result<Map<String, Any>> {
        return try {
            val mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".toMediaTypeOrNull()
            val requestFile = fileBytes.toRequestBody(mediaType)
            val body = okhttp3.MultipartBody.Part.createFormData("file", filename, requestFile)
            safeApiCall { api.importExcelFile(body) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
