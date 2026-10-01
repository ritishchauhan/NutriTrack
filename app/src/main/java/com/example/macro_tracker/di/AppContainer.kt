package com.example.macro_tracker.di

import android.content.Context
import com.example.macro_tracker.data.local.AppDatabase
import com.example.macro_tracker.data.local.UserProfileManager
import com.example.macro_tracker.data.remote.NutritionApiClient
import com.example.macro_tracker.data.repository.FoodRepository
import com.example.macro_tracker.data.repository.FoodRepositoryImpl
import com.example.macro_tracker.data.repository.UserRepository
import com.example.macro_tracker.data.repository.UserRepositoryImpl
import com.example.macro_tracker.data.repository.AuthRepository
import com.example.macro_tracker.data.repository.FirebaseAuthRepositoryImpl
import com.example.macro_tracker.data.repository.FirestoreRepository
import com.example.macro_tracker.data.repository.FirestoreRepositoryImpl
import com.example.macro_tracker.data.repository.MealCloudSyncRepository
import com.example.macro_tracker.data.repository.MealCloudSyncRepositoryImpl

/**
 * Dependency container interface defining application-level singletons for MVVM architecture.
 */
interface AppContainer {
    val foodRepository: FoodRepository
    val userRepository: UserRepository
    val authRepository: AuthRepository
    val firestoreRepository: FirestoreRepository
    val mealCloudSyncRepository: MealCloudSyncRepository
}

/**
 * Default implementation of [AppContainer] providing production dependencies.
 */
class DefaultAppContainer(private val context: Context) : AppContainer {

    init {
        NutritionApiClient.initCache(context.cacheDir)
    }

    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    private val userProfileManager: UserProfileManager by lazy {
        UserProfileManager(context)
    }

    private val neonApiClient: com.example.macro_tracker.data.remote.neon.NeonApiClient by lazy {
        com.example.macro_tracker.data.remote.neon.NeonApiClient()
    }

    override val firestoreRepository: FirestoreRepository by lazy {
        FirestoreRepositoryImpl()
    }

    override val mealCloudSyncRepository: MealCloudSyncRepository by lazy {
        MealCloudSyncRepositoryImpl(
            foodLogDao = database.foodLogDao(),
            userProfileManager = userProfileManager,
            firestoreRepository = firestoreRepository,
            neonApiClient = neonApiClient
        )
    }

    override val foodRepository: FoodRepository by lazy {
        FoodRepositoryImpl(
            foodLogDao = database.foodLogDao(),
            nutritionApi = NutritionApiClient.api,
            firestoreRepository = firestoreRepository,
            neonApiClient = neonApiClient
        )
    }

    override val userRepository: UserRepository by lazy {
        UserRepositoryImpl(
            userProfileManager = userProfileManager,
            firestoreRepository = firestoreRepository,
            neonApiClient = neonApiClient
        )
    }

    override val authRepository: AuthRepository by lazy {
        FirebaseAuthRepositoryImpl(
            context = context,
            userProfileManager = userProfileManager,
            firestoreRepository = firestoreRepository,
            foodLogDao = database.foodLogDao(),
            mealCloudSyncRepository = mealCloudSyncRepository
        )
    }
}
