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
    
    // In-memory mock data
    var mockFirstName = "SARATOSHI"
    var mockLastName = "NARIMOTO"
    var mockBio = "✨Занимаюсь дизайном карточек товаров и вайбкодингом, это моё хобби✨"
    var mockPhone = "+7 (922) 669-26-82"
    var mockUsername = "CreepsyDear"
    var mockBirthDate = "21 июн. 2005"
    var mockAvatarUrl: String? = null
    var mockSocialLinks = mapOf(
        "telegram" to "https://t.me/CreepsyDear",
        "github" to "https://github.com/CreepsyDear"
    )

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
            // In a real app we'd wipe the state, but here it's just a mock
            return Result.success(Unit)
        }
    }

    object Database {
        suspend fun updateProfile(firstName: String, lastName: String, bio: String, birthDate: String, username: String, phone: String = mockPhone): Result<Unit> {
            delay(1000)
            mockFirstName = firstName
            mockLastName = lastName
            mockBio = bio
            mockBirthDate = birthDate
            mockUsername = username
            mockPhone = phone
            return Result.success(Unit)
        }
        
        suspend fun checkUsername(username: String): Boolean {
            delay(500)
            return username != "admin" && username != "telegram"
        }
        
        suspend fun updateAvatar(url: String): Result<Unit> {
            delay(1000)
            mockAvatarUrl = url
            return Result.success(Unit)
        }
        
        suspend fun updateSocialLinks(links: Map<String, String>): Result<Unit> {
            delay(1000)
            mockSocialLinks = links
            return Result.success(Unit)
        }
        
        fun getProfile(): AccountState {
            return AccountState(
                firstName = mockFirstName,
                lastName = mockLastName,
                bio = mockBio,
                phone = mockPhone,
                username = mockUsername,
                birthDate = mockBirthDate,
                avatarUrl = mockAvatarUrl,
                socialLinks = mockSocialLinks,
                isInitialLoading = false
            )
        }
    }
}

data class AccountState(
    val firstName: String = "",
    val lastName: String = "",
    val bio: String = "",
    val phone: String = "",
    val username: String = "",
    val birthDate: String = "",
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

private var lastSavedState: AccountState? = null

    fun initialize(account: com.example.ui.UserAccount?) {
        if (account == null) return
        val parts = account.displayName.split(" ", limit = 2)
        val firstName = parts.getOrElse(0) { "" }
        val lastName = parts.getOrElse(1) { "" }
        val profile = AccountState(
            firstName = firstName,
            lastName = lastName,
            bio = account.bio,
            phone = account.phoneNumber.ifBlank { "+7 (922) 669-26-82" },
            username = account.username,
            avatarUrl = account.profilePicUrl,
            isInitialLoading = false,
            birthDate = "21 июн. 2005" // Default since we don't store it yet
        )
        lastSavedState = profile
        _uiState.value = profile
    }

    fun hasUnsavedChanges(firstName: String, lastName: String, bio: String, socialLinks: Map<String, String>): Boolean {
        val currentSaved = lastSavedState ?: _uiState.value
        return firstName != currentSaved.firstName ||
               lastName != currentSaved.lastName ||
               bio != currentSaved.bio ||
               socialLinks != currentSaved.socialLinks
    }

    fun updateProfileData(firstName: String, lastName: String, bio: String, socialLinks: Map<String, String>) {
        val previousState = _uiState.value
        val newState = previousState.copy(
            firstName = firstName, 
            lastName = lastName, 
            bio = bio,
            socialLinks = socialLinks,
            isSuccess = false,
            error = null
        )
        
        // Optimistic UI update
        _uiState.value = newState

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val profileResult = SupabaseMock.Database.updateProfile(
                firstName = firstName,
                lastName = lastName,
                bio = bio,
                birthDate = newState.birthDate,
                username = newState.username,
                phone = newState.phone
            )
            
            val socialResult = SupabaseMock.Database.updateSocialLinks(socialLinks)
            
            if (profileResult.isSuccess && socialResult.isSuccess) {
                val finalState = newState.copy(isLoading = false, isSuccess = true)
                lastSavedState = finalState
                _uiState.value = finalState
            } else {
                // Rollback on failure
                _uiState.value = previousState.copy(
                    isLoading = false,
                    error = profileResult.exceptionOrNull()?.message ?: socialResult.exceptionOrNull()?.message ?: "Ошибка сохранения"
                )
            }
        }
    }
    
    fun resetSuccessFlag() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }

    
    fun saveProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, isSuccess = false)
            val currentState = _uiState.value
            val result = SupabaseMock.Database.updateProfile(
                firstName = currentState.firstName,
                lastName = currentState.lastName,
                bio = currentState.bio,
                birthDate = currentState.birthDate,
                username = currentState.username,
                phone = currentState.phone
            )
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                lastSavedState = _uiState.value
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun updateBirthDate(date: String) {
        _uiState.value = _uiState.value.copy(birthDate = date)
        saveProfile()
    }
    
    fun updateAvatar(url: String) {
        _uiState.value = _uiState.value.copy(avatarUrl = url, isSuccess = false)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = SupabaseMock.Database.updateAvatar(url)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                lastSavedState = _uiState.value
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
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
