import re

with open("app/src/main/java/com/example/ui/SettingsScreens.kt", "r") as f:
    content = f.read()

target = """                val phoneNumber = "+7 (922) 669-26-82" // Dummy phone number as requested to match style"""
replacement = """                val phoneNumber = activeAccount.phoneNumber.ifBlank { "+7 (922) 669-26-82" }"""
content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/SettingsScreens.kt", "w") as f:
    f.write(content)
print("Updated SettingsScreens.kt")
