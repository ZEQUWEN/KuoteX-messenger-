with open("app/src/main/java/com/example/ui/StorageSettingsScreen.kt", "r") as f:
    content = f.read()

start_idx = content.find('ListItem(\n                headlineContent = { Text("Использование памяти") }')
end_idx = content.find('var showOptimizeDialog', start_idx)

if start_idx != -1 and end_idx != -1:
    replacement = """Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Использование памяти") },
                        supportingContent = { Text("Настройка автоудаления кэша") },
                        leadingContent = { 
                            Icon(
                                Icons.Filled.Storage, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            ) 
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { navController.navigate("settings/storage/memory") }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    
                    ListItem(
                        headlineContent = { Text("Использование сети") },
                        supportingContent = { Text("Статистика трафика") },
                        leadingContent = { 
                            Icon(
                                Icons.Filled.DataUsage, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            ) 
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { navController.navigate("settings/storage/network") }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            """
    content = content[:start_idx] + replacement + content[end_idx:]

with open("app/src/main/java/com/example/ui/StorageSettingsScreen.kt", "w") as f:
    f.write(content)
