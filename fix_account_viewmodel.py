import re

with open("app/src/main/java/com/example/ui/AccountViewModel.kt", "r") as f:
    content = f.read()

target = """    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isInitialLoading = true)
            delay(1500) // Mocking network load from Supabase
            val profile = SupabaseMock.Database.getProfile()
            lastSavedState = profile
            _uiState.value = profile
        }
    }"""

replacement = """    fun initialize(account: com.example.ui.UserAccount?) {
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
    }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/AccountViewModel.kt", "w") as f:
    f.write(content)
print("Updated AccountViewModel")
