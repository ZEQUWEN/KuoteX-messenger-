import re

with open("app/src/main/java/com/example/ui/AuthScreens.kt", "r") as f:
    content = f.read()

target_btn = """                        onClick = {
                            if (accounts.any { it.username == username }) {
                                android.widget.Toast.makeText(
                                    androidx.compose.ui.platform.LocalContext.current,
                                    "Аккаунт с таким номером/email уже существует",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } else {"""

replacement_btn = """                        onClick = {
                            if (accounts.any { it.username == username }) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Аккаунт с таким номером/email уже существует",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } else {"""
content = content.replace(target_btn, replacement_btn)

target_scope = """    val scope = rememberCoroutineScope()"""
replacement_scope = """    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current"""
content = content.replace(target_scope, replacement_scope)

with open("app/src/main/java/com/example/ui/AuthScreens.kt", "w") as f:
    f.write(content)
print("Updated AuthScreens.kt")
