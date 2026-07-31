with open("app/src/main/java/com/example/ui/MainScreen.kt", "r") as f:
    content = f.read()

content = content.replace(
    "AccountScreen(onBack = { mainNavController.popBackStack() })",
    "AccountScreen(onBack = { mainNavController.popBackStack() }, appViewModel = viewModel)"
)

with open("app/src/main/java/com/example/ui/MainScreen.kt", "w") as f:
    f.write(content)
print("Updated MainScreen")
