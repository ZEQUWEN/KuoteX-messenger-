import re

with open("app/src/main/java/com/example/ui/StorageSettingsScreen.kt", "r") as f:
    content = f.read()

target = "CacheCalculator.clearAppCache(context)"
replacement = "CacheCalculator.clearAppCache(context, selectedNames)"
content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/StorageSettingsScreen.kt", "w") as f:
    f.write(content)
