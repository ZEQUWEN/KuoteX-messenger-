import re
with open("app/src/main/java/com/example/ui/SettingsScreens.kt", "r") as f:
    text = f.read()

text = text.replace('Text("Фон чата"', 'Text("Цвет акцента"')

with open("app/src/main/java/com/example/ui/SettingsScreens.kt", "w") as f:
    f.write(text)
