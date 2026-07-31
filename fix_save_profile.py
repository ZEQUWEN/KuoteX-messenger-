with open("app/src/main/java/com/example/ui/AccountViewModel.kt", "r") as f:
    content = f.read()

save_profile_code = """
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
"""

content = content.replace("fun updateBirthDate", save_profile_code + "\n    fun updateBirthDate")

with open("app/src/main/java/com/example/ui/AccountViewModel.kt", "w") as f:
    f.write(content)
print("Restored saveProfile")
