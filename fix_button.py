import re

with open("app/src/main/java/com/example/ui/AccountScreen.kt", "r") as f:
    content = f.read()

target = """                        Button(
                            onClick = { 
                                viewModel.updateBirthDate(selectedDate)
                                showDatePicker = false 
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                            Text("Сохранить")
                        }"""

replacement = """                        Button(
                            onClick = { 
                                viewModel.updateBirthDate(selectedDate)
                                showDatePicker = false 
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Сохранить")
                        }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/AccountScreen.kt", "w") as f:
        f.write(content)
    print("Replaced!")
else:
    print("Target not found.")
