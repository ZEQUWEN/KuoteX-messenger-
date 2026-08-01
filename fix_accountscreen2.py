import re

with open("app/src/main/java/com/example/ui/AccountScreen.kt", "r") as f:
    content = f.read()

target = """    val activeAccount = com.example.ui.LocalActiveAccount.current"""

replacement = """    val activeAccount = com.example.ui.LocalActiveAccount.current

    LaunchedEffect(activeAccount) {
        if (state.isInitialLoading) {
            viewModel.initialize(activeAccount)
        }
    }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/AccountScreen.kt", "w") as f:
    f.write(content)
print("Updated AccountScreen.kt")
