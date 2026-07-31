import re

with open("app/src/main/java/com/example/ui/AccountScreen.kt", "r") as f:
    content = f.read()

# Add imports
imports = """
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.layout.ContentScale
import android.net.Uri
"""
content = content.replace("import androidx.lifecycle.viewmodel.compose.viewModel", "import androidx.lifecycle.viewmodel.compose.viewModel\n" + imports)

# Replace the Avatar section
target_avatar = """                // Avatar section
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { viewModel.updateAvatar("mock_avatar_url") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.avatarUrl != null) {
                                Icon(Icons.Default.Person, contentDescription = "Avatar", modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            } else {
                                Icon(Icons.Default.AddAPhoto, contentDescription = "Add Avatar", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }"""

replacement_avatar = """                // Avatar section
                item {
                    var showCropDialog by remember { mutableStateOf<Uri?>(null) }
                    val photoPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickVisualMedia(),
                        onResult = { uri ->
                            if (uri != null) {
                                showCropDialog = uri
                            }
                        }
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { 
                                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.avatarUrl != null) {
                                AsyncImage(
                                    model = state.avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.AddAPhoto, contentDescription = "Add Avatar", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                    
                    if (showCropDialog != null) {
                        AlertDialog(
                            onDismissRequest = { showCropDialog = null },
                            title = { Text("Обрезка фото") },
                            text = { 
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Настройте область видимости фото профиля.", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(16.dp))
                                    Box(modifier = Modifier.size(200.dp).clip(CircleShape).background(Color.Black)) {
                                        AsyncImage(
                                            model = showCropDialog,
                                            contentDescription = "Crop",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        // Mock crop overlay
                                        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.2f)))
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    viewModel.updateAvatar(showCropDialog.toString())
                                    showCropDialog = null
                                }) {
                                    Text("Готово")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCropDialog = null }) {
                                    Text("Отмена")
                                }
                            }
                        )
                    }
                }"""

content = content.replace(target_avatar, replacement_avatar)

with open("app/src/main/java/com/example/ui/AccountScreen.kt", "w") as f:
    f.write(content)
print("Updated avatar picker")
