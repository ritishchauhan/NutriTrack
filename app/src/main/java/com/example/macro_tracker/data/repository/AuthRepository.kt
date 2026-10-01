package com.example.macro_tracker.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.example.macro_tracker.data.local.FoodLogDao
import com.example.macro_tracker.data.local.UserProfileManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Domain representation of an authenticated user.
 */
data class AuthUser(
    val uid: String,
    val email: String = "",
    val phoneNumber: String = "",
    val displayName: String = "",
    val isAnonymous: Boolean = false
)

/**
 * Repository interface defining authentication operations with Firebase.
 * Enforces account-based data ownership, distinct sign-out vs account deletion,
 * and immediate session clearing.
 */
interface AuthRepository {
    val currentUser: StateFlow<AuthUser?>
    val currentUserLiveData: LiveData<AuthUser?>
    val isAuthReady: StateFlow<Boolean>

    suspend fun signIn(email: String, password: String): Result<AuthUser>
    suspend fun signUp(email: String, password: String, displayName: String): Result<AuthUser>
    suspend fun signInWithGoogle(idToken: String): Result<AuthUser>
    fun sendPhoneVerificationCode(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (verificationId: String, resendToken: PhoneAuthProvider.ForceResendingToken) -> Unit,
        onVerificationCompleted: (AuthUser) -> Unit,
        onVerificationFailed: (Exception) -> Unit
    )
    suspend fun verifyPhoneOtp(verificationId: String, smsCode: String): Result<AuthUser>
    suspend fun signInAnonymously(): Result<AuthUser>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun signOut(wipeLocalData: Boolean = false)
    suspend fun deleteAccount(): Result<Unit>
    suspend fun wipeLocalMealData(): Result<Unit>
    suspend fun keepLocalMealData(): Result<Unit>
    suspend fun checkServerConnectionTime(): Result<Long>
}

/**
 * Production implementation of [AuthRepository] integrating Firebase Authentication,
 * Google Sign-In, Phone Verification, Neon Postgres, and Cloud Firestore.
 */
class FirebaseAuthRepositoryImpl(
    private val context: Context,
    private val userProfileManager: UserProfileManager,
    private val firestoreRepository: FirestoreRepository = FirestoreRepositoryImpl(),
    private val foodLogDao: FoodLogDao? = null,
    private val mealCloudSyncRepository: MealCloudSyncRepository = MealCloudSyncRepositoryImpl(
        foodLogDao = foodLogDao ?: com.example.macro_tracker.data.local.AppDatabase.getDatabase(context).foodLogDao(),
        userProfileManager = userProfileManager,
        firestoreRepository = firestoreRepository
    ),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AuthRepository {

    companion object {
        private const val TAG = "FirebaseAuthRepo"
    }

    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val _currentUser = MutableStateFlow<AuthUser?>(
        try {
            FirebaseAuth.getInstance().currentUser?.toAuthUser()
        } catch (ignored: Exception) {
            null
        }
    )
    override val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()
    override val currentUserLiveData: LiveData<AuthUser?> = _currentUser.asLiveData(ioDispatcher)

    private val _isAuthReady = MutableStateFlow(
        try {
            FirebaseAuth.getInstance().currentUser != null
        } catch (ignored: Exception) {
            false
        }
    )
    override val isAuthReady: StateFlow<Boolean> = _isAuthReady.asStateFlow()

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance().also { auth ->
                auth.addAuthStateListener { firebase ->
                    val user = firebase.currentUser
                    val authUser = user?.toAuthUser()
                    _currentUser.value = authUser
                    if (authUser != null) {
                        userProfileManager.setActiveUser(authUser.uid)
                    } else {
                        userProfileManager.clearActiveSession()
                    }
                    _isAuthReady.value = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FirebaseAuth", e)
            _isAuthReady.value = true
            null
        }
    }

    init {
        try {
            val auth = firebaseAuth
            val user = auth?.currentUser
            val authUser = user?.toAuthUser()
            _currentUser.value = authUser
            if (authUser != null) {
                userProfileManager.setActiveUser(authUser.uid)
                repositoryScope.launch {
                    mealCloudSyncRepository.syncAccountData(authUser.uid)
                    syncUserToFirestore(authUser)
                }
            }
            _isAuthReady.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking current user", e)
            _isAuthReady.value = true
        }
    }

    override suspend fun signIn(email: String, password: String): Result<AuthUser> = withContext(ioDispatcher) {
        if (email.isBlank() || password.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Email and password cannot be empty."))
        }
        val auth = firebaseAuth
            ?: return@withContext Result.failure(IllegalStateException("Firebase Authentication is not available. Please verify network connection."))

        try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user
                ?: return@withContext Result.failure(IllegalStateException("No user returned from Firebase."))
            val domainUser = firebaseUser.toAuthUser()
            _currentUser.value = domainUser
            userProfileManager.setActiveUser(domainUser.uid)

            // Sync account-specific data from Neon cloud backend & Firestore
            mealCloudSyncRepository.syncAccountData(domainUser.uid)
            syncUserToFirestore(domainUser)
            Result.success(domainUser)
        } catch (e: Exception) {
            // If login fails, guarantee no previous private data remains active
            userProfileManager.clearActiveSession()
            Result.failure(e)
        }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): Result<AuthUser> = withContext(ioDispatcher) {
        if (email.isBlank() || password.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Email and password cannot be empty."))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        val auth = firebaseAuth
            ?: return@withContext Result.failure(IllegalStateException("Firebase Authentication is not available. Please verify network connection."))

        try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user
                ?: return@withContext Result.failure(IllegalStateException("Failed to create user."))

            val cleanName = displayName.trim()
            if (cleanName.isNotBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
            }

            val domainUser = AuthUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: email.trim(),
                displayName = cleanName.ifBlank { firebaseUser.displayName ?: "" },
                isAnonymous = firebaseUser.isAnonymous
            )
            _currentUser.value = domainUser
            userProfileManager.setActiveUser(domainUser.uid)

            mealCloudSyncRepository.syncAccountData(domainUser.uid)
            syncUserToFirestore(domainUser)
            Result.success(domainUser)
        } catch (e: Exception) {
            userProfileManager.clearActiveSession()
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> = withContext(ioDispatcher) {
        if (idToken.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Google ID Token cannot be empty."))
        }
        val auth = firebaseAuth
            ?: return@withContext Result.failure(IllegalStateException("Firebase Authentication is not available."))

        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
                ?: return@withContext Result.failure(IllegalStateException("Failed to sign in with Google."))
            val domainUser = firebaseUser.toAuthUser()
            _currentUser.value = domainUser
            userProfileManager.setActiveUser(domainUser.uid)

            mealCloudSyncRepository.syncAccountData(domainUser.uid)
            syncUserToFirestore(domainUser)
            Result.success(domainUser)
        } catch (e: Exception) {
            userProfileManager.clearActiveSession()
            Result.failure(e)
        }
    }

    override fun sendPhoneVerificationCode(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (verificationId: String, resendToken: PhoneAuthProvider.ForceResendingToken) -> Unit,
        onVerificationCompleted: (AuthUser) -> Unit,
        onVerificationFailed: (Exception) -> Unit
    ) {
        val auth = firebaseAuth ?: run {
            onVerificationFailed(IllegalStateException("Firebase Authentication is not available."))
            return
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                repositoryScope.launch {
                    try {
                        val authResult = auth.signInWithCredential(credential).await()
                        val firebaseUser = authResult.user
                        if (firebaseUser != null) {
                            val domainUser = firebaseUser.toAuthUser()
                            _currentUser.value = domainUser
                            userProfileManager.setActiveUser(domainUser.uid)
                            mealCloudSyncRepository.syncAccountData(domainUser.uid)
                            syncUserToFirestore(domainUser)
                            onVerificationCompleted(domainUser)
                        }
                    } catch (e: Exception) {
                        userProfileManager.clearActiveSession()
                        onVerificationFailed(e)
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "Phone verification failed", e)
                userProfileManager.clearActiveSession()
                onVerificationFailed(e)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(TAG, "Phone verification code sent. ID: $verificationId")
                onCodeSent(verificationId, token)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber.trim())
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    override suspend fun verifyPhoneOtp(
        verificationId: String,
        smsCode: String
    ): Result<AuthUser> = withContext(ioDispatcher) {
        if (verificationId.isBlank() || smsCode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Verification ID and SMS OTP cannot be empty."))
        }
        val auth = firebaseAuth
            ?: return@withContext Result.failure(IllegalStateException("Firebase Authentication is not available."))

        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, smsCode.trim())
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
                ?: return@withContext Result.failure(IllegalStateException("Phone authentication failed."))
            val domainUser = firebaseUser.toAuthUser()
            _currentUser.value = domainUser
            userProfileManager.setActiveUser(domainUser.uid)

            mealCloudSyncRepository.syncAccountData(domainUser.uid)
            syncUserToFirestore(domainUser)
            Result.success(domainUser)
        } catch (e: Exception) {
            userProfileManager.clearActiveSession()
            Result.failure(e)
        }
    }

    override suspend fun signInAnonymously(): Result<AuthUser> = withContext(ioDispatcher) {
        // Guest mode is permanently disabled per system rules
        Result.failure(UnsupportedOperationException("Guest access is disabled. Please create an account or sign in to track your macros."))
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(ioDispatcher) {
        if (email.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your email address."))
        }
        val auth = firebaseAuth
            ?: return@withContext Result.failure(IllegalStateException("Firebase Authentication is not available."))
        try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign Out: Ends the active session and clears active credentials from memory.
     * Server data on Neon & Firestore is NOT deleted.
     */
    override suspend fun signOut(wipeLocalData: Boolean) = withContext(ioDispatcher) {
        val oldUserId = _currentUser.value?.uid
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Error during signOut", e)
        }
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(context, gso).signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out GoogleSignInClient during signOut", e)
        }
        userProfileManager.clearActiveSession()
        if (wipeLocalData && !oldUserId.isNullOrBlank()) {
            mealCloudSyncRepository.wipeLocalUserData(oldUserId)
        }
        _currentUser.value = null
    }

    /**
     * Account Deletion: Treated as a completely separate operation from sign out.
     * Permanently purges user data from Neon, Firestore, and local storage, then deletes the auth account.
     */
    override suspend fun deleteAccount(): Result<Unit> = withContext(ioDispatcher) {
        val user = firebaseAuth?.currentUser
            ?: return@withContext Result.failure(IllegalStateException("No authenticated user found for account deletion"))
        val uid = user.uid
        try {
            // 1. Purge server and local data
            mealCloudSyncRepository.deleteAccountCloudAndLocalData(uid)
            // 2. Delete Firebase Auth user
            user.delete().await()
            // 3. Clear active session in memory
            userProfileManager.clearActiveSession()
            // 4. Clear Google Sign-In client session
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                GoogleSignIn.getClient(context, gso).signOut()
            } catch (e: Exception) {
                Log.e(TAG, "Error signing out GoogleSignInClient during deleteAccount", e)
            }
            _currentUser.value = null
            Log.d(TAG, "Account deletion operation successful for $uid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete account $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun wipeLocalMealData(): Result<Unit> = withContext(ioDispatcher) {
        val uid = _currentUser.value?.uid ?: return@withContext Result.success(Unit)
        mealCloudSyncRepository.wipeLocalUserData(uid)
    }

    override suspend fun keepLocalMealData(): Result<Unit> = withContext(ioDispatcher) {
        Log.d(TAG, "Local meal data preserved on device")
        Result.success(Unit)
    }

    override suspend fun checkServerConnectionTime(): Result<Long> = withContext(ioDispatcher) {
        mealCloudSyncRepository.checkServerConnectionTime()
    }

    private suspend fun syncUserToFirestore(authUser: AuthUser) {
        if (authUser.isAnonymous || authUser.uid.startsWith("guest_") || authUser.uid == "guest") {
            return
        }
        try {
            val profileData = mutableMapOf<String, Any>(
                "uid" to authUser.uid
            )
            if (authUser.email.isNotBlank()) {
                profileData["email"] = authUser.email
                userProfileManager.saveUserGmail(authUser.email)
            }
            if (authUser.phoneNumber.isNotBlank()) {
                profileData["phoneNumber"] = authUser.phoneNumber
            }
            if (authUser.displayName.isNotBlank()) {
                profileData["displayName"] = authUser.displayName
                userProfileManager.saveUserName(authUser.displayName)
            }

            // Sync with Firestore
            firestoreRepository.saveUserProfile(authUser.uid, profileData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync user with Firestore", e)
        }
    }

    private fun FirebaseUser.toAuthUser(): AuthUser {
        return AuthUser(
            uid = uid,
            email = email ?: "",
            phoneNumber = phoneNumber ?: "",
            displayName = displayName ?: "",
            isAnonymous = isAnonymous
        )
    }
}
