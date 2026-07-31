with open("app/src/main/java/com/example/ui/AccountViewModel.kt", "r") as f:
    content = f.read()

target = """    fun updateAvatar(url: String) {
        _uiState.value = _uiState.value.copy(avatarUrl = url)
        viewModelScope.launch {
            SupabaseMock.Database.updateAvatar(url)
        }
    }"""

replacement = """    fun updateAvatar(url: String) {
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
    }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/AccountViewModel.kt", "w") as f:
    f.write(content)
print("Fixed updateAvatar")
