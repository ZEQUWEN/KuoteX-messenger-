with open("app/src/main/java/com/example/ui/StorageSettingsScreen.kt", "r") as f:
    content = f.read()

# Add context
start_idx = content.find('val snackbarHostState = remember { SnackbarHostState() }')
if start_idx != -1:
    content = content[:start_idx] + 'val context = LocalContext.current\n    ' + content[start_idx:]

# Call export
export_target = """coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Файлы успешно сохранены в zip архив")
                                }"""
export_replacement = """coroutineScope.launch {
                                    com.example.utils.ExportUtils.exportCacheToZip(context)
                                    snackbarHostState.showSnackbar("Файлы успешно сохранены в zip архив")
                                }"""
content = content.replace(export_target, export_replacement)

with open("app/src/main/java/com/example/ui/StorageSettingsScreen.kt", "w") as f:
    f.write(content)
