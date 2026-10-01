package com.example.macro_tracker.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.macro_tracker.data.local.FoodLogEntity
import com.example.macro_tracker.data.remote.Product
import com.example.macro_tracker.data.repository.FoodRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel for food tracking, adhering strictly to MVVM architecture by communicating
 * exclusively with [FoodRepository], lifecycle-aware via [LiveData] and [StateFlow],
 * and optimized with WhileSubscribed(5_000) for minimal memory usage.
 */
@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class FoodViewModel(
    private val foodRepository: FoodRepository
) : ViewModel() {

    private var searchJob: kotlinx.coroutines.Job? = null
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Product>>(emptyList())
    val searchResults: StateFlow<List<Product>> = _searchResults.asStateFlow()
    val searchResultsLiveData: LiveData<List<Product>> = searchResults.asLiveData(viewModelScope.coroutineContext)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val isLoadingLiveData: LiveData<Boolean> = isLoading.asLiveData(viewModelScope.coroutineContext)

    private val _searchErrorMessage = MutableStateFlow<String?>(null)
    val searchErrorMessage: StateFlow<String?> = _searchErrorMessage.asStateFlow()

    private val _isBarcodeLoading = MutableStateFlow(false)
    val isBarcodeLoading: StateFlow<Boolean> = _isBarcodeLoading.asStateFlow()

    private val _scannedProduct = MutableStateFlow<Product?>(null)
    val scannedProduct: StateFlow<Product?> = _scannedProduct.asStateFlow()

    private val _barcodeErrorMessage = MutableStateFlow<String?>(null)
    val barcodeErrorMessage: StateFlow<String?> = _barcodeErrorMessage.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    val selectedDateLiveData: LiveData<LocalDate> = selectedDate.asLiveData(viewModelScope.coroutineContext)

    private val _mealFilter = MutableStateFlow("All")
    val mealFilter: StateFlow<String> = _mealFilter.asStateFlow()

    val dailyFoodLogs: StateFlow<List<FoodLogEntity>> = _selectedDate.flatMapLatest { date ->
        foodRepository.getFoodLogsByDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val dailyFoodLogsLiveData: LiveData<List<FoodLogEntity>> = dailyFoodLogs.asLiveData(viewModelScope.coroutineContext)

    val filteredFoodLogs: StateFlow<List<FoodLogEntity>> = combine(
        dailyFoodLogs,
        _mealFilter
    ) { logs, filter ->
        if (filter == "All") logs
        else logs.filter { it.mealType.equals(filter, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val filteredFoodLogsLiveData: LiveData<List<FoodLogEntity>> = filteredFoodLogs.asLiveData(viewModelScope.coroutineContext)

    // Weekly logs for Insights
    val weeklyFoodLogs: StateFlow<List<FoodLogEntity>> = foodRepository.getFoodLogsSince(7)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val weeklyFoodLogsLiveData: LiveData<List<FoodLogEntity>> = weeklyFoodLogs.asLiveData(viewModelScope.coroutineContext)

    private val _insightsRangeDays = MutableStateFlow(7)
    val insightsRangeDays: StateFlow<Int> = _insightsRangeDays.asStateFlow()

    fun setInsightsRangeDays(days: Int) {
        _insightsRangeDays.value = days
    }

    val insightsFoodLogs: StateFlow<List<FoodLogEntity>> = _insightsRangeDays.flatMapLatest { days ->
        foodRepository.getFoodLogsSince(days)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val insightsFoodLogsLiveData: LiveData<List<FoodLogEntity>> = insightsFoodLogs.asLiveData(viewModelScope.coroutineContext)

    init {
        viewModelScope.launch {
            foodRepository.currentUserIdFlow.collect { uid ->
                if (!uid.isNullOrBlank()) {
                    foodRepository.syncRemoteMeals()
                } else {
                    _searchResults.value = emptyList()
                    _scannedProduct.value = null
                    _searchErrorMessage.value = null
                    _barcodeErrorMessage.value = null
                }
            }
        }

        viewModelScope.launch {
            _searchQuery
                .debounce(280L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    executeSearch(query)
                }
        }
    }

    fun setDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun resetToToday() {
        _selectedDate.value = LocalDate.now()
    }

    fun setMealFilter(filter: String) {
        _mealFilter.value = filter
    }

    fun previousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun nextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun searchFood(query: String, immediate: Boolean = false) {
        _searchQuery.value = query
        if (immediate || query.isBlank()) {
            executeSearch(query)
        }
    }

    private fun executeSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            searchJob?.cancel()
            _searchResults.value = emptyList()
            _searchErrorMessage.value = null
            _isLoading.value = false
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _searchErrorMessage.value = null
            foodRepository.searchFood(trimmed)
                .onSuccess { products ->
                    _searchResults.value = products
                    if (products.isEmpty()) {
                        _searchErrorMessage.value = "No food items found for \"$trimmed\"."
                    }
                }
                .onFailure { error ->
                    _searchResults.value = emptyList()
                    _searchErrorMessage.value = error.message ?: "Failed to fetch food items"
                }
            _isLoading.value = false
        }
    }

    fun saveFood(product: Product, mealType: String = "Breakfast") {
        viewModelScope.launch {
            foodRepository.saveFoodFromProduct(product, mealType, _selectedDate.value)
        }
    }

    private var barcodeLookupJob: kotlinx.coroutines.Job? = null

    fun lookupBarcode(barcode: String, onComplete: ((Product?, String?) -> Unit)? = null) {
        val trimmed = barcode.trim()
        if (trimmed.isBlank()) return

        barcodeLookupJob?.cancel()
        barcodeLookupJob = viewModelScope.launch {
            _isBarcodeLoading.value = true
            _barcodeErrorMessage.value = null
            try {
                kotlinx.coroutines.withTimeout(9000L) {
                    foodRepository.fetchProductByBarcode(trimmed)
                }.onSuccess { product ->
                    _scannedProduct.value = product
                    _isBarcodeLoading.value = false
                    onComplete?.invoke(product, null)
                }.onFailure { error ->
                    _scannedProduct.value = null
                    val msg = error.message ?: "Could not find food details for barcode $trimmed"
                    _barcodeErrorMessage.value = msg
                    _isBarcodeLoading.value = false
                    onComplete?.invoke(null, msg)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _scannedProduct.value = null
                val msg = "Barcode lookup timed out. Please check network connection."
                _barcodeErrorMessage.value = msg
                _isBarcodeLoading.value = false
                onComplete?.invoke(null, msg)
            } catch (e: Exception) {
                _scannedProduct.value = null
                val msg = e.localizedMessage ?: "Failed to lookup barcode"
                _barcodeErrorMessage.value = msg
                _isBarcodeLoading.value = false
                onComplete?.invoke(null, msg)
            }
        }
    }

    fun clearScannedProduct() {
        barcodeLookupJob?.cancel()
        _scannedProduct.value = null
        _barcodeErrorMessage.value = null
        _isBarcodeLoading.value = false
    }

    fun addScannedProductToMeals(
        product: Product,
        mealType: String,
        servingMultiplier: Float = 1.0f,
        customName: String? = null
    ) {
        viewModelScope.launch {
            val mult = servingMultiplier.coerceAtLeast(0.1f)
            val baseCal = product.nutriments?.calories ?: 0
            val basePro = product.nutriments?.proteinGrams ?: 0f
            val baseCarbs = product.nutriments?.carbsGrams ?: 0f
            val baseFat = product.nutriments?.fatGrams ?: 0f
            val baseFiber = product.nutriments?.fiberGrams ?: 0f

            val finalCal = (baseCal * mult).toInt()
            val finalPro = basePro * mult
            val finalCarbs = baseCarbs * mult
            val finalFat = baseFat * mult
            val finalFiber = baseFiber * mult

            val name = customName?.ifBlank { null }
                ?: product.product_name?.ifBlank { "Scanned Food" }
                ?: "Scanned Food"

            val brand = if (!product.brands.isNullOrBlank()) " • ${product.brands}" else ""
            val servingDesc = if (mult != 1.0f) " • ${(mult * 100).toInt()}% serving" else ""
            val details = "P ${finalPro.toInt()}g • C ${finalCarbs.toInt()}g • F ${finalFat.toInt()}g$brand$servingDesc"

            foodRepository.quickAddMeal(
                mealType = mealType,
                foodName = name,
                calories = finalCal,
                protein = finalPro,
                carbs = finalCarbs,
                fat = finalFat,
                fiber = finalFiber,
                details = details,
                date = _selectedDate.value
            )
            clearScannedProduct()
        }
    }

    fun quickAddMeal(
        mealType: String,
        foodName: String = mealType,
        calories: Int = 450,
        protein: Float = 25f,
        carbs: Float = 50f,
        fat: Float = 15f,
        fiber: Float = 0f,
        details: String = "Balanced meal"
    ) {
        viewModelScope.launch {
            foodRepository.quickAddMeal(
                mealType = mealType,
                foodName = foodName,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                fiber = fiber,
                details = details,
                date = _selectedDate.value
            )
        }
    }

    fun updateMealServings(foodLog: FoodLogEntity, newServings: Int) {
        viewModelScope.launch {
            if (newServings <= 0) {
                foodRepository.deleteFoodLog(foodLog)
            } else {
                foodRepository.updateMealServings(foodLog, newServings)
            }
        }
    }

    fun deleteFoodLog(foodLog: FoodLogEntity) {
        viewModelScope.launch {
            foodRepository.deleteFoodLog(foodLog)
        }
    }

    fun deleteFoodLogsForDate(date: LocalDate = _selectedDate.value) {
        viewModelScope.launch {
            foodRepository.deleteFoodLogsForDate(date)
        }
    }

    fun clearAllFoodLogs() {
        viewModelScope.launch {
            foodRepository.clearAllFoodLogs()
        }
    }

    /**
     * Refreshes the dashboard and meal tracking data from the cloud for the active account.
     */
    fun refreshData() {
        viewModelScope.launch {
            foodRepository.syncRemoteMeals()
        }
    }

    /**
     * Clears all in-memory food states on account sign-out.
     */
    fun clearSessionData() {
        _searchResults.value = emptyList()
        _scannedProduct.value = null
        _searchErrorMessage.value = null
        _barcodeErrorMessage.value = null
        _selectedDate.value = LocalDate.now()
        foodRepository.clearActiveSession()
    }

    class Factory(
        private val foodRepository: FoodRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FoodViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FoodViewModel(foodRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
