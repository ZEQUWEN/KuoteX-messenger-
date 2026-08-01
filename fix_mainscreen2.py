import re

with open("app/src/main/java/com/example/ui/MainScreen.kt", "r") as f:
    content = f.read()

target = """                    composable("register") {
                        RegistrationScreen(
                            onNavigateToLogin = { rootNavController.navigate("auth") },
                            onRegisterSuccess = { username ->
                                viewModel.clearAddingAccount() 
                                viewModel.createAccount(username, username)
                            }
                        )
                    }"""

replacement = """                    composable("register") {
                        RegistrationScreen(
                            accounts = accounts,
                            onNavigateToLogin = { rootNavController.navigate("auth") },
                            onRegisterSuccess = { username ->
                                viewModel.clearAddingAccount() 
                                viewModel.createAccount(username, username)
                            }
                        )
                    }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/MainScreen.kt", "w") as f:
    f.write(content)
print("Updated MainScreen.kt")
