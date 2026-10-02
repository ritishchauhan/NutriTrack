package com.example.macro_tracker.data.repository

import com.example.macro_tracker.data.local.FoodLogDao
import com.example.macro_tracker.data.local.FoodLogEntity
import com.example.macro_tracker.data.local.RecipesData
import com.example.macro_tracker.data.local.WholeFoodsData
import com.example.macro_tracker.data.remote.NutritionApi
import com.example.macro_tracker.data.remote.Nutriments
import com.example.macro_tracker.data.remote.Product
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/**
 * Repository interface defining the Single Source of Truth for food logs and nutrition data.
 * All operations are strictly bound to the authenticated User ID.
 */
interface FoodRepository {
    val currentUserIdFlow: StateFlow<String?>
    fun setActiveUser(userId: String?)
    fun clearActiveSession()

    fun getFoodLogsByDate(date: LocalDate): Flow<List<FoodLogEntity>>
    fun getFoodLogsSince(days: Int): Flow<List<FoodLogEntity>>
    fun getAllFoodLogs(): Flow<List<FoodLogEntity>>
    suspend fun searchFood(query: String): Result<List<Product>>
    suspend fun fetchProductByBarcode(barcode: String): Result<Product?>
    suspend fun insertFoodLog(foodLog: FoodLogEntity)
    suspend fun saveFoodFromProduct(product: Product, mealType: String, date: LocalDate)
    suspend fun quickAddMeal(
        mealType: String,
        foodName: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        fiber: Float,
        details: String,
        date: LocalDate
    )
    suspend fun updateMealServings(foodLog: FoodLogEntity, newServings: Int)
    suspend fun deleteFoodLog(foodLog: FoodLogEntity)
    suspend fun deleteFoodLogsForDate(date: LocalDate)
    suspend fun copyMealsFromDate(sourceDate: LocalDate, targetDate: LocalDate, mealType: String? = null): Result<Int>
    suspend fun clearAllFoodLogs()
    suspend fun deleteAccountMeals(userId: String): Result<Unit>
    suspend fun syncRemoteMeals(): Result<Unit>
}

/**
 * Concrete implementation of [FoodRepository] coordinating local database, remote nutrition API,
 * and Neon Cloud Postgres backend synchronization with strict User ID isolation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FoodRepositoryImpl(
    private val foodLogDao: FoodLogDao,
    private val nutritionApi: NutritionApi,
    private val firestoreRepository: FirestoreRepository = FirestoreRepositoryImpl(),
    private val neonApiClient: com.example.macro_tracker.data.remote.neon.NeonApiClient = com.example.macro_tracker.data.remote.neon.NeonApiClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FoodRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val _currentUserIdFlow = MutableStateFlow<String?>(
        try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null && !user.isAnonymous && !user.uid.startsWith("guest_")) user.uid else null
        } catch (ignored: Exception) {
            null
        }
    )
    override val currentUserIdFlow: StateFlow<String?> = _currentUserIdFlow.asStateFlow()

    override fun setActiveUser(userId: String?) {
        val cleanId = userId?.trim()?.ifBlank { null }
        if (cleanId != null && (cleanId.startsWith("guest_") || cleanId == "guest")) {
            _currentUserIdFlow.value = null
        } else {
            _currentUserIdFlow.value = cleanId
        }
    }

    override fun clearActiveSession() {
        _currentUserIdFlow.value = null
    }

    private fun getAuthenticatedUserId(): String? {
        val active = _currentUserIdFlow.value
        if (!active.isNullOrBlank()) return active
        return try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null && !user.isAnonymous && !user.uid.startsWith("guest_")) {
                _currentUserIdFlow.value = user.uid
                user.uid
            } else {
                null
            }
        } catch (ignored: Exception) {
            null
        }
    }

    override fun getFoodLogsByDate(date: LocalDate): Flow<List<FoodLogEntity>> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        return _currentUserIdFlow.flatMapLatest { uid ->
            if (uid.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                foodLogDao.getFoodLogsByDateRange(uid, startOfDay, endOfDay)
            }
        }
    }

    override fun getFoodLogsSince(days: Int): Flow<List<FoodLogEntity>> {
        val since = LocalDate.now()
            .minusDays((days - 1).coerceAtLeast(0).toLong())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return _currentUserIdFlow.flatMapLatest { uid ->
            if (uid.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                foodLogDao.getFoodLogsSince(uid, since)
            }
        }
    }

    override fun getAllFoodLogs(): Flow<List<FoodLogEntity>> {
        return _currentUserIdFlow.flatMapLatest { uid ->
            if (uid.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                foodLogDao.getAllFoodLogs(uid)
            }
        }
    }

    private val searchCache = SimpleLruCache<String, List<Product>>(120)
    private val barcodeCache = SimpleLruCache<String, Product>(100)

    override suspend fun searchFood(query: String): Result<List<Product>> = withContext(ioDispatcher) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.success(emptyList())
        }
        val cleanQuery = trimmed.lowercase()

        // 1. In-memory LRU cache lookup for instant 0ms result on repeated/recent queries
        searchCache.get(cleanQuery)?.let { cached ->
            return@withContext Result.success(cached)
        }

        // 2. Instant local matching against fresh fruits & whole foods (NIN/USDA verified)
        val isFruitSearch = cleanQuery == "fruit" || cleanQuery == "fruits" || cleanQuery.startsWith("fruit")
        val localWholeFoodMatches = try {
            WholeFoodsData.allWholeFoods.filter { item ->
                item.name.contains(cleanQuery, ignoreCase = true) ||
                        item.category.contains(cleanQuery, ignoreCase = true) ||
                        (isFruitSearch && item.category.contains("Fruit", ignoreCase = true)) ||
                        item.tags.any { tag ->
                            tag.contains(cleanQuery, ignoreCase = true) || cleanQuery.contains(tag, ignoreCase = true)
                        }
            }.map { item ->
                Product(
                    product_name = item.name,
                    brands = item.category,
                    nutriments = Nutriments(
                        energy_kcal_100g = item.calories.toDouble(),
                        proteins_100g = item.protein.toDouble(),
                        carbohydrates_100g = item.carbs.toDouble(),
                        fat_100g = item.fat.toDouble(),
                        fiber_100g = item.fiber.toDouble()
                    ),
                    serving_size = item.servingSize
                )
            }
        } catch (ignored: Exception) {
            emptyList()
        }

        // 3. Instant local matching against 105+ curated, authentic Indian kitchen recipes
        val localRecipeMatches = try {
            RecipesData.allRecipes.filter { recipe ->
                recipe.title.contains(cleanQuery, ignoreCase = true) ||
                        recipe.category.displayName.contains(cleanQuery, ignoreCase = true) ||
                        recipe.tags.any { tag ->
                            tag.contains(cleanQuery, ignoreCase = true) || cleanQuery.contains(tag, ignoreCase = true)
                        }
            }.map { recipe ->
                Product(
                    product_name = recipe.title,
                    brands = if (recipe.isVeg) "Pure Veg Recipe" else "Healthy Recipe",
                    nutriments = Nutriments(
                        energy_kcal_100g = recipe.calories.toDouble(),
                        proteins_100g = recipe.protein.toDouble(),
                        carbohydrates_100g = recipe.carbs.toDouble(),
                        fat_100g = recipe.fat.toDouble(),
                        fiber_100g = recipe.fiber.toDouble()
                    ),
                    serving_size = "1 serving"
                )
            }
        } catch (ignored: Exception) {
            emptyList()
        }

        // 4. Remote search via OpenFoodFacts (optimized with fields and page_size)
        var networkFailed = false
        var networkError: Exception? = null
        val remoteMatches = try {
            val response = nutritionApi.searchFood(cleanQuery)
            response.products?.filter { !it.product_name.isNullOrBlank() } ?: emptyList()
        } catch (e: Exception) {
            networkFailed = true
            networkError = e
            emptyList()
        }

        // 5. Merge, prioritize exact/word matches and fresh whole foods, and deduplicate
        val allLocal = localWholeFoodMatches + localRecipeMatches
        val combined = (allLocal + remoteMatches)
            .distinctBy { (it.product_name ?: "").trim().lowercase() }
            .sortedWith(
                compareByDescending<Product> { prod ->
                    val name = (prod.product_name ?: "").lowercase()
                    val words = name.split(" ", "(", ")", "/", "-", ",").map { it.trim() }.filter { it.isNotEmpty() }
                    val brand = (prod.brands ?: "").lowercase()
                    when {
                        name == cleanQuery -> 100
                        words.any { it == cleanQuery } -> 90
                        isFruitSearch && brand.contains("fruit") -> 85
                        name.startsWith(cleanQuery) -> 80
                        words.any { it.startsWith(cleanQuery) } -> 70
                        name.contains(cleanQuery) -> 50
                        else -> 20
                    }
                }.thenByDescending { prod ->
                    val brand = (prod.brands ?: "").lowercase()
                    when {
                        brand.contains("fruit") -> 30
                        brand.contains("recipe") -> 20
                        brand.contains("protein") || brand.contains("dairy") -> 15
                        else -> 10
                    }
                }
            )
            .take(30)

        if (combined.isNotEmpty()) {
            searchCache.put(cleanQuery, combined)
            Result.success(combined)
        } else if (networkFailed && allLocal.isEmpty()) {
            Result.failure(networkError ?: RuntimeException("No food items found for \"$trimmed\""))
        } else {
            Result.success(emptyList())
        }
    }

    override suspend fun fetchProductByBarcode(barcode: String): Result<Product?> = withContext(ioDispatcher) {
        val trimmed = barcode.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Barcode is empty"))
        }

        // Check LRU cache first
        barcodeCache.get(trimmed)?.let { cached ->
            return@withContext Result.success(cached)
        }

        try {
            val response = nutritionApi.getProductByBarcode(trimmed)
            if (response.status == 1 && response.product != null) {
                barcodeCache.put(trimmed, response.product)
                Result.success(response.product)
            } else {
                Result.failure(NoSuchElementException(response.status_verbose ?: "Product not found for barcode $trimmed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun insertFoodLog(foodLog: FoodLogEntity): Unit = withContext(ioDispatcher) {
        val uid = getAuthenticatedUserId() ?: throw IllegalStateException("Must be signed in to log meals.")
        val entityWithUser = foodLog.copy(userId = uid)
        val rowId = foodLogDao.insertFoodLog(entityWithUser)
        val syncEntity = if (entityWithUser.id == 0 && rowId > 0) entityWithUser.copy(id = rowId.toInt()) else entityWithUser
        repositoryScope.launch {
            neonApiClient.uploadMeals(uid, listOf(syncEntity))
            try {
                firestoreRepository.syncFoodLog(uid, syncEntity)
            } catch (ignored: Exception) {}
        }
        Unit
    }

    override suspend fun saveFoodFromProduct(
        product: Product,
        mealType: String,
        date: LocalDate
    ): Unit = withContext(ioDispatcher) {
        val uid = getAuthenticatedUserId() ?: throw IllegalStateException("Must be signed in to log meals.")
        val selectedTimestamp = if (date == LocalDate.now()) {
            System.currentTimeMillis()
        } else {
            date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        val cal = product.nutriments?.calories ?: 0
        val pro = product.nutriments?.proteinGrams ?: 0f
        val carbs = product.nutriments?.carbsGrams ?: 0f
        val fat = product.nutriments?.fatGrams ?: 0f
        val fib = product.nutriments?.fiberGrams ?: 0f

        val brandInfo = if (!product.brands.isNullOrBlank()) " • ${product.brands}" else ""
        val details = "P ${pro.toInt()}g • C ${carbs.toInt()}g • F ${fat.toInt()}g$brandInfo"

        val entity = FoodLogEntity(
            userId = uid,
            foodName = product.product_name?.ifBlank { "Scanned Food" } ?: "Scanned Food",
            calories = cal,
            protein = pro,
            carbs = carbs,
            fat = fat,
            fiber = fib,
            timestamp = selectedTimestamp,
            mealType = mealType,
            details = details
        )
        insertFoodLog(entity)
        Unit
    }

    override suspend fun quickAddMeal(
        mealType: String,
        foodName: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        fiber: Float,
        details: String,
        date: LocalDate
    ): Unit = withContext(ioDispatcher) {
        val uid = getAuthenticatedUserId() ?: throw IllegalStateException("Must be signed in to log meals.")
        val defaultHour = when (mealType.lowercase()) {
            "breakfast" -> 8
            "lunch" -> 13
            "snack" -> 16
            else -> 20
        }
        val selectedTimestamp = if (date == LocalDate.now()) {
            System.currentTimeMillis()
        } else {
            date.atTime(defaultHour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        val entity = FoodLogEntity(
            userId = uid,
            foodName = foodName,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            fiber = fiber,
            timestamp = selectedTimestamp,
            mealType = mealType,
            details = details
        )
        insertFoodLog(entity)
        Unit
    }

    override suspend fun updateMealServings(foodLog: FoodLogEntity, newServings: Int): Unit = withContext(ioDispatcher) {
        val uid = getAuthenticatedUserId() ?: return@withContext
        if (foodLog.userId.isNotBlank() && foodLog.userId != uid) {
            throw SecurityException("Unauthorized: Cannot update meal belonging to another user.")
        }
        val safeCurrentServings = if (foodLog.servings > 0) foodLog.servings else 1
        val targetServings = newServings.coerceAtLeast(1)
        if (safeCurrentServings == targetServings) return@withContext

        val ratio = targetServings.toFloat() / safeCurrentServings.toFloat()
        val updatedCalories = kotlin.math.round(foodLog.calories * ratio).toInt().coerceAtLeast(0)
        val updatedProtein = (foodLog.protein * ratio).coerceAtLeast(0f)
        val updatedCarbs = (foodLog.carbs * ratio).coerceAtLeast(0f)
        val updatedFat = (foodLog.fat * ratio).coerceAtLeast(0f)
        val updatedFiber = (foodLog.fiber * ratio).coerceAtLeast(0f)

        val updatedLog = foodLog.copy(
            userId = uid,
            calories = updatedCalories,
            protein = updatedProtein,
            carbs = updatedCarbs,
            fat = updatedFat,
            fiber = updatedFiber,
            servings = targetServings
        )

        foodLogDao.insertFoodLog(updatedLog)
        repositoryScope.launch {
            neonApiClient.uploadSingleMeal(uid, updatedLog)
            try {
                firestoreRepository.syncFoodLog(uid, updatedLog)
            } catch (ignored: Exception) {}
        }
        Unit
    }

    override suspend fun deleteFoodLog(foodLog: FoodLogEntity): Unit = withContext(ioDispatcher) {
        val uid = getAuthenticatedUserId() ?: return@withContext
        if (foodLog.userId.isNotBlank() && foodLog.userId != uid) {
            throw SecurityException("Unauthorized: Cannot delete meal belonging to another user.")
        }
        val target = foodLog.copy(userId = uid)
        foodLogDao.deleteFoodLog(target)
        repositoryScope.launch {
            neonApiClient.deleteMeal(uid, target)
            try {
                firestoreRepository.deleteFoodLog(uid, target.id, target.timestamp, target.foodName)
            } catch (ignored: Exception) {}
        }
        Unit
    }

    override suspend fun deleteFoodLogsForDate(date: LocalDate): Unit = withContext(ioDispatcher) {
        val uid = getAuthenticatedUserId() ?: return@withContext
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        foodLogDao.deleteFoodLogsByDateRange(uid, startOfDay, endOfDay)
        repositoryScope.launch {
            neonApiClient.deleteMealsByDateRange(uid, startOfDay, endOfDay)
            try {
                firestoreRepository.deleteFoodLogsByDateRange(uid, startOfDay, endOfDay)
            } catch (ignored: Exception) {}
        }
        Unit
    }

    override suspend fun copyMealsFromDate(
        sourceDate: LocalDate,
        targetDate: LocalDate,
        mealType: String?
    ): Result<Int> = withContext(ioDispatcher) {
        val uid = getAuthenticatedUserId() ?: return@withContext Result.failure(IllegalStateException("No user logged in"))
        val startOfDay = sourceDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = sourceDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        val sourceLogs = foodLogDao.getFoodLogsByDateRange(uid, startOfDay, endOfDay).first()
        val filteredLogs = if (!mealType.isNullOrBlank() && mealType != "All") {
            sourceLogs.filter { it.mealType.equals(mealType, ignoreCase = true) }
        } else {
            sourceLogs
        }

        if (filteredLogs.isEmpty()) {
            return@withContext Result.success(0)
        }

        val targetBaseTimestamp = targetDate.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val newMeals = filteredLogs.mapIndexed { index, meal ->
            meal.copy(
                id = 0,
                userId = uid,
                timestamp = targetBaseTimestamp + (index * 1000L)
            )
        }

        foodLogDao.insertAll(newMeals)
        repositoryScope.launch {
            neonApiClient.uploadMeals(uid, newMeals)
            try {
                firestoreRepository.syncAllFoodLogsToFirestore(uid, newMeals)
            } catch (ignored: Exception) {}
        }

        Result.success(newMeals.size)
    }

    override suspend fun clearAllFoodLogs(): Unit = withContext(ioDispatcher) {
        val uid = getAuthenticatedUserId() ?: return@withContext
        foodLogDao.deleteAllFoodLogsForUser(uid)
        repositoryScope.launch {
            neonApiClient.clearUserMeals(uid)
            try {
                firestoreRepository.clearUserFoodLogs(uid)
            } catch (ignored: Exception) {}
        }
        Unit
    }

    override suspend fun deleteAccountMeals(userId: String): Result<Unit> = withContext(ioDispatcher) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Cannot delete account meals for empty userId"))
        }
        try {
            foodLogDao.deleteAllFoodLogsForUser(userId)
            neonApiClient.clearUserMeals(userId)
            firestoreRepository.clearUserFoodLogs(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncRemoteMeals(): Result<Unit> = withContext(ioDispatcher) {
        val uid = getAuthenticatedUserId() ?: return@withContext Result.success(Unit)
        try {
            val neonResult = neonApiClient.fetchMeals(uid)
            val remoteMeals = (neonResult.getOrNull() ?: emptyList()).map { it.copy(userId = uid) }
            val localLogs = foodLogDao.getAllFoodLogsList(uid)
            val localKeys = localLogs.map { "${it.timestamp}_${it.foodName.trim().lowercase()}" }.toSet()

            val missingFromLocal = remoteMeals.filter { "${it.timestamp}_${it.foodName.trim().lowercase()}" !in localKeys }
            if (missingFromLocal.isNotEmpty()) {
                foodLogDao.insertAll(missingFromLocal)
            }

            val remoteKeys = remoteMeals.map { "${it.timestamp}_${it.foodName.trim().lowercase()}" }.toSet()
            // Strictly upload only local logs tagged with this authenticated user ID
            val missingFromRemote = localLogs.filter {
                it.userId == uid && "${it.timestamp}_${it.foodName.trim().lowercase()}" !in remoteKeys
            }
            if (missingFromRemote.isNotEmpty()) {
                neonApiClient.uploadMeals(uid, missingFromRemote)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Thread-safe LRU cache backed by standard LinkedHashMap, avoiding Android framework mock limitations in unit tests.
 */
class SimpleLruCache<K, V>(private val maxSize: Int) {
    private val map = object : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxSize
        }
    }

    @Synchronized
    fun get(key: K): V? = map[key]

    @Synchronized
    fun put(key: K, value: V) {
        map[key] = value
    }

    @Synchronized
    fun clear() {
        map.clear()
    }
}

