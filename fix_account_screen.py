import re

with open("app/src/main/java/com/example/ui/AccountScreen.kt", "r") as f:
    content = f.read()

# Update signature
old_sig = """fun AccountScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = viewModel()
) {"""
new_sig = """fun AccountScreen(
    onBack: () -> Unit,
    appViewModel: com.example.ui.AppViewModel,
    viewModel: AccountViewModel = viewModel()
) {"""
content = content.replace(old_sig, new_sig)

# Update LaunchedEffect for isSuccess
target = """    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            snackbarHostState.showSnackbar("Изменения успешно сохранены")
            viewModel.resetSuccessFlag()
        }
    }"""
replacement = """    val activeAccount = com.example.ui.LocalActiveAccount.current

    LaunchedEffect(state.isSuccess) {
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
content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/AccountScreen.kt", "w") as f:
    f.write(content)
print("Updated AccountScreen")
