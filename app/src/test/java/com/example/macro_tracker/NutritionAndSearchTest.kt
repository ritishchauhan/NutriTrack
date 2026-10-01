package com.example.macro_tracker

import com.example.macro_tracker.data.local.FoodLogDao
import com.example.macro_tracker.data.local.FoodLogEntity
import com.example.macro_tracker.data.remote.NutritionApi
import com.example.macro_tracker.data.remote.Nutriments
import com.example.macro_tracker.data.remote.Product
import com.example.macro_tracker.data.remote.SearchResponse
import com.example.macro_tracker.data.repository.FoodRepositoryImpl
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class NutritionAndSearchTest {

    @Test
    fun testDirectKcalResolution() {
        val nutriments = Nutriments(
            energy_kcal_100g = 250.4,
            proteins_100g = 15.0,
            carbohydrates_100g = 30.0,
            fat_100g = 5.0
        )
        assertEquals(250, nutriments.calories)
        assertEquals(15f, nutriments.proteinGrams, 0.01f)
        assertEquals(30f, nutriments.carbsGrams, 0.01f)
        assertEquals(5f, nutriments.fatGrams, 0.01f)
    }

    @Test
    fun testKjToKcalConversion() {
        // 1566 kJ / 4.184 = 374.28 kcal -> 374
        val nutriments = Nutriments(
            energy_100g = 1566.0,
            energy_unit = "kJ",
            proteins_100g = 13.5,
            carbohydrates_100g = 58.7,
            fat_100g = 7.0
        )
        assertEquals(374, nutriments.calories)
    }

    @Test
    fun testAtwaterGeneralFactorFallbackWhenEnergyMissing() {
        // When OpenFoodFacts has no energy fields at all, Atwater formula computes:
        // Protein (20g * 4) + Carbs (30g * 4) + Fat (10g * 9) + Fiber (5g * 2) = 80 + 120 + 90 + 10 = 300 kcal
        val nutriments = Nutriments(
            energy_kcal = null,
            energy_kcal_100g = null,
            energy_100g = null,
            energy = null,
            proteins_100g = 20.0,
            carbohydrates_100g = 30.0,
            fat_100g = 10.0,
            fiber_100g = 5.0
        )
        assertEquals(300, nutriments.calories)
        assertEquals(20f, nutriments.proteinGrams, 0.01f)
        assertEquals(30f, nutriments.carbsGrams, 0.01f)
        assertEquals(10f, nutriments.fatGrams, 0.01f)
        assertEquals(5f, nutriments.fiberGrams, 0.01f)
    }

    @Test
    fun testServingAndValueFieldFallbacks() {
        val nutriments = Nutriments(
            proteins_serving = 18.0,
            carbohydrates_value = 24.0,
            fat_serving = 8.0,
            fiber_value = 4.0
        )
        // 18*4 + 24*4 + 8*9 + 4*2 = 72 + 96 + 72 + 8 = 248 kcal
        assertEquals(18f, nutriments.proteinGrams, 0.01f)
        assertEquals(24f, nutriments.carbsGrams, 0.01f)
        assertEquals(8f, nutriments.fatGrams, 0.01f)
        assertEquals(4f, nutriments.fiberGrams, 0.01f)
        assertEquals(248, nutriments.calories)
    }

    @Test
    fun testHybridSearchReturnsLocalRecipesEvenWhenNetworkOffline() = runTest {
        val offlineApi = object : NutritionApi {
            override suspend fun searchFood(query: String): SearchResponse {
                throw java.io.IOException("Network unreachable")
            }

            override suspend fun getProductByBarcode(barcode: String): com.example.macro_tracker.data.remote.BarcodeLookupResponse {
                throw java.io.IOException("Network unreachable")
            }
        }

        val dummyDao = createDummyDao()
        val repo = FoodRepositoryImpl(
            foodLogDao = dummyDao,
            nutritionApi = offlineApi
        )

        // Search for "chilla" - an authentic Indian breakfast recipe in RecipesData
        val result = repo.searchFood("chilla")
        assertTrue(result.isSuccess)
        val products = result.getOrNull()!!
        assertTrue("Should return local recipes even offline", products.isNotEmpty())
        assertTrue(products.any { it.product_name?.contains("Chilla", ignoreCase = true) == true })
        // Check that calories and macros are properly populated on the local recipe product
        val chilla = products.first { it.product_name?.contains("Chilla", ignoreCase = true) == true }
        assertTrue("Calories should be > 0", (chilla.nutriments?.calories ?: 0) > 0)
        assertTrue("Protein should be > 0", (chilla.nutriments?.proteinGrams ?: 0f) > 0f)
    }

    @Test
    fun testSearchLruCachePreventsDuplicateNetworkCalls() = runTest {
        val networkCallCount = AtomicInteger(0)
        val mockApi = object : NutritionApi {
            override suspend fun searchFood(query: String): SearchResponse {
                networkCallCount.incrementAndGet()
                return SearchResponse(
                    products = listOf(
                        Product(
                            product_name = "Rolled Oats 500g",
                            brands = "Quaker",
                            nutriments = Nutriments(
                                energy_kcal_100g = 370.0,
                                proteins_100g = 12.0,
                                carbohydrates_100g = 60.0,
                                fat_100g = 7.0
                            )
                        )
                    )
                )
            }

            override suspend fun getProductByBarcode(barcode: String): com.example.macro_tracker.data.remote.BarcodeLookupResponse {
                throw UnsupportedOperationException()
            }
        }

        val dummyDao = createDummyDao()
        val repo = FoodRepositoryImpl(
            foodLogDao = dummyDao,
            nutritionApi = mockApi
        )

        // First call triggers network
        val result1 = repo.searchFood("quaker oats")
        assertTrue(result1.isSuccess)
        assertEquals(1, networkCallCount.get())

        // Second call for the same query should hit LRU cache and NOT trigger network again
        val result2 = repo.searchFood("quaker oats")
        assertTrue(result2.isSuccess)
        assertEquals(1, networkCallCount.get())
    }

    @Test
    fun testBananaSearchReturnsFreshBananaWithAccurateMacros() = runTest {
        val mockApi = object : NutritionApi {
            override suspend fun searchFood(query: String) = SearchResponse(products = emptyList())
            override suspend fun getProductByBarcode(barcode: String) = throw UnsupportedOperationException()
        }

        val repo = FoodRepositoryImpl(foodLogDao = createDummyDao(), nutritionApi = mockApi)

        // 1. Searching for "banana" must return Fresh Banana
        val result = repo.searchFood("banana")
        assertTrue(result.isSuccess)
        val products = result.getOrNull()!!
        assertTrue("Banana search must return items", products.isNotEmpty())

        val topBanana = products.first()
        assertTrue("Top banana result should be fresh banana", topBanana.product_name?.contains("Banana", ignoreCase = true) == true)
        assertEquals(105, topBanana.nutriments?.calories)
        assertEquals(1.3f, topBanana.nutriments?.proteinGrams ?: 0f, 0.05f)
        assertEquals(27.0f, topBanana.nutriments?.carbsGrams ?: 0f, 0.05f)
        assertEquals(0.3f, topBanana.nutriments?.fatGrams ?: 0f, 0.05f)
        assertEquals(3.1f, topBanana.nutriments?.fiberGrams ?: 0f, 0.05f)
    }

    @Test
    fun testFruitAndFruitsQueryReturnsComprehensiveFruitsList() = runTest {
        val mockApi = object : NutritionApi {
            override suspend fun searchFood(query: String) = SearchResponse(products = emptyList())
            override suspend fun getProductByBarcode(barcode: String) = throw UnsupportedOperationException()
        }

        val repo = FoodRepositoryImpl(foodLogDao = createDummyDao(), nutritionApi = mockApi)

        // 1. Test "fruit" query
        val fruitResult = repo.searchFood("fruit")
        assertTrue(fruitResult.isSuccess)
        val fruits = fruitResult.getOrNull()!!
        assertTrue("Should return multiple fresh fruits", fruits.size >= 10)
        assertTrue(fruits.any { it.product_name?.contains("Banana", ignoreCase = true) == true })
        assertTrue(fruits.any { it.product_name?.contains("Apple", ignoreCase = true) == true })
        assertTrue(fruits.any { it.product_name?.contains("Mango", ignoreCase = true) == true })

        // 2. Test "fruits" query
        val fruitsPluralResult = repo.searchFood("fruits")
        assertTrue(fruitsPluralResult.isSuccess)
        val fruitsPlural = fruitsPluralResult.getOrNull()!!
        assertTrue("Plural fruits should return multiple items", fruitsPlural.isNotEmpty())
        assertTrue(fruitsPlural.any { it.product_name?.contains("Banana", ignoreCase = true) == true })

        // 3. Test Hindi fruit name "kela"
        val kelaResult = repo.searchFood("kela")
        assertTrue(kelaResult.isSuccess)
        val kelaItems = kelaResult.getOrNull()!!
        assertTrue("kela should return banana", kelaItems.any { it.product_name?.contains("Banana", ignoreCase = true) == true })
    }

    private fun createDummyDao() = object : FoodLogDao {
        override suspend fun insertFoodLog(foodLog: FoodLogEntity): Long = 1L
        override suspend fun insertAll(foodLogs: List<FoodLogEntity>) {}
        override fun getAllFoodLogs(userId: String) = flowOf(emptyList<FoodLogEntity>())
        override suspend fun getAllFoodLogsList(userId: String): List<FoodLogEntity> = emptyList()
        override fun getFoodLogsByDateRange(userId: String, startOfDay: Long, endOfDay: Long) = flowOf(emptyList<FoodLogEntity>())
        override fun getFoodLogsSince(userId: String, sinceTimestamp: Long) = flowOf(emptyList<FoodLogEntity>())
        override suspend fun getCount(userId: String): Int = 0
        override suspend fun deleteFoodLog(foodLog: FoodLogEntity) {}
        override suspend fun deleteFoodLogsByDateRange(userId: String, startOfDay: Long, endOfDay: Long) {}
        override suspend fun deleteAllFoodLogsForUser(userId: String) {}
        override suspend fun deleteAllFoodLogs() {}
    }
}
