package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsStorageScreen(viewModel: AppViewModel, navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Данные и память") },
                navigationIcon = { 
                    IconButton(onClick = { navController.popBackStack() }) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") 
                    } 
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
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
                modifier = Modifier.clickable { navController.navigate("settings/storage/memory") }
            )
            
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
                modifier = Modifier.clickable { navController.navigate("settings/storage/network") }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Автозагрузка медиа",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            var mobileEnabled by remember { mutableStateOf(true) }
            ListItem(
                headlineContent = { Text("Через мобильную сеть") },
                supportingContent = { Text("Фото, Видео (15 MB), Файлы (3 MB)") },
                trailingContent = { Switch(checked = mobileEnabled, onCheckedChange = { mobileEnabled = it }) }
            )
            
            var wifiEnabled by remember { mutableStateOf(true) }
            ListItem(
                headlineContent = { Text("Через сети Wi-Fi") },
                supportingContent = { Text("Фото, Видео (15 MB), Файлы (3 MB)") },
                trailingContent = { Switch(checked = wifiEnabled, onCheckedChange = { wifiEnabled = it }) }
            )
            
            var roamingEnabled by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text("В роуминге") },
                supportingContent = { Text("Фото, Видео (15 MB), Файлы (3 MB)") },
                trailingContent = { Switch(checked = roamingEnabled, onCheckedChange = { roamingEnabled = it }) }
            )
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes == 0L) return "0 B"
    val k = 1024L
    val sizes = arrayOf("B", "KB", "MB", "GB", "TB")
    val i = (Math.log(bytes.toDouble()) / Math.log(k.toDouble())).toInt()
    return String.format("%.1f %s", bytes / Math.pow(k.toDouble(), i.toDouble()), sizes[i])
}

@Composable
fun DonutChart(categories: List<NetworkCategoryStats>, totalBytes: Long, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var startAngle = -90f
            val strokeWidth = 32.dp.toPx()
            
            if (totalBytes > 0) {
                for (category in categories) {
                    val sweepAngle = (category.sizeBytes.toFloat() / totalBytes.toFloat()) * 360f
                    if (sweepAngle > 0) {
                        drawArc(
                            color = category.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )
                    }
                    startAngle += sweepAngle
                }
            } else {
                drawArc(
                    color = Color.DarkGray,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val formatted = formatBytes(totalBytes).split(" ")
            if (formatted.size == 2) {
                Text(formatted[0], style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                Text(formatted[1], style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(formatBytes(totalBytes), style = MaterialTheme.typography.displayMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkUsageScreen(viewModel: AppViewModel, navController: NavController) {
    val networkStatsMap by viewModel.networkStats.collectAsState()
    
    val tabs = listOf(NetworkType.ALL, NetworkType.MOBILE, NetworkType.WIFI, NetworkType.ROAMING)
    val tabTitles = listOf("Весь", "Мобильный", "Wi-Fi", "Роуминг")
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    val currentType = tabs[selectedTabIndex]
    val currentStats = networkStatsMap[currentType] ?: return
    
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Сбросить статистику") },
            text = { Text("Сбросить статистику использования для этого типа сети?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetNetworkStats(currentType)
                    showResetDialog = false
                }) {
                    Text("Сбросить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Использование сети") },
                navigationIcon = { 
                    IconButton(onClick = { navController.popBackStack() }) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") 
                    } 
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    val total = currentStats.sentBytes + currentStats.receivedBytes
                    DonutChart(
                        categories = currentStats.categories, 
                        totalBytes = total,
                        modifier = Modifier.padding(32.dp).size(200.dp).align(Alignment.CenterHorizontally)
                    )
                }
                
                items(currentStats.categories) { category ->
                    val percentage = if (currentStats.sentBytes + currentStats.receivedBytes > 0) {
                        (category.sizeBytes.toFloat() / (currentStats.sentBytes + currentStats.receivedBytes).toFloat() * 100).toInt()
                    } else 0
                    
                    ListItem(
                        headlineContent = { Text(category.categoryName) },
                        leadingContent = { 
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(category.color, shape = CircleShape)
                            )
                        },
                        trailingContent = { 
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatBytes(category.sizeBytes), style = MaterialTheme.typography.bodyLarge)
                                Text("$percentage%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    )
                }
                
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        "Общий расход трафика",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                item {
                    ListItem(
                        headlineContent = { Text("Отправлено байт") },
                        leadingContent = { Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = Color(0xFF2196F3)) },
                        trailingContent = { Text(formatBytes(currentStats.sentBytes), style = MaterialTheme.typography.bodyLarge) }
                    )
                    ListItem(
                        headlineContent = { Text("Получено байт") },
                        leadingContent = { Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = Color(0xFF4CAF50)) },
                        trailingContent = { Text(formatBytes(currentStats.receivedBytes), style = MaterialTheme.typography.bodyLarge) }
                    )
                }
                
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        text = "Сбросить статистику",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showResetDialog = true }
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageUsageScreen(viewModel: AppViewModel, navController: NavController) {
    val storageStats by viewModel.storageStats.collectAsState()
    
    var isClearing by remember { mutableStateOf(false) }
    var clearProgress by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    
    val totalSize = storageStats.categories.sumOf { it.sizeBytes }
    val selectedSize = storageStats.categories.filter { it.isSelected }.sumOf { it.sizeBytes }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Использование памяти") },
                navigationIcon = { 
                    IconButton(onClick = { navController.popBackStack() }) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") 
                    } 
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(200.dp)) {
                            var startAngle = -90f
                            val strokeWidth = 32.dp.toPx()
                            
                            if (totalSize > 0) {
                                for (category in storageStats.categories) {
                                    val sweepAngle = (category.sizeBytes.toFloat() / totalSize.toFloat()) * 360f
                                    if (sweepAngle > 0) {
                                        drawArc(
                                            color = category.color,
                                            startAngle = startAngle,
                                            sweepAngle = sweepAngle,
                                            useCenter = false,
                                            style = Stroke(width = strokeWidth)
                                        )
                                    }
                                    startAngle += sweepAngle
                                }
                            } else {
                                drawArc(
                                    color = Color.DarkGray,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                            }
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val formatted = formatBytes(totalSize).split(" ")
                            if (formatted.size == 2) {
                                Text(formatted[0], style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                                Text(formatted[1], style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text(formatBytes(totalSize), style = MaterialTheme.typography.displayMedium)
                            }
                        }
                    }
                }
                
                items(storageStats.categories) { category ->
                    val percentage = if (totalSize > 0) {
                        (category.sizeBytes.toFloat() / totalSize.toFloat() * 100).toInt()
                    } else 0
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleStorageCategory(category.categoryName) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (category.isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (category.isSelected) category.color else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(category.categoryName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Text("$percentage%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(formatBytes(category.sizeBytes), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                item {
                    if (isClearing) {
                        CacheClearAnimation(
                            progress = clearProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .padding(horizontal = 16.dp)
                        )
                    } else {
                        Button(
                            onClick = {
                                if (selectedSize > 0) {
                                    isClearing = true
                                    clearProgress = 0f
                                    scope.launch {
                                        val steps = 100
                                        for (i in 0..steps) {
                                            clearProgress = i / steps.toFloat()
                                            delay(20) 
                                        }
                                        delay(500)
                                        val selectedNames = storageStats.categories.filter { it.isSelected }.map { it.categoryName }
                                        viewModel.clearCache(selectedNames)
                                        isClearing = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            enabled = selectedSize > 0
                        ) {
                            Text("Очистить кэш (${formatBytes(selectedSize)})")
                        }
                    }
                }
                
                item {
                    CacheManagerSection(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun CacheClearAnimation(progress: Float, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "flame")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "time"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val corner = CornerRadius(h / 2, h / 2)
        
        drawRoundRect(
            color = Color.DarkGray,
            size = Size(w, h),
            cornerRadius = corner
        )

        val clearWidth = w * progress

        clipRect(right = clearWidth) {
            drawRoundRect(
                color = Color(0xFF4CAF50),
                size = Size(w, h),
                cornerRadius = corner
            )
        }

        if (progress > 0f && progress < 1f) {
            val flameX = clearWidth
            val flameYOffset = sin(time * 10f) * (h * 0.2f)
            
            val broomPath = Path().apply {
                moveTo(flameX, h * 0.9f)
                quadraticBezierTo(
                    flameX + h * 0.5f + sin(time * 15f) * h * 0.2f, h * 0.5f,
                    flameX, -h * 0.2f + flameYOffset
                )
                quadraticBezierTo(
                    flameX - h * 0.5f + sin(time * 12f) * h * 0.2f, h * 0.5f,
                    flameX, h * 0.9f
                )
                close()
            }
            
            val innerBroomPath = Path().apply {
                moveTo(flameX, h * 0.8f)
                quadraticBezierTo(
                    flameX + h * 0.3f + sin(time * 20f) * h * 0.1f, h * 0.6f,
                    flameX, h * 0.1f + flameYOffset * 0.5f
                )
                quadraticBezierTo(
                    flameX - h * 0.3f + sin(time * 18f) * h * 0.1f, h * 0.6f,
                    flameX, h * 0.8f
                )
                close()
            }

            drawPath(broomPath, color = Color(0xFFFF5722)) 
            drawPath(innerBroomPath, color = Color(0xFFFFEB3B))
        }
    }
}

@Composable
fun CacheManagerSection(viewModel: AppViewModel) {
    val maxCacheSizeIndex by viewModel.maxCacheSizeIndex.collectAsState()
    
    val steps = listOf("5 GB", "16 GB", "32 GB", "∞")
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                "Максимальный размер кэша",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.Center)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = maxCacheSizeIndex / 3f)
                        .height(2.dp)
                        .align(Alignment.CenterStart)
                        .background(MaterialTheme.colorScheme.primary)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { index, label ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    viewModel.setMaxCacheSizeIndex(index)
                                }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (index <= maxCacheSizeIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(if (index == maxCacheSizeIndex) 16.dp else 8.dp)
                                    .background(
                                        if (index <= maxCacheSizeIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
    
    Text(
        text = "Если размер кэша превысит этот лимит, самые старые из неиспользуемых медиа будут удалены из памяти устройства.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start,
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}
