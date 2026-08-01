import re

with open("app/src/main/java/com/example/ui/AccountScreen.kt", "r") as f:
    content = f.read()

target = """    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            snackbarHostState.showSnackbar("Изменения успешно сохранены")
            
            // Sync with global AppViewModel profile
            if (activeAccount != null) {
                appViewModel.updateProfile(
                    id = activeAccount.id,
                    username = "@" + state.username.removePrefix("@"),
                    displayName = state.firstName + if (state.lastName.isNotBlank()) " ${state.lastName}" else "",
                    bio = state.bio,
                    profilePicUrl = state.avatarUrl ?: activeAccount.profilePicUrl
                )
            }
            
            viewModel.resetSuccessFlag()
        }
    }"""

replacement = """    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            // Sync with global AppViewModel profile FIRST so it doesn't get cancelled by snackbar suspend
            if (activeAccount != null) {
                appViewModel.updateProfile(
                    id = activeAccount.id,
                    username = "@" + state.username.removePrefix("@"),
                    displayName = state.firstName + if (state.lastName.isNotBlank()) " ${state.lastName}" else "",
                    bio = state.bio,
                    profilePicUrl = state.avatarUrl ?: activeAccount.profilePicUrl
                )
            }
            viewModel.resetSuccessFlag()
            snackbarHostState.showSnackbar("Изменения успешно сохранены")
        }
    }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/AccountScreen.kt", "w") as f:
    f.write(content)
print("Updated AccountScreen.kt")
