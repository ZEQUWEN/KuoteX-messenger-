with open("app/src/main/java/com/example/ui/StorageSettingsScreen.kt", "r") as f:
    content = f.read()

start_idx = content.find('            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))\n            \n            Text(\n                text = "Автозагрузка медиа"')
end_idx = content.find('        }\n    }\n}\n\nfun formatBytes')

if start_idx != -1 and end_idx != -1:
    replacement = """            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Автозагрузка медиа",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    var mobileEnabled by remember { mutableStateOf(true) }
                    ListItem(
                        headlineContent = { Text("Через мобильную сеть") },
                        supportingContent = { Text("Фото, Видео (15 MB), Файлы (3 MB)") },
                        trailingContent = { Switch(checked = mobileEnabled, onCheckedChange = { mobileEnabled = it }) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    
                    var wifiEnabled by remember { mutableStateOf(true) }
                    ListItem(
                        headlineContent = { Text("Через сети Wi-Fi") },
                        supportingContent = { Text("Фото, Видео (15 MB), Файлы (3 MB)") },
                        trailingContent = { Switch(checked = wifiEnabled, onCheckedChange = { wifiEnabled = it }) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    
                    var roamingEnabled by remember { mutableStateOf(false) }
                    ListItem(
                        headlineContent = { Text("В роуминге") },
                        supportingContent = { Text("Фото, Видео (15 MB), Файлы (3 MB)") },
                        trailingContent = { Switch(checked = roamingEnabled, onCheckedChange = { roamingEnabled = it }) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
"""
    content = content[:start_idx] + replacement + content[end_idx:]

with open("app/src/main/java/com/example/ui/StorageSettingsScreen.kt", "w") as f:
    f.write(content)
