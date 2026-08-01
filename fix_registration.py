import re

with open("app/src/main/java/com/example/ui/AuthScreens.kt", "r") as f:
    content = f.read()

target_sig = """fun RegistrationScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: (String) -> Unit
) {"""

replacement_sig = """fun RegistrationScreen(
    accounts: List<com.example.ui.UserAccount>,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: (String) -> Unit
) {"""

content = content.replace(target_sig, replacement_sig)

target_btn = """                        onClick = {
                            scope.launch {
                                isLoading = true
                                delay(1500)
                                isLoading = false
                                onRegisterSuccess(username)
                            }
                        },"""

replacement_btn = """                        onClick = {
                            if (accounts.any { it.username == username }) {
                                android.widget.Toast.makeText(
                                    androidx.compose.ui.platform.LocalContext.current,
                                    "Аккаунт с таким номером/email уже существует",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } else {
                                scope.launch {
                                    isLoading = true
                                    delay(1500)
                                    isLoading = false
                                    onRegisterSuccess(username)
                                }
                            }
                        },"""

content = content.replace(target_btn, replacement_btn)

with open("app/src/main/java/com/example/ui/AuthScreens.kt", "w") as f:
    f.write(content)
print("Updated AuthScreens.kt")
