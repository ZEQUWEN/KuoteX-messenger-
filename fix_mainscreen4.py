import re

with open("app/src/main/java/com/example/ui/MainScreen.kt", "r") as f:
    content = f.read()

target = """                        RegistrationScreen(
                            accounts = accounts,
                            onNavigateToLogin = { rootNavController.navigate("auth") },
                            onRegisterSuccess = { phoneNumber ->
                                viewModel.clearAddingAccount() 
                                viewModel.createAccount(phoneNumber = phoneNumber, username = phoneNumber, displayName = phoneNumber)
                            }
                        )"""

replacement = """                        RegistrationScreen(
                            accounts = accounts,
                            onNavigateToLogin = { rootNavController.navigate("auth") },
                            onRegisterSuccess = { username ->
                                viewModel.clearAddingAccount() 
                                viewModel.createAccount(phoneNumber = username, username = username, displayName = username)
                            }
                        )"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/MainScreen.kt", "w") as f:
    f.write(content)
print("Updated MainScreen.kt")
