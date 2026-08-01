import re

with open("app/src/main/java/com/example/ui/AuthScreens.kt", "r") as f:
    content = f.read()

target = """                        onClick = {
                            if (accounts.any { it.username == username }) {"""

replacement = """                        onClick = {
                            if (accounts.any { it.username == username || it.phoneNumber == username }) {"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/AuthScreens.kt", "w") as f:
    f.write(content)
print("Updated AuthScreens.kt")
