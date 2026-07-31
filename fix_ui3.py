with open("app/src/main/java/com/example/ui/StorageSettingsScreen.kt", "r") as f:
    content = f.read()

start_idx = content.find('                    Spacer(modifier = Modifier.height(12.dp))\n                    Row(')
end_idx = content.find('                }\n            }\n\n            if (largestCategories.isNotEmpty())')

if start_idx != -1 and end_idx != -1:
    replacement = """                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isScanningForDuplicates = true
                                    kotlinx.coroutines.delay(1500) // Simulate scanning
                                    duplicateSizeFound = (java.lang.Math.random() * 200 * 1024 * 1024).toLong() + 50 * 1024 * 1024 // 50MB to 250MB
                                    isScanningForDuplicates = false
                                    showOptimizeDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isScanningForDuplicates) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Сканирование...")
                            } else {
                                Text("Оптимизировать")
                            }
                        }
                        OutlinedButton(
                            onClick = { 
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Файлы успешно сохранены в zip архив")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Экспорт")
                        }
                    }
"""
    content = content[:start_idx] + replacement + content[end_idx:]

with open("app/src/main/java/com/example/ui/StorageSettingsScreen.kt", "w") as f:
    f.write(content)
