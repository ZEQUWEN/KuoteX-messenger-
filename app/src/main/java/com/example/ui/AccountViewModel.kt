package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Mocking Supabase and FCM for compilation without adding heavy dependencies
object SupabaseMock {
    object Auth {
        suspend fun updateUserPhone(phone: String): Result<Unit> {
            delay(1000)
            if (phone.length < 10) return Result.failure(Exception("Неверный формат номера"))
            return Result.success(Unit)
        }
        
        suspend fun updateUserEmail(email: String): Result<Unit> {
            delay(1000)
            if (!email.contains("@")) return Result.failure(Exception("Неверный формат email"))
            return Result.success(Unit)
        }

        suspend fun verifyOtp(otp: String): Result<Unit> {
            delay(1000)
            if (otp == "000000") return Result.failure(Exception("Неверный код"))
            return Result.success(Unit)
        }
        
        suspend fun deleteAccount(): Result<Unit> {
            delay(1500)
            return Result.success(Unit)
        }
    }

    object Database {
        suspend fun updateProfile(firstName: String, lastName: String, bio: String, birthDate: String, username: String): Result<Unit> {
            delay(1000)
            return Result.success(Unit)
        }
        
        suspend fun checkUsername(username: String): Boolean {
            delay(500)
            return username != "admin" && username != "telegram"
        }
        
        suspend fun updateAvatar(url: String): Result<Unit> {
            delay(1000)
            return Result.success(Unit)
        }
        
        suspend fun updateSocialLinks(links: Map<String, String>): Result<Unit> {
            delay(1000)
            return Result.success(Unit)
        }
    }
}

data class AccountState(
    val firstName: String = "SARATOSHI",
    val lastName: String = "NARIMOTO",
    val bio: String = "✨Занимаюсь дизайном карточек товаров и вайбкодингом, это моё хобби✨",
    val phone: String = "+7 (922) 669-26-82",
    val username: String = "CreepsyDear",
    val birthDate: String = "21 июн. 2005",
    val avatarUrl: String? = null,
    val socialLinks: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = true,
    val error: String? = null,
    val isSuccess: Boolean = false
)

enum class OtpType { SMS, EMAIL }

class AccountViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AccountState())
    val uiState: StateFlow<AccountState> = _uiState.asStateFlow()
    
    private val _usernameAvailable = MutableStateFlow<Boolean?>(null)
    val usernameAvailable: StateFlow<Boolean?> = _usernameAvailable.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isInitialLoading = true)
            delay(1500) // Mocking network load from Supabase
            _uiState.value = _uiState.value.copy(
                isInitialLoading = false,
                socialLinks = mapOf(
                    "telegram" to "https://t.me/CreepsyDear",
                    "github" to "https://github.com/CreepsyDear"
                )
            )
        }
    }

    fun updateProfile(firstName: String, lastName: String, bio: String) {
        _uiState.value = _uiState.value.copy(firstName = firstName, lastName = lastName, bio = bio)
        saveProfile()
    }

    fun updateBirthDate(date: String) {
        _uiState.value = _uiState.value.copy(birthDate = date)
        saveProfile()
    }
    
    fun updateAvatar(url: String) {
        _uiState.value = _uiState.value.copy(avatarUrl = url)
        viewModelScope.launch {
            SupabaseMock.Database.updateAvatar(url)
        }
    }
    
    fun updateSocialLinks(links: Map<String, String>) {
        _uiState.value = _uiState.value.copy(socialLinks = links)
        viewModelScope.launch {
            SupabaseMock.Database.updateSocialLinks(links)
        }
    }

    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = SupabaseMock.Auth.deleteAccount()
            _uiState.value = _uiState.value.copy(isLoading = false)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError("Ошибка удаления аккаунта")
            }
        }
    }
    
    private fun saveProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val currentState = _uiState.value
            val result = SupabaseMock.Database.updateProfile(
                firstName = currentState.firstName,
                lastName = currentState.lastName,
                bio = currentState.bio,
                birthDate = currentState.birthDate,
                username = currentState.username
            )
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun requestPhoneChange(newPhone: String, onCodeSent: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = SupabaseMock.Auth.updateUserPhone(newPhone)
            _uiState.value = _uiState.value.copy(isLoading = false)
            
            if (result.isSuccess) {
                onCodeSent()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Ошибка")
            }
        }
    }
    
    fun requestEmailChange(newEmail: String, onCodeSent: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = SupabaseMock.Auth.updateUserEmail(newEmail)
            _uiState.value = _uiState.value.copy(isLoading = false)
            
            if (result.isSuccess) {
                onCodeSent()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Ошибка")
            }
        }
    }

    fun verifyOtp(otp: String, type: OtpType, newContact: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = SupabaseMock.Auth.verifyOtp(otp)
            
            if (result.isSuccess) {
                if (type == OtpType.SMS) {
                    _uiState.value = _uiState.value.copy(phone = newContact, isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                saveProfile()
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
                onError(result.exceptionOrNull()?.message ?: "Неверный код")
            }
        }
    }
    
    fun checkUsername(username: String) {
        if (username.length < 5) {
            _usernameAvailable.value = false
            return
        }
        viewModelScope.launch {
            val available = SupabaseMock.Database.checkUsername(username)
            _usernameAvailable.value = available
        }
    }
    
    fun saveUsername(newUsername: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (_usernameAvailable.value == true) {
            _uiState.value = _uiState.value.copy(username = newUsername)
            saveProfile()
            onSuccess()
        } else {
            onError("Имя пользователя недоступно")
        }
    }
}
