package com.example.macro_tracker.ui

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.macro_tracker.data.repository.AuthRepository
import com.example.macro_tracker.data.repository.AuthUser
import com.example.macro_tracker.data.repository.UserRepository
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI State for Authentication operations across Email/Password, Google Sign-in, and Phone Auth.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val user: AuthUser? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isAuthenticated: Boolean = false,
    // Phone Auth states
    val isOtpSent: Boolean = false,
    val phoneVerificationId: String? = null,
    val formattedPhoneNumber: String = ""
)

/**
 * Lifecycle-aware ViewModel for Authentication operations, exposing both LiveData
 * and StateFlow for responsive, lifecycle-aware UI updates.
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // Current User Flow & LiveData
    val currentUser: StateFlow<AuthUser?> = authRepository.currentUser
    val currentUserLiveData: LiveData<AuthUser?> = authRepository.currentUserLiveData
    val isAuthReady: StateFlow<Boolean> = authRepository.isAuthReady

    // UI State Management
    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    // Cloud Server Connection Diagnostics
    private val _serverLatency = MutableStateFlow<Long?>(null)
    val serverLatency: StateFlow<Long?> = _serverLatency.asStateFlow()

    private val _isCheckingServer = MutableStateFlow(false)
    val isCheckingServer: StateFlow<Boolean> = _isCheckingServer.asStateFlow()

    private val _authStateLiveData = MutableLiveData(AuthUiState())
    val authStateLiveData: LiveData<AuthUiState> = _authStateLiveData

    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                updateState {
                    copy(
                        user = user,
                        isAuthenticated = user != null
                    )
                }
            }
        }
    }

    companion object {
        // Strict RFC-compliant email regex: requires valid username, '@', domain, and a 2+ letter top-level domain
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

        // Phone number validation: allows international numbers with '+' or standard 10-15 digit numbers
        private val PHONE_CLEAN_REGEX = "^\\+?[1-9]\\d{7,14}$".toRegex()

        /**
         * Validates whether an email string adheres to standard email formatting rules.
         */
        fun isValidEmail(email: String): Boolean {
            val trimmed = email.trim()
            if (trimmed.isEmpty()) return false
            return EMAIL_REGEX.matches(trimmed)
        }

        /**
         * Validates whether a phone number string contains a valid sequence of digits (8 to 15 digits).
         */
        fun isValidPhoneNumber(phoneNumber: String): Boolean {
            val clean = phoneNumber.replace("[\\s\\-\\(\\)]".toRegex(), "")
            if (clean.length < 8) return false
            return PHONE_CLEAN_REGEX.matches(clean)
        }

        /**
         * Formats phone number into international E.164 format.
         */
        fun formatE164Phone(countryCode: String, number: String): String {
            val cleanNum = number.replace("[\\s\\-\\(\\)]".toRegex(), "").trimStart('0')
            val cleanCode = if (countryCode.startsWith("+")) countryCode else "+$countryCode"
            return if (cleanNum.startsWith("+")) cleanNum else "$cleanCode$cleanNum"
        }
    }

    private fun updateState(transform: AuthUiState.() -> AuthUiState) {
        val newState = _authState.value.transform()
        _authState.value = newState
        _authStateLiveData.postValue(newState)
    }

    /**
     * Sets an immediate UI validation error message without triggering network requests.
     */
    fun setValidationError(message: String) {
        updateState { copy(errorMessage = message, successMessage = null) }
    }

    // ==========================================
    // Email / Password Authentication
    // ==========================================

    fun signIn(email: String, password: String, onNavigateHome: () -> Unit) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            updateState { copy(errorMessage = "Please enter your email address.", successMessage = null) }
            return
        }
        if (!isValidEmail(trimmedEmail)) {
            updateState { copy(errorMessage = "Please enter a valid email address (e.g. name@example.com).", successMessage = null) }
            return
        }
        if (password.isBlank()) {
            updateState { copy(errorMessage = "Please enter your password.", successMessage = null) }
            return
        }

        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = authRepository.signIn(trimmedEmail, password)
            result.onSuccess { user ->
                if (user.displayName.isNotBlank()) {
                    userRepository.saveUserName(user.displayName)
                }
                updateState {
                    copy(
                        isLoading = false,
                        user = user,
                        isAuthenticated = true,
                        successMessage = "Welcome back, ${user.displayName.ifBlank { "User" }}!"
                    )
                }
                onNavigateHome()
            }.onFailure { error ->
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = formatErrorMessage(error)
                    )
                }
            }
        }
    }

    fun signUp(email: String, password: String, displayName: String, onNavigateHome: () -> Unit) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            updateState { copy(errorMessage = "Please enter an email address.", successMessage = null) }
            return
        }
        if (!isValidEmail(trimmedEmail)) {
            updateState { copy(errorMessage = "Please enter a valid email address (e.g. name@example.com).", successMessage = null) }
            return
        }
        if (password.isBlank()) {
            updateState { copy(errorMessage = "Please enter a password.", successMessage = null) }
            return
        }
        if (password.length < 6) {
            updateState { copy(errorMessage = "Password must be at least 6 characters long.", successMessage = null) }
            return
        }

        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = authRepository.signUp(trimmedEmail, password, displayName)
            result.onSuccess { user ->
                val cleanName = displayName.ifBlank { user.displayName }
                if (cleanName.isNotBlank()) {
                    userRepository.saveUserName(cleanName)
                }
                updateState {
                    copy(
                        isLoading = false,
                        user = user,
                        isAuthenticated = true,
                        successMessage = "Account created successfully!"
                    )
                }
                onNavigateHome()
            }.onFailure { error ->
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = formatErrorMessage(error)
                    )
                }
            }
        }
    }

    // ==========================================
    // Google Sign-In
    // ==========================================

    fun signInWithGoogle(idToken: String, onNavigateHome: () -> Unit) {
        if (idToken.isBlank()) {
            updateState { copy(errorMessage = "Google authentication failed: ID Token was empty.") }
            return
        }
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = authRepository.signInWithGoogle(idToken)
            result.onSuccess { user ->
                if (user.displayName.isNotBlank()) {
                    userRepository.saveUserName(user.displayName)
                }
                updateState {
                    copy(
                        isLoading = false,
                        user = user,
                        isAuthenticated = true,
                        successMessage = "Signed in with Google as ${user.displayName.ifBlank { user.email }}."
                    )
                }
                onNavigateHome()
            }.onFailure { error ->
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = formatErrorMessage(error)
                    )
                }
            }
        }
    }

    // ==========================================
    // Phone Number Authentication (OTP)
    // ==========================================

    fun sendPhoneOtp(
        activity: Activity,
        countryCode: String,
        phoneNumber: String
    ) {
        val fullPhone = formatE164Phone(countryCode, phoneNumber)
        if (!isValidPhoneNumber(fullPhone)) {
            updateState {
                copy(
                    errorMessage = "Please enter a valid phone number (minimum 8-10 digits).",
                    successMessage = null
                )
            }
            return
        }

        updateState { copy(isLoading = true, errorMessage = null, successMessage = null) }

        authRepository.sendPhoneVerificationCode(
            activity = activity,
            phoneNumber = fullPhone,
            onCodeSent = { verificationId, token ->
                resendToken = token
                updateState {
                    copy(
                        isLoading = false,
                        isOtpSent = true,
                        phoneVerificationId = verificationId,
                        formattedPhoneNumber = fullPhone,
                        successMessage = "6-digit OTP code sent to $fullPhone"
                    )
                }
            },
            onVerificationCompleted = { user ->
                updateState {
                    copy(
                        isLoading = false,
                        user = user,
                        isAuthenticated = true,
                        isOtpSent = false,
                        successMessage = "Phone verified automatically!"
                    )
                }
            },
            onVerificationFailed = { error ->
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = formatErrorMessage(error)
                    )
                }
            }
        )
    }

    fun verifyPhoneOtp(
        smsCode: String,
        onNavigateHome: () -> Unit
    ) {
        val cleanOtp = smsCode.trim()
        val verificationId = _authState.value.phoneVerificationId

        if (cleanOtp.length < 6) {
            updateState { copy(errorMessage = "Please enter the full 6-digit verification code.") }
            return
        }
        if (verificationId.isNullOrBlank()) {
            updateState { copy(errorMessage = "Verification session not found. Please request a new code.") }
            return
        }

        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = authRepository.verifyPhoneOtp(verificationId, cleanOtp)
            result.onSuccess { user ->
                updateState {
                    copy(
                        isLoading = false,
                        user = user,
                        isAuthenticated = true,
                        isOtpSent = false,
                        phoneVerificationId = null,
                        successMessage = "Phone verified successfully!"
                    )
                }
                onNavigateHome()
            }.onFailure { error ->
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = formatErrorMessage(error)
                    )
                }
            }
        }
    }

    fun resetPhoneAuth() {
        updateState {
            copy(
                isOtpSent = false,
                phoneVerificationId = null,
                formattedPhoneNumber = "",
                errorMessage = null
            )
        }
    }

    // ==========================================
    // Password Reset
    // ==========================================

    fun sendPasswordReset(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            updateState { copy(errorMessage = "Please enter your email address to reset password.", successMessage = null) }
            return
        }
        if (!isValidEmail(trimmedEmail)) {
            updateState { copy(errorMessage = "Please enter a valid email address (e.g. name@example.com).", successMessage = null) }
            return
        }
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = authRepository.sendPasswordResetEmail(trimmedEmail)
            result.onSuccess {
                updateState {
                    copy(
                        isLoading = false,
                        successMessage = "Password reset email sent! Check your inbox."
                    )
                }
            }.onFailure { error ->
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = formatErrorMessage(error)
                    )
                }
            }
        }
    }

    fun signOut(wipeLocalData: Boolean = false, onSignedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut(wipeLocalData)
            updateState {
                copy(
                    user = null,
                    isAuthenticated = false,
                    isOtpSent = false,
                    phoneVerificationId = null,
                    successMessage = if (wipeLocalData) "Signed out and local data wiped." else "Signed out successfully."
                )
            }
            onSignedOut()
        }
    }

    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            val res = authRepository.deleteAccount()
            res.onSuccess {
                updateState {
                    copy(
                        isLoading = false,
                        user = null,
                        isAuthenticated = false,
                        isOtpSent = false,
                        phoneVerificationId = null,
                        successMessage = "Account and all associated data permanently deleted."
                    )
                }
                onComplete()
            }.onFailure { error ->
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = formatErrorMessage(error)
                    )
                }
            }
        }
    }

    fun wipeLocalMealData(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            authRepository.wipeLocalMealData()
            onComplete?.invoke()
        }
    }

    fun keepLocalMealData(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            authRepository.keepLocalMealData()
            onComplete?.invoke()
        }
    }

    fun checkServerConnectionTime(onResult: ((Long?) -> Unit)? = null) {
        viewModelScope.launch {
            _isCheckingServer.value = true
            val result = authRepository.checkServerConnectionTime()
            val latency = result.getOrNull()
            _serverLatency.value = latency
            _isCheckingServer.value = false
            onResult?.invoke(latency)
        }
    }

    fun clearMessages() {
        updateState { copy(errorMessage = null, successMessage = null) }
    }

    private fun formatErrorMessage(throwable: Throwable): String {
        val msg = throwable.message ?: "Authentication failed."
        return when {
            msg.contains("badly formatted", ignoreCase = true) ||
            msg.contains("invalid-email", ignoreCase = true) ||
            msg.contains("ERROR_INVALID_EMAIL", ignoreCase = true) ->
                "Invalid email format. Please enter a valid email address (e.g. name@example.com)."

            msg.contains("email-already-in-use", ignoreCase = true) ||
            msg.contains("ERROR_EMAIL_ALREADY_IN_USE", ignoreCase = true) ->
                "An account already exists with this email address. Please sign in instead."

            msg.contains("user-not-found", ignoreCase = true) ||
            msg.contains("ERROR_USER_NOT_FOUND", ignoreCase = true) ||
            msg.contains("no user record", ignoreCase = true) ->
                "No account found with this email. Please switch to Sign Up to create one."

            msg.contains("wrong-password", ignoreCase = true) ||
            msg.contains("ERROR_WRONG_PASSWORD", ignoreCase = true) ||
            msg.contains("invalid-credential", ignoreCase = true) ->
                "Incorrect email or password. Please verify your credentials and try again."

            msg.contains("weak-password", ignoreCase = true) ||
            msg.contains("ERROR_WEAK_PASSWORD", ignoreCase = true) ->
                "Password is too weak. Please use at least 6 characters."

            msg.contains("operation-not-allowed", ignoreCase = true) ||
            msg.contains("ERROR_OPERATION_NOT_ALLOWED", ignoreCase = true) ||
            msg.contains("configuration not found", ignoreCase = true) ||
            msg.contains("configuration-not-found", ignoreCase = true) ||
            msg.contains("ERROR_CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                "Sign-in provider is not enabled in Firebase Console (nutritrack-e5534). Go to Firebase Console > Authentication > Sign-in method, and enable Phone and Google."

            msg.contains("invalid-verification-code", ignoreCase = true) ||
            msg.contains("ERROR_INVALID_VERIFICATION_CODE", ignoreCase = true) ->
                "Invalid SMS verification code. Please check your SMS and try again."

            msg.contains("session-expired", ignoreCase = true) ||
            msg.contains("ERROR_SESSION_EXPIRED", ignoreCase = true) ->
                "Verification code has expired. Please request a new code."

            msg.contains("too-many-requests", ignoreCase = true) ||
            msg.contains("ERROR_TOO_MANY_REQUESTS", ignoreCase = true) ->
                "Too many attempts. Requests from this device are temporarily blocked. Please try again later."

            msg.contains("user-disabled", ignoreCase = true) ||
            msg.contains("ERROR_USER_DISABLED", ignoreCase = true) ->
                "This user account has been disabled. Please contact support."

            msg.contains("10:", ignoreCase = true) ||
            msg.contains("DEVELOPER_ERROR", ignoreCase = true) ->
                "Google Sign-In configuration error (Code 10). Ensure your SHA-1 fingerprint and OAuth Web Client ID are configured in Firebase Console."

            msg.contains("12500", ignoreCase = true) ->
                "Google Sign-In was cancelled or configuration mismatch occurred."

            msg.contains("network", ignoreCase = true) ||
            msg.contains("timeout", ignoreCase = true) ->
                "Network connection error. Please check your internet connection."

            else -> msg
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(authRepository, userRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
