package com.example.macro_tracker.data.remote

import androidx.compose.runtime.Immutable
import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NutritionApi {
    // OpenFoodFacts search query optimized with fields filter and page size for ultra-fast, lightweight responses
    @GET("cgi/search.pl?search_simple=1&action=process&json=1&page_size=25&fields=product_name,brands,nutriments,image_url,serving_size,code")
    suspend fun searchFood(
        @Query("search_terms") query: String
    ): SearchResponse

    // OpenFoodFacts product by barcode lookup
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): BarcodeLookupResponse
}

@Immutable
data class SearchResponse(
    val products: List<Product>? = null
)

@Immutable
data class BarcodeLookupResponse(
    val status: Int? = null,
    @field:Json(name = "status_verbose") val status_verbose: String? = null,
    val code: String? = null,
    val product: Product? = null
)

@Immutable
data class Product(
    @field:Json(name = "product_name") val product_name: String? = null,
    val brands: String? = null,
    val nutriments: Nutriments? = null,
    @field:Json(name = "image_url") val image_url: String? = null,
    @field:Json(name = "serving_size") val serving_size: String? = null,
    val code: String? = null
)

@Immutable
data class Nutriments(
    @field:Json(name = "energy-kcal") val energy_kcal: Double? = null,
    @field:Json(name = "energy-kcal_100g") val energy_kcal_100g: Double? = null,
    @field:Json(name = "energy-kcal_serving") val energy_kcal_serving: Double? = null,
    @field:Json(name = "energy-kcal_value") val energy_kcal_value: Double? = null,
    @field:Json(name = "energy-kcal_unit") val energy_kcal_unit: String? = null,
    val energy: Double? = null,
    @field:Json(name = "energy_100g") val energy_100g: Double? = null,
    @field:Json(name = "energy_serving") val energy_serving: Double? = null,
    @field:Json(name = "energy_value") val energy_value: Double? = null,
    @field:Json(name = "energy_unit") val energy_unit: String? = null,
    val proteins: Double? = null,
    @field:Json(name = "proteins_100g") val proteins_100g: Double? = null,
    @field:Json(name = "proteins_serving") val proteins_serving: Double? = null,
    @field:Json(name = "proteins_value") val proteins_value: Double? = null,
    val carbohydrates: Double? = null,
    @field:Json(name = "carbohydrates_100g") val carbohydrates_100g: Double? = null,
    @field:Json(name = "carbohydrates_serving") val carbohydrates_serving: Double? = null,
    @field:Json(name = "carbohydrates_value") val carbohydrates_value: Double? = null,
    val fat: Double? = null,
    @field:Json(name = "fat_100g") val fat_100g: Double? = null,
    @field:Json(name = "fat_serving") val fat_serving: Double? = null,
    @field:Json(name = "fat_value") val fat_value: Double? = null,
    val fiber: Double? = null,
    @field:Json(name = "fiber_100g") val fiber_100g: Double? = null,
    @field:Json(name = "fiber_serving") val fiber_serving: Double? = null,
    @field:Json(name = "fiber_value") val fiber_value: Double? = null
) {
    /**
     * Resolves accurate calorie count by checking:
     * 1. Direct kcal fields (energy-kcal_100g, energy-kcal, energy-kcal_serving, energy-kcal_value).
     * 2. kJ energy fields (energy_100g, energy, energy_serving, energy_value) converted via 1 kcal = 4.184 kJ.
     * 3. Atwater General Factor System fallback: (Protein * 4) + (Carbs * 4) + (Fat * 9) + (Fiber * 2).
     */
    val calories: Int
        get() {
            // 1. Explicit kcal
            val explicitKcal = energy_kcal_100g ?: energy_kcal ?: energy_kcal_serving ?: energy_kcal_value
            if (explicitKcal != null && explicitKcal > 0.0) {
                return kotlin.math.round(explicitKcal).toInt()
            }

            // 2. Raw energy (frequently returned in kJ by OpenFoodFacts)
            val rawEnergy = energy_100g ?: energy ?: energy_serving ?: energy_value
            if (rawEnergy != null && rawEnergy > 0.0) {
                val isExplicitKcal = energy_unit?.equals("kcal", ignoreCase = true) == true ||
                        energy_kcal_unit?.equals("kcal", ignoreCase = true) == true
                val kcalVal = if (isExplicitKcal) rawEnergy else (rawEnergy / 4.184)
                if (kcalVal > 0.0) {
                    return kotlin.math.round(kcalVal).toInt()
                }
            }

            // 3. Atwater General Factor fallback: ensures calories are never 0 when macros exist
            val pro = proteinGrams
            val carb = carbsGrams
            val f = fatGrams
            val fib = fiberGrams
            if (pro > 0f || carb > 0f || f > 0f) {
                val atwater = kotlin.math.round(pro * 4f + carb * 4f + f * 9f + fib * 2f).toInt()
                if (atwater > 0) return atwater
            }

            return 0
        }

    val proteinGrams: Float
        get() = (proteins_100g ?: proteins ?: proteins_serving ?: proteins_value ?: 0.0).toFloat()

    val carbsGrams: Float
        get() = (carbohydrates_100g ?: carbohydrates ?: carbohydrates_serving ?: carbohydrates_value ?: 0.0).toFloat()

    val fatGrams: Float
        get() = (fat_100g ?: fat ?: fat_serving ?: fat_value ?: 0.0).toFloat()

    val fiberGrams: Float
        get() = (fiber_100g ?: fiber ?: fiber_serving ?: fiber_value ?: 0.0).toFloat()
}

