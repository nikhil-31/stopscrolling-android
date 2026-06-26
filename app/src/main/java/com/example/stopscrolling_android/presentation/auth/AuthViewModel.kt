package com.example.stopscrolling_android.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stopscrolling_android.data.remote.dto.AuthenticatedUser
import com.example.stopscrolling_android.data.remote.dto.DeviceStatusRow
import com.example.stopscrolling_android.data.remote.dto.MfaPendingResponse
import com.example.stopscrolling_android.data.settings.BackendSettingsStore
import com.example.stopscrolling_android.data.sync.BackendSyncService
import com.example.stopscrolling_android.data.sync.DeviceStatusFetchResult
import com.example.stopscrolling_android.domain.repository.AuthRepository
import com.example.stopscrolling_android.startup.AppStartupRunner
import com.example.stopscrolling_android.worker.HeartbeatScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthFormMode { SIGN_IN, SIGN_UP }

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val backendSyncService: BackendSyncService,
    private val backendSettingsStore: BackendSettingsStore,
    private val appStartupRunner: AppStartupRunner,
    private val heartbeatScheduler: HeartbeatScheduler
) : ViewModel() {

    val currentUser: StateFlow<AuthenticatedUser?> = authRepository.currentUser
    val mfaChallenge: StateFlow<MfaPendingResponse?> = authRepository.mfaChallenge

    private val _formMode = MutableStateFlow(AuthFormMode.SIGN_IN)
    val formMode: StateFlow<AuthFormMode> = _formMode.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _mfaCode = MutableStateFlow("")
    val mfaCode: StateFlow<String> = _mfaCode.asStateFlow()

    private val _backupCode = MutableStateFlow("")
    val backupCode: StateFlow<String> = _backupCode.asStateFlow()

    private val _useBackupCode = MutableStateFlow(false)
    val useBackupCode: StateFlow<Boolean> = _useBackupCode.asStateFlow()

    private val _statusMessage = MutableStateFlow(
        "Sign in or create an account to sync screen time with the backend."
    )
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _devices = MutableStateFlow<List<DeviceStatusRow>>(emptyList())
    val devices: StateFlow<List<DeviceStatusRow>> = _devices.asStateFlow()

    private val _isLoadingDevices = MutableStateFlow(false)
    val isLoadingDevices: StateFlow<Boolean> = _isLoadingDevices.asStateFlow()

    private val _devicesError = MutableStateFlow<String?>(null)
    val devicesError: StateFlow<String?> = _devicesError.asStateFlow()

    val registeredDeviceId: StateFlow<String?> = backendSettingsStore.settings
        .map { it.registeredDeviceId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            authRepository.restoreSession().onFailure {
                _statusMessage.value = "Session expired. Sign in again."
            }.onSuccess {
                currentUser.value?.let { user ->
                    _statusMessage.value = "Signed in as ${user.email}"
                    refreshDevices()
                }
            }
        }
    }

    fun setFormMode(mode: AuthFormMode) {
        _formMode.value = mode
    }

    fun setEmail(value: String) { _email.value = value }
    fun setPassword(value: String) { _password.value = value }
    fun setConfirmPassword(value: String) { _confirmPassword.value = value }
    fun setPhoneNumber(value: String) { _phoneNumber.value = value }
    fun setMfaCode(value: String) { _mfaCode.value = value }
    fun setBackupCode(value: String) { _backupCode.value = value }
    fun setUseBackupCode(value: Boolean) { _useBackupCode.value = value }

    fun canSubmitSignIn(): Boolean {
        return !_isLoading.value &&
            AuthFormValidation.signInError(_email.value, _password.value) == null
    }

    fun canSubmitSignUp(): Boolean {
        return !_isLoading.value &&
            AuthFormValidation.signUpError(
                _email.value,
                _password.value,
                _confirmPassword.value,
                _phoneNumber.value
            ) == null
    }

    fun register() {
        val error = AuthFormValidation.signUpError(
            _email.value,
            _password.value,
            _confirmPassword.value,
            _phoneNumber.value
        )
        if (error != null) {
            _statusMessage.value = error
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val phone = _phoneNumber.value.trim().ifEmpty { null }
            authRepository.register(
                email = AuthFormValidation.normalizedEmail(_email.value),
                password = _password.value,
                phoneNumber = phone
            )
                .onSuccess {
                    mfaChallenge.value?.let { challenge ->
                        _statusMessage.value = challenge.message
                    } ?: run {
                        _statusMessage.value = "Account created. Complete sign-in verification."
                    }
                }
                .onFailure {
                    _statusMessage.value = it.message ?: "Registration failed."
                }
            _isLoading.value = false
        }
    }

    fun login() {
        val error = AuthFormValidation.signInError(_email.value, _password.value)
        if (error != null) {
            _statusMessage.value = error
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            authRepository.login(
                email = AuthFormValidation.normalizedEmail(_email.value),
                password = _password.value
            )
                .onSuccess { challenge ->
                    _statusMessage.value = challenge.message
                }
                .onFailure {
                    _statusMessage.value = it.message ?: "Sign in failed."
                }
            _isLoading.value = false
        }
    }

    fun verifyMfa() {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.verifyMfa(
                code = _mfaCode.value.trim(),
                backupCode = _backupCode.value.trim(),
                useBackupCode = _useBackupCode.value
            )
                .onSuccess {
                    clearSensitiveFields()
                    currentUser.value?.let { user ->
                        _statusMessage.value = "Signed in as ${user.email}"
                        registerDeviceAndSync()
                    }
                }
                .onFailure {
                    _statusMessage.value = it.message ?: "Verification failed."
                }
            _isLoading.value = false
        }
    }

    fun resendMfa() {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.resendMfa()
                .onSuccess { detail -> _statusMessage.value = detail }
                .onFailure { _statusMessage.value = it.message ?: "Could not resend code." }
            _isLoading.value = false
        }
    }

    fun cancelMfa() {
        authRepository.cancelMfa()
        _mfaCode.value = ""
        _backupCode.value = ""
        _useBackupCode.value = false
        _statusMessage.value = if (_formMode.value == AuthFormMode.SIGN_UP) {
            "Account created. Sign in when you are ready."
        } else {
            "Sign in cancelled."
        }
    }

    fun logout() {
        authRepository.logout()
        clearSensitiveFields()
        _devices.value = emptyList()
        _devicesError.value = null
        viewModelScope.launch {
            backendSettingsStore.clearRegisteredDeviceId()
            heartbeatScheduler.cancel()
        }
        _statusMessage.value = "Signed out."
    }

    fun registerDeviceOnResume() {
        viewModelScope.launch {
            appStartupRunner.registerDeviceIfSignedIn()
            if (currentUser.value != null) {
                refreshDevices()
            }
        }
    }

    fun refreshDevices() {
        if (currentUser.value == null) {
            _devices.value = emptyList()
            _devicesError.value = null
            return
        }

        viewModelScope.launch {
            _isLoadingDevices.value = true
            when (val result = backendSyncService.fetchDeviceStatus()) {
                is DeviceStatusFetchResult.Success -> {
                    _devices.value = result.response.results
                    _devicesError.value = null
                }
                is DeviceStatusFetchResult.Skipped -> {
                    _devices.value = emptyList()
                    _devicesError.value = null
                }
                is DeviceStatusFetchResult.Failure -> {
                    _devicesError.value = result.message
                }
            }
            _isLoadingDevices.value = false
        }
    }

    private fun registerDeviceAndSync() {
        viewModelScope.launch {
            appStartupRunner.registerDeviceIfSignedIn()
            backendSyncService.syncUnsyncedRecords()
            refreshDevices()
        }
    }

    private fun clearSensitiveFields() {
        _password.value = ""
        _confirmPassword.value = ""
        _mfaCode.value = ""
        _backupCode.value = ""
        _useBackupCode.value = false
    }
}
