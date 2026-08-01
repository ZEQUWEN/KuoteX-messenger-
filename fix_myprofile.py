import re

with open("app/src/main/java/com/example/ui/MyProfileScreen.kt", "r") as f:
    content = f.read()

target = """                            title = "+7 (922) 669-26-82", // Example hardcoded or from account"""
replacement = """                            title = activeAccount.phoneNumber.ifBlank { "+7 (922) 669-26-82" },"""
content = content.replace(target, replacement)

target2 = """                        ProfileInfoItem(
                            icon = Icons.Filled.AlternateEmail,
                            title = "@CreepsyDear","""

replacement2 = """                        ProfileInfoItem(
                            icon = Icons.Filled.AlternateEmail,
                            title = activeAccount.username,"""

content = content.replace(target2, replacement2)

with open("app/src/main/java/com/example/ui/MyProfileScreen.kt", "w") as f:
    f.write(content)
print("Updated MyProfileScreen.kt")
