import re

with open("app/src/main/java/com/example/ui/StorageSettingsScreen.kt", "r") as f:
    content = f.read()

# Replace the first two ListItems and HorizontalDivider
pattern1 = r"""            ListItem\(
                headlineContent = \{ Text\("Использование памяти"\) \},
                supportingContent = \{ Text\("Настройка автоудаления кэша"\) \},
                leadingContent = \{ 
                     Icon\(
                        Icons.Filled.Storage, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    \) 
                 \},
                modifier = Modifier.clickable \{ navController.navigate\("settings/storage/memory"\) \}
            \)
            
            ListItem\(
                headlineContent = \{ Text\("Использование сети"\) \},
                supportingContent = \{ Text\("Статистика трафика"\) \},
                leadingContent = \{ 
                     Icon\(
                        Icons.Filled.DataUsage, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    \) 
                 \},
                modifier = Modifier.clickable \{ navController.navigate\("settings/storage/network"\) \}
            \)
            
            HorizontalDivider\(modifier = Modifier.padding\(vertical = 8.dp\)\)"""

replacement1 = """            Card(
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
            
            Spacer(modifier = Modifier.height(16.dp))"""

content = re.sub(pattern1, replacement1, content, count=1)

with open("app/src/main/java/com/example/ui/StorageSettingsScreen.kt", "w") as f:
    f.write(content)
