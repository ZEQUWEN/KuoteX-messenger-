package com.example.ui

import androidx.compose.foundation.Image
import com.example.ui.botapi.BotRegistry
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.zIndex
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.navigation.NavDestination
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.LazyRow

val LocalActiveAccount = compositionLocalOf<UserAccount?> { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigation(viewModel: AppViewModel) {
    var showSplash by remember { mutableStateOf(true) }
    
    if (showSplash) {
        SplashScreen { showSplash = false }
        return
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val theme by viewModel.theme.collectAsState()
    val isBatterySaverSetting by viewModel.batterySaverEnabled.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var showStorageAlert by remember { mutableStateOf(false) }
    var storageAlertMessage by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                fun getFolderSize(file: java.io.File): Long {
                    var size: Long = 0
                    if (file.isDirectory) {
                        file.listFiles()?.forEach {
                            size += getFolderSize(it)
                        }
                    } else {
                        size = file.length()
                    }
                    return size
                }
                
                val dbSize = getFolderSize(context.getDatabasePath("messenger_db").parentFile ?: context.filesDir)
                val cacheSize = getFolderSize(context.cacheDir)
                val totalAppSize = dbSize + cacheSize
                
                val stat = android.os.StatFs(context.filesDir.path)
                val availableBytes = stat.availableBytes
                
                // If less than 500 MB remaining, alert the user
                if (availableBytes < 500L * 1024 * 1024 || totalAppSize > 1024L * 1024 * 1024) { 
                    storageAlertMessage = "Storage Warning: Device has ${availableBytes / (1024 * 1024)} MB free. App encrypted database and cache are currently using ${totalAppSize / (1024 * 1024)} MB. Please free up space."
                    showStorageAlert = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    if (showStorageAlert) {
        AlertDialog(
            onDismissRequest = { showStorageAlert = false },
            title = { Text("Storage Limit Warning") },
            text = { Text(storageAlertMessage) },
            confirmButton = {
                TextButton(onClick = { showStorageAlert = false }) { Text("OK") }
            }
        )
    }
    var isCharging by remember { mutableStateOf(false) }
    var batteryLevel by remember { mutableStateOf(100f) }
    androidx.compose.runtime.DisposableEffect(context) {
        val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context, intent: android.content.Intent) {
                val status: Int = intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
                
                val level: Int = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    batteryLevel = level * 100f / scale.toFloat()
                }
            }
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
    val isBatterySaver = isBatterySaverSetting && batteryLevel < 20f
    val opacity by viewModel.themeOpacity.collectAsState()
    val requires2FA by viewModel.requires2FA.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val isAddingAccount by viewModel.isAddingAccount.collectAsState()
    val activeAccount = accounts.find { it.isActive }
    
    // We only consider auth complete if we have an active account AND 2FA is not required
    val isAuthComplete = activeAccount != null && requires2FA == null && !isAddingAccount
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showCreateChannelDialog by remember { mutableStateOf(false) }
    var showCreateSecretChatDialog by remember { mutableStateOf(false) }
    var isStoryExpanded by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalActiveAccount provides activeAccount) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Render Active Theme Canvas in background
            androidx.compose.animation.Crossfade(
                targetState = theme,
                animationSpec = tween(1000),
                label = "ThemeTransition"
            ) { targetTheme ->
                when (targetTheme) {
                    AppTheme.NEON_SNOWFLAKES -> NeonSnowflakesBackground(isBatterySaver = isBatterySaver, opacity = 1f)
                    AppTheme.NEON_CHERRY_BLOSSOM -> NeonCherryBlossomBackground(isBatterySaver = isBatterySaver, opacity = 1f)
                    AppTheme.NEON_CONFETTI -> NeonConfettiBackground(isBatterySaver = isBatterySaver, opacity = 1f)
                    AppTheme.NEON_MOON -> NeonMoonBackground(opacity = 1f)
                    AppTheme.NEON_ROOM_FOG -> NeonRoomFogBackground(opacity = 1f)
                    AppTheme.DEFAULT -> ElegantDarkBackground(opacity = 1f)
                }
            }
            // ParticleOverlay(theme)


            if (!isAuthComplete) {
                val rootNavController = rememberNavController()
                NavHost(
                    navController = rootNavController,
                    startDestination = if (requires2FA != null) "2fa" else "auth",
                    modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()
                ) {
                    composable("auth") {
                        LoginScreen(
                            accounts = accounts,
                            onNavigateToRegister = { rootNavController.navigate("register") },
                            forceManualLogin = isAddingAccount,
                            onLoginSuccess = { username ->
                                viewModel.clearAddingAccount() 
                                val accountId = accounts.find { it.username == username || it.displayName == username }?.id ?: "1"
                                viewModel.switchAccount(accountId)
                            }
                        )
                    }
                    composable("register") {
                        RegistrationScreen(
                            onNavigateToLogin = { rootNavController.navigate("auth") },
                            onRegisterSuccess = { username ->
                                viewModel.clearAddingAccount() 
                                viewModel.createAccount(username, username)
                            }
                        )
                    }
                    composable("2fa") {
                        TwoFactorAuthScreen(
                            onVerify = { viewModel.verify2FA(it) },
                            onCancel = { viewModel.cancel2FA() }
                        )
                    }
                }
            } else {
                val mainNavController = rememberNavController()
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            modifier = Modifier.width(300.dp),
                            drawerContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ) {
                            AccountDrawerContent(
                                viewModel = viewModel,
                                onCloseDrawer = { scope.launch { drawerState.close() } },
                                navController = mainNavController,
                                onCreateGroupClick = { showCreateGroupDialog = true },
                                onCreateChannelClick = { showCreateChannelDialog = true },
                                onCreateSecretChatClick = { showCreateSecretChatDialog = true }
                            )
                        }
                    }
                ) {
            if (showCreateSecretChatDialog) {
                var secretChatName by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showCreateSecretChatDialog = false },
                    title = { Text("New Secret Chat") },
                    text = {
                        OutlinedTextField(
                            value = secretChatName,
                            onValueChange = { secretChatName = it },
                            label = { Text("Contact Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.createSecretChat(secretChatName)
                                showCreateSecretChatDialog = false
                            },
                            enabled = secretChatName.isNotBlank()
                        ) {
                            Text("Create")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateSecretChatDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            if (showCreateGroupDialog) {
                CreateChatDialog(
                    isGroup = true,
                    onDismiss = { showCreateGroupDialog = false },
                    onCreate = { name, desc, photo, isPrivate, linkOrUsername ->
                        viewModel.createChat(name, desc, photo, isPrivate, linkOrUsername, true, false)
                    }
                )
            }
            if (showCreateChannelDialog) {
                CreateChatDialog(
                    isGroup = false,
                    onDismiss = { showCreateChannelDialog = false },
                    onCreate = { name, desc, photo, isPrivate, linkOrUsername ->
                        viewModel.createChat(name, desc, photo, isPrivate, linkOrUsername, false, true)
                    }
                )
            }
                    val currentBackStackEntry by mainNavController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStackEntry?.destination?.route?.substringBefore("/")

                    Scaffold(
                        containerColor = Color.Transparent, // Let Canvas show through
                        topBar = {
                            if (currentRoute == "chat_list") {
                                TopAppBar(
                                    title = { 
                                        androidx.compose.animation.AnimatedVisibility(visible = !isStoryExpanded) {
                                            Text("Neon Messenger", fontWeight = FontWeight.Bold) 
                                        }
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color(0xFF18181B).copy(alpha = 0.4f), // bg-zinc-900/40
                                        titleContentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    actions = {
                                    }
                                )
                            }
                        },
                        floatingActionButton = {
                            if (currentRoute == "chat_list") {
                                FloatingActionButton(
                                    onClick = { mainNavController.navigate("contacts") },
                                    containerColor = Color(0xFFDB2777), // bg-pink-600
                                    contentColor = Color.White,
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, "New Chat")
                                }
                            }
                        }
                    ) { padding ->
                        NavHost(
                            navController = mainNavController,
                            startDestination = "chat_list",
                            modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding()
                        ) {
                            composable("chat_list") { ChatListScreen(viewModel, mainNavController, isStoryExpanded) { isStoryExpanded = it } }
                            composable("archived_chats") { ArchivedChatsScreen(viewModel, mainNavController) }
                            composable("archive_settings") { ArchiveSettingsScreen(mainNavController) }
                            composable("discover_bots") { DiscoverBotsScreen(viewModel, mainNavController) }
                            composable("contacts") { ContactsScreen(viewModel, mainNavController) }
                            composable("calls") { CallsListScreen(mainNavController) }
                            composable("settings") { SettingsMenuScreen(viewModel, mainNavController) }
                            composable("settings/accounts") { SettingsAccountsScreen(viewModel, mainNavController) }
                            composable("settings/profile?highlightId={highlightId}", arguments = listOf(navArgument("highlightId") { nullable = true; defaultValue = null })) { backStackEntry ->
                                val highlightId = backStackEntry.arguments?.getString("highlightId")
                                androidx.compose.runtime.LaunchedEffect(highlightId) { viewModel.setHighlightEvent(highlightId) }
                                MyProfileScreen(viewModel, mainNavController)
                            }
                            composable("settings/general?highlightId={highlightId}", arguments = listOf(navArgument("highlightId") { nullable = true; defaultValue = null })) { backStackEntry ->
                                val highlightId = backStackEntry.arguments?.getString("highlightId")
                                androidx.compose.runtime.LaunchedEffect(highlightId) { viewModel.setHighlightEvent(highlightId) }
                                SettingsGeneralScreen(viewModel, mainNavController)
                            }
                            composable("settings/storage?highlightId={highlightId}", arguments = listOf(navArgument("highlightId") { nullable = true; defaultValue = null })) { backStackEntry ->
                                val highlightId = backStackEntry.arguments?.getString("highlightId")
                                androidx.compose.runtime.LaunchedEffect(highlightId) { viewModel.setHighlightEvent(highlightId) }
                                SettingsStorageScreen(viewModel, mainNavController)
                            }
                            composable("settings/storage/memory") {
                                StorageUsageScreen(viewModel, mainNavController)
                            }
                            composable("settings/storage/network") {
                                NetworkUsageScreen(viewModel, mainNavController)
                            }

                            composable("settings/themes?highlightId={highlightId}", arguments = listOf(navArgument("highlightId") { nullable = true; defaultValue = null })) { backStackEntry ->
                                val highlightId = backStackEntry.arguments?.getString("highlightId")
                                androidx.compose.runtime.LaunchedEffect(highlightId) { viewModel.setHighlightEvent(highlightId) }
                                SettingsThemesScreen(viewModel, mainNavController)
                            }
                            composable("settings/security?highlightId={highlightId}", arguments = listOf(navArgument("highlightId") { nullable = true; defaultValue = null })) { backStackEntry ->
                                val highlightId = backStackEntry.arguments?.getString("highlightId")
                                androidx.compose.runtime.LaunchedEffect(highlightId) { viewModel.setHighlightEvent(highlightId) }
                                SettingsSecurityScreen(viewModel, mainNavController)
                            }
                            composable("settings/two_step") { TwoStepVerificationScreen(mainNavController) }
                            composable("settings/passcode") { PasscodeLockScreen(viewModel, mainNavController) }
                            composable("settings/email") { LoginEmailScreen(viewModel, mainNavController) }
                            composable("settings/verify_email") { VerifyEmailScreen(viewModel, mainNavController) }
                            composable("settings/devices?highlightId={highlightId}", arguments = listOf(navArgument("highlightId") { nullable = true; defaultValue = null })) { backStackEntry ->
                                val highlightId = backStackEntry.arguments?.getString("highlightId")
                                androidx.compose.runtime.LaunchedEffect(highlightId) { viewModel.setHighlightEvent(highlightId) }
                                DevicesScreen(mainNavController)
                            }
                            composable("settings/blocked_users") { BlockedUsersScreen(viewModel, mainNavController) }
                            composable("settings/language?highlightId={highlightId}", arguments = listOf(navArgument("highlightId") { nullable = true; defaultValue = null })) { backStackEntry ->
                                val highlightId = backStackEntry.arguments?.getString("highlightId")
                                androidx.compose.runtime.LaunchedEffect(highlightId) { viewModel.setHighlightEvent(highlightId) }
                                SettingsLanguageScreen(viewModel, mainNavController)
                            }
                            composable("settings/battery?highlightId={highlightId}", arguments = listOf(navArgument("highlightId") { nullable = true; defaultValue = null })) { backStackEntry ->
                                val highlightId = backStackEntry.arguments?.getString("highlightId")
                                androidx.compose.runtime.LaunchedEffect(highlightId) { viewModel.setHighlightEvent(highlightId) }
                                SettingsBatterySaverScreen(viewModel, mainNavController)
                            }
                            composable("settings/privacy/{title}") { backStackEntry -> 
                                val title = backStackEntry.arguments?.getString("title") ?: ""
                                PrivacySettingScreen(mainNavController, title)
                            }
                            composable("chat/{chatId}") { backStackEntry -> 
                                val chatId = backStackEntry.arguments?.getString("chatId")
                                if (chatId != null) {
                                    ChatScreen(viewModel, chatId, mainNavController)
                                }
                            }
                            composable(
                                route = "call/{chatId}?isVideo={isVideo}",
                                arguments = listOf(
                                    androidx.navigation.navArgument("isVideo") {
                                        type = androidx.navigation.NavType.BoolType
                                        defaultValue = false
                                    }
                                )
                            ) { backStackEntry ->
                                val chatId = backStackEntry.arguments?.getString("chatId")
                                val isVideo = backStackEntry.arguments?.getBoolean("isVideo") ?: false
                                if (chatId != null) {
                                    CallScreen(viewModel, chatId, isVideo, mainNavController)
                                }
                            }
                            composable("sandbox/{botId}") { backStackEntry ->
                                val botId = backStackEntry.arguments?.getString("botId")
                                if (botId != null) {
                                    SandboxScreen(viewModel, botId, mainNavController)
                                }
                            }
                            composable("dashboard/{botId}") { backStackEntry ->
                                val botId = backStackEntry.arguments?.getString("botId")
                                if (botId != null) {
                                    BotDashboardScreen(botId, mainNavController)
                                }
                            }
                            composable("profile/{chatId}") { backStackEntry ->
                                val chatId = backStackEntry.arguments?.getString("chatId")
                                if (chatId != null) {
                                    ProfileScreen(viewModel, chatId, mainNavController)
                                }
                            }
                            composable("group_admin/{chatId}") { backStackEntry -> 
                                val chatId = backStackEntry.arguments?.getString("chatId")
                                if (chatId != null) {
                                    GroupAdminScreen(viewModel, chatId, mainNavController)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun TwoFactorAuthScreen(
    onVerify: (String) -> Unit,
    onCancel: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha=0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Lock, contentDescription = "2FA", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Two-Factor Authentication", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Enter the 6-digit code from your authenticator app.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it },
                    placeholder = { Text("000000") },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Center, letterSpacing = 8.sp, fontSize = 24.sp)
                )
                
                Spacer(Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onVerify(code) },
                        enabled = code.length == 6
                    ) {
                        Text("Verify")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(viewModel: AppViewModel, navController: NavController, isStoryExpanded: Boolean, onStoryExpandedChange: (Boolean) -> Unit) {
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val typingChats by viewModel.typingChats.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Personal", "Groups", "Channels", "Bots")

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    
    val filteredChats = chats.filter { 
        !it.isBlocked &&
        !it.isArchived &&
        (it.title.contains(searchQuery, ignoreCase = true) || 
        it.lastMessage.contains(searchQuery, ignoreCase = true)) &&
        when (selectedTabIndex) {
            1 -> !it.isGroup && !it.isChannel && !it.isBot // Personal
            2 -> it.isGroup
            3 -> it.isChannel
            4 -> it.isBot
            else -> true // All
        }
    }

    val globalSearchChats = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val localIds = chats.map { it.id }.toSet()
            listOf(
                Chat("g1", "OpenAI Devs", isGroup = true, lastMessage = "", unreadCount = 0),
                Chat("g2", "Android Kotlin", isGroup = true, lastMessage = "", unreadCount = 0),
                Chat("c5", "Android News", isChannel = true, lastMessage = "", unreadCount = 0),
                Chat("u10", "@durov", isBot = false, isGroup = false, lastMessage = "", unreadCount = 0),
                Chat("u11", "Alice Hacker", isBot = false, isGroup = false, lastMessage = "", unreadCount = 0)
            ).filter { 
                it.title.contains(searchQuery, true) && !localIds.contains(it.id)
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        StoriesPanel(onStorySwipe = { onStoryExpandedChange(it) }, onAvatarClick = { story -> 
            viewModel.addBot(Chat(story.id, story.author, isGroup = false, isChannel = false, isBot = false, lastMessage = ""))
            navController.navigate("profile/${story.id}") 
        })
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search chats...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )

        }

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            edgePadding = 16.dp,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f)) },
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.syncMessages() },
            state = pullRefreshState,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {

            if (searchQuery.isBlank() && selectedTabIndex == 0) {
                item {
                    val archivedCount = chats.count { it.isArchived && !it.isBlocked }
                    if (archivedCount > 0) {
                        ListItem(
                            headlineContent = { Text("Archived Chats", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("$archivedCount chat${if(archivedCount>1)"s" else ""}") },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.clickable { navController.navigate("archived_chats") }
                        )
                    }
                }
            }



            items(filteredChats, key = { it.id }) { chat ->
                SwipeableChatListItem(
                    chat = chat, 
                    isTyping = typingChats.contains(chat.id),
                    viewModel = viewModel,
                    onClick = { navController.navigate("chat/${chat.id}") },
                    onAvatarClick = { navController.navigate("profile/${chat.id}") }
                )
            }

            if (globalSearchChats.isNotEmpty()) {
                item {
                    Text(
                        "Global Search Results",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                }
                items(globalSearchChats) { chat ->
                    ChatListItem(chat = chat, isTyping = typingChats.contains(chat.id), onClick = { 
                        viewModel.addBot(chat)
                        navController.navigate("chat/${chat.id}") 
                    }, onAvatarClick = { 
                        viewModel.addBot(chat)
                        navController.navigate("profile/${chat.id}") 
                    })
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableChatListItem(chat: Chat, isTyping: Boolean = false, viewModel: AppViewModel, onClick: () -> Unit, onAvatarClick: () -> Unit = {}) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                viewModel.toggleArchive(chat.id, !chat.isArchived)
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.Settled -> Color.Transparent
                else -> if (chat.isArchived) Color(0xFF4CAF50) else Color(0xFFF44336)
            }
            val icon = if (chat.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                    Icon(icon, contentDescription = "Archive", tint = Color.White)
                }
            }
        },
        content = {
            ChatListItem(chat = chat, isTyping = isTyping, onClick = onClick, onAvatarClick = onAvatarClick)
        }
    )
}

@Composable
fun ChatListItem(chat: Chat, isTyping: Boolean = false, onClick: () -> Unit, onAvatarClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), CircleShape)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data("https://picsum.photos/seed/${chat.id}/100")
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Picture",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
            // Fallback letter if image is loading or fails (AsyncImage handles this nicely, but we can just put text behind it)
            val letter = chat.title.take(1).uppercase()
            Text(letter, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.align(Alignment.Center).zIndex(-1f))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.isSecret) {
                    Icon(Icons.Filled.Lock, contentDescription = "Secret Chat", modifier = Modifier.size(16.dp), tint = Color(0xFF4CAF50))
                    Spacer(Modifier.width(4.dp))
                } else if (chat.isChannel) {
                    Icon(Icons.Filled.Campaign, contentDescription = "Channel", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                } else if (chat.isGroup) {
                    Icon(Icons.Filled.Groups, contentDescription = "Group", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(4.dp))
                } else if (chat.isBot) {
                    Icon(Icons.Filled.SmartToy, contentDescription = "Bot", modifier = Modifier.size(16.dp), tint = Color(0xFF00D4FF))
                    Spacer(Modifier.width(4.dp))
                }
                Text(chat.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            if (isTyping) {
                Text(
                    text = "typing...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            } else {
                Text(
                    text = chat.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        if (chat.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(chat.unreadCount.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val theme by viewModel.theme.collectAsState()
    val isAutoThemeEnabled by viewModel.isAutoThemeEnabled.collectAsState()
    val customPrimary by viewModel.customPrimaryColor.collectAsState()
    val customSecondary by viewModel.customSecondaryColor.collectAsState()
    val batterySaverEnabled by viewModel.batterySaverEnabled.collectAsState()
    val themeOpacity by viewModel.themeOpacity.collectAsState()
    val favoriteThemes by viewModel.favoriteThemes.collectAsState()
    
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = LocalContext.current
    
    val activeAccount = LocalActiveAccount.current ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        // 2FA Setting
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.7f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Two-Factor Authentication", style = MaterialTheme.typography.titleMedium)
                    Text("Secure your account with 2FA", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = activeAccount.is2FAEnabled,
                    onCheckedChange = { viewModel.toggle2FA(activeAccount.id, activeAccount.is2FAEnabled) }
                )
            }
        }

        // Push Notifications Setting
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.7f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Push Notifications", style = MaterialTheme.typography.titleMedium)
                    Text("Real-time message alerts", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                var notificationsEnabled by remember { mutableStateOf(true) }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )
            }
        }

        // Auto Theme Seting
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.7f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto Theme Switcher", style = MaterialTheme.typography.titleMedium)
                    Text("Switch between Cherry Blossom (Day) and Neon Moon (Night)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isAutoThemeEnabled,
                    onCheckedChange = { viewModel.setAutoThemeEnabled(it) }
                )
            }
        }

        // Battery Saver Setting
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.7f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Battery Saver", style = MaterialTheme.typography.titleMedium)
                    Text("Reduce background animation frame rates", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = batterySaverEnabled,
                    onCheckedChange = { viewModel.setBatterySaverEnabled(it) }
                )
            }
        }

        // Theme Opacity
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.7f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Theme Opacity / Dimness", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = themeOpacity,
                    onValueChange = { viewModel.setThemeOpacity(it) },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Primary Accent Color
        Spacer(Modifier.height(24.dp))
        Text("Custom UI Colors", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
             val defaultColors = listOf(Color(0xFFFF007F), Color(0xFF00FFFF), Color(0xFF39FF14), Color(0xFFFF00FF), Color(0xFFB500FF), Color(0xFFE4E1E6))
             
             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                 Text("Primary", style = MaterialTheme.typography.bodySmall)
                 LazyRow {
                    items(defaultColors) { color ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(2.dp, if (customPrimary?.toULong() == color.value) Color.White else Color.Transparent, CircleShape)
                                .clickable { 
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    viewModel.setCustomPrimaryColor(color.value.toLong()) 
                                }
                        )
                    }
                 }
             }
        }

        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
             val defaultColors = listOf(Color(0xFF00E5FF), Color(0xFFFFEA00), Color(0xFFFF2A2A), Color(0xFF9000FF), Color(0xFF20202F))
             
             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                 Text("Secondary", style = MaterialTheme.typography.bodySmall)
                 LazyRow {
                    items(defaultColors) { color ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(2.dp, if (customSecondary?.toULong() == color.value) Color.White else Color.Transparent, CircleShape)
                                .clickable { 
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    viewModel.setCustomSecondaryColor(color.value.toLong()) 
                                }
                        )
                    }
                 }
             }
        }

        Spacer(Modifier.height(24.dp))
        Text("Neon Background Themes", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        val themes = listOf(
            AppTheme.NEON_SNOWFLAKES to "Snowflakes",
            AppTheme.NEON_CHERRY_BLOSSOM to "Cherry Blossom",
            AppTheme.NEON_CONFETTI to "Confetti",
            AppTheme.NEON_MOON to "Moon Sky",
            AppTheme.NEON_ROOM_FOG to "Yellow Fog",
            AppTheme.DEFAULT to "Default"
        )
        
        val sortedThemes = themes.sortedByDescending { favoriteThemes.contains(it.first.name) }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
        ) {
            items(sortedThemes) { (appTheme, name) ->
                val isFavorite = favoriteThemes.contains(appTheme.name)
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { 
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.setAutoThemeEnabled(false) // Disable auto switch if picked manually
                        viewModel.switchTheme(appTheme) 
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp, 160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                2.dp, 
                                if (theme == appTheme) MaterialTheme.colorScheme.primary else Color.Transparent, 
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        // thumbnail preview
                        when (appTheme) {
                            AppTheme.NEON_SNOWFLAKES -> NeonSnowflakesBackground(isStatic = true)
                            AppTheme.NEON_CHERRY_BLOSSOM -> NeonCherryBlossomBackground(isStatic = true)
                            AppTheme.NEON_CONFETTI -> NeonConfettiBackground(isStatic = true)
                            AppTheme.NEON_MOON -> NeonMoonBackground()
                            AppTheme.NEON_ROOM_FOG -> NeonRoomFogBackground()
                            AppTheme.DEFAULT -> ElegantDarkBackground()
                        }
                        
                        // Favorite toggle
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color.Yellow else Color.White.copy(alpha=0.6f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .clickable { viewModel.toggleFavoriteTheme(appTheme.name) }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (theme == appTheme) FontWeight.Bold else FontWeight.Normal,
                        color = if (theme == appTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        var showImportDialog by remember { mutableStateOf(false) }

        if (showImportDialog) {
            var importCode by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Import Theme") },
                text = {
                    Column {
                        Text("Paste a theme code to apply it.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = importCode,
                            onValueChange = { importCode = it },
                            label = { Text("Neon Messenger Theme Code") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { 
                        viewModel.importTheme(importCode)
                        showImportDialog = false 
                    }) {
                        Text("Import")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { 
                    viewModel.resetTheme() 
                }
            ) {
                Text("Reset to Default", color = MaterialTheme.colorScheme.error)
            }
            
            Row {
                IconButton(onClick = { showImportDialog = true }) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Download, contentDescription = "Import Theme")
                }
                Button(
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, "Neon Messenger Theme Code: ${theme.name}-${customPrimary ?: "def"}-${customSecondary ?: "def"}")
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Export Theme Layout"))
                    }
                ) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Share, contentDescription = "Export")
                    Spacer(Modifier.width(8.dp))
                    Text("Export Theme")
                }
            }
        }
        
        Spacer(Modifier.height(48.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupAdminScreen(viewModel: AppViewModel, chatId: String, navController: NavController) {
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val chat = chats.find { it.id == chatId } ?: return
    
    val members by viewModel.getGroupMembers(chatId).collectAsState(initial = emptyList())
    var groupName by remember { mutableStateOf(chat.title) }

    var selectedBotMember by remember { mutableStateOf<GroupMember?>(null) }
    
    if (selectedBotMember != null) {
        var canRead by remember { mutableStateOf(selectedBotMember!!.canReadMessages) }
        var canSend by remember { mutableStateOf(selectedBotMember!!.canSendMessages) }
        
        AlertDialog(
            onDismissRequest = { selectedBotMember = null },
            title = { Text("Bot Permissions") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canRead, onCheckedChange = { canRead = it })
                        Text("Can Read Messages")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canSend, onCheckedChange = { canSend = it })
                        Text("Can Send Messages")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateBotPermissions(chatId, selectedBotMember!!.userId, canRead, canSend)
                    selectedBotMember = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { selectedBotMember = null }) { Text("Cancel") }
            }
        )
    }

    var showAddBotDialog by remember { mutableStateOf(false) }
    if (showAddBotDialog) {
        val availableBots = BotRegistry.getAllBots()
        AlertDialog(
            onDismissRequest = { showAddBotDialog = false },
            title = { Text("Add Bot") },
            text = {
                LazyColumn {
                    items(availableBots) { bot ->
                        ListItem(
                            headlineContent = { Text(bot.name) },
                            modifier = Modifier.clickable {
                                viewModel.addGroupMember(chatId, bot.id, bot.name, isAdmin = false)
                                showAddBotDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAddBotDialog = false }) { Text("Close") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Admin: ${chat.title}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .padding(16.dp)
        ) {
            Text("Group Configuration", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { /* Update group settings in db */ }, modifier = Modifier.align(Alignment.End)) {
                Text("Save Settings")
            }
            
            Spacer(Modifier.height(24.dp))
            Text("Group Members", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                showAddBotDialog = true
            }, modifier = Modifier.align(Alignment.End)) {
                Text("Add Bot to Group")
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn {
                items(members) { member ->
                    val isBot = member.userId.startsWith("b") || BotRegistry.getBot(member.userId) != null
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(member.userName, style = MaterialTheme.typography.bodyLarge)
                                if (member.isAdmin) {
                                    Text("Admin", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                if (isBot) {
                                    Text("Bot", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                            if (isBot) {
                                TextButton(onClick = { selectedBotMember = member }) {
                                    Text("Permissions")
                                }
                            } else {
                                TextButton(onClick = { viewModel.updateAdminStatus(chatId, member.userId, !member.isAdmin) }) {
                                    Text(if (member.isAdmin) "Revoke Admin" else "Make Admin")
                                }
                            }
                            TextButton(onClick = { viewModel.removeGroupMember(chatId, member.userId) }) {
                                Text("Kick", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun ContactsScreen(viewModel: AppViewModel, navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var sortByName by remember { mutableStateOf(true) }

    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showCreateChannelDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val dbContacts by viewModel.contacts.collectAsState()
    
    val permissionState = com.google.accompanist.permissions.rememberPermissionState(
        android.Manifest.permission.READ_CONTACTS
    )

    LaunchedEffect(permissionState.status) {
        if (permissionState.status.isGranted) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val contacts = mutableListOf<com.example.ui.Contact>()
                val cursor = context.contentResolver.query(
                    android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null,
                    null,
                    null
                )
                cursor?.use {
                    val nameIndex = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (it.moveToNext()) {
                        val name = it.getString(nameIndex) ?: ""
                        val number = it.getString(numberIndex) ?: ""
                        if (name.isNotEmpty()) {
                            contacts.add(com.example.ui.Contact(id = java.util.UUID.randomUUID().toString(), name = name, phoneNumber = number))
                        }
                    }
                }
                val uniqueContacts = contacts.distinctBy { it.name }
                viewModel.syncContacts(uniqueContacts)
            }
        } else if (!permissionState.status.shouldShowRationale) {
            permissionState.launchPermissionRequest()
        }
    }

    val filteredContacts = dbContacts.filter { 
        it.name.contains(searchQuery, ignoreCase = true) && it.isRegistered
    }

    val sortedContacts = if (sortByName) {
        filteredContacts.sortedBy { it.name }
    } else {
        filteredContacts.sortedByDescending { it.name.hashCode() } // Pseudo-random time sort for demo
    }

    val groupedContacts = if (sortByName) sortedContacts.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' } else emptyMap()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новое сообщение") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { sortByName = !sortByName }) {
                        Icon(Icons.Filled.Sort, contentDescription = "Sort")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Add contact placeholder */ },
                containerColor = Color(0xFF3B82F6),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.PersonAdd, "Add Contact")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск контактов") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )
            
            Spacer(Modifier.height(8.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreateGroupDialog = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(androidx.compose.material3.MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Group, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.width(16.dp))
                    Text("Создать группу", style = MaterialTheme.typography.titleMedium)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreateChannelDialog = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(androidx.compose.material3.MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Campaign, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.width(16.dp))
                    Text("Создать канал", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.fillMaxSize()) {
                    val sortText = if (sortByName) "Сортировка по имени" else "Сортировка по времени захода"
                    Text(
                        text = sortText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                    
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (sortByName) {
                            groupedContacts.forEach { (letter, contactsList) ->
                                itemsIndexed(contactsList) { index: Int, contact: com.example.ui.Contact ->
                                    ContactRowItem(
                                        contact = contact,
                                        showLetter = index == 0,
                                        letter = letter.toString(),
                                        onClick = { navController.navigate("chat/${contact.id}") }
                                    )
                                }
                            }
                        } else {
                            items(sortedContacts) { contact ->
                                ContactRowItem(
                                    contact = contact,
                                    showLetter = false,
                                    letter = "",
                                    onClick = { navController.navigate("chat/${contact.id}") }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
    
    if (showCreateGroupDialog) {
        CreateChatDialog(
            isGroup = true,
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, desc, photo, isPrivate, linkOrUsername ->
                viewModel.createChat(name, desc, photo, isPrivate, linkOrUsername, true, false)
                showCreateGroupDialog = false
                navController.popBackStack()
            }
        )
    }
    if (showCreateChannelDialog) {
        CreateChatDialog(
            isGroup = false,
            onDismiss = { showCreateChannelDialog = false },
            onCreate = { name, desc, photo, isPrivate, linkOrUsername ->
                viewModel.createChat(name, desc, photo, isPrivate, linkOrUsername, false, true)
                showCreateChannelDialog = false
                navController.popBackStack()
            }
        )
    }
}

@Composable
fun ContactRowItem(contact: com.example.ui.Contact, showLetter: Boolean, letter: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLetter) {
            Text(
                text = letter,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else {
            Spacer(modifier = Modifier.width(32.dp))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        val color = remember(contact.id) { 
            listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5), Color(0xFF4CAF50), Color(0xFFFF9800)).random() 
        }
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.take(1).uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            val isRecently = remember(contact.id) { Math.random() > 0.5 }
            Text(
                text = if (isRecently) "был(а) недавно" else "был(а) давно",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CreateChatDialog(
    isGroup: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, photoUri: String, isPrivate: Boolean, usernameOrLink: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var generatedLink by remember { mutableStateOf("https://t.me/joinchat/${java.util.UUID.randomUUID().toString().take(8)}") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isGroup) "Create Group" else "Create Channel") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    if (photoUri.isEmpty()) {
                        IconButton(
                            onClick = { photoUri = "https://picsum.photos/seed/${java.util.UUID.randomUUID()}/200" },
                            modifier = Modifier.size(80.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = "Select Photo", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    } else {
                        coil.compose.AsyncImage(
                            model = photoUri,
                            contentDescription = "Selected Photo",
                            modifier = Modifier.size(80.dp).clip(CircleShape).clickable { photoUri = "https://picsum.photos/seed/${java.util.UUID.randomUUID()}/200" },
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    maxLines = 3
                )
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("Private ${if(isGroup) "Group" else "Channel"}", modifier = Modifier.weight(1f))
                    Switch(checked = isPrivate, onCheckedChange = { 
                        isPrivate = it
                        if (it) generatedLink = "https://t.me/joinchat/${java.util.UUID.randomUUID().toString().take(8)}"
                    })
                }
                
                if (isPrivate) {
                    OutlinedTextField(
                        value = generatedLink,
                        onValueChange = {},
                        label = { Text("Invite Link") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )
                    Text("People can only join via this link.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            if (it.length < 5 && it.isNotEmpty()) {
                                usernameError = "Username must be at least 5 characters"
                            } else if (!it.matches(Regex("^[a-zA-Z0-9_]+$")) && it.isNotEmpty()) {
                                usernameError = "Invalid characters"
                            } else if (it == "admin" || it == "system") {
                                usernameError = "Username is already taken"
                            } else {
                                usernameError = null
                            }
                        },
                        label = { Text("@username") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = usernameError != null,
                        supportingText = { usernameError?.let { Text(it) } }
                    )
                    Text("Public ${if(isGroup) "groups" else "channels"} can be found in search.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val linkOrUsername = if (isPrivate) generatedLink else username
                    onCreate(name, description, photoUri, isPrivate, linkOrUsername)
                    onDismiss()
                },
                enabled = name.isNotBlank() && (isPrivate || (username.isNotBlank() && usernameError == null))
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
@Composable
fun AccountDrawerContent(viewModel: AppViewModel, onCloseDrawer: () -> Unit, navController: NavController, onCreateGroupClick: () -> Unit, onCreateChannelClick: () -> Unit, onCreateSecretChatClick: () -> Unit) {
    val accounts by viewModel.accounts.collectAsState()
    val activeAccount = LocalActiveAccount.current
    var isAccountsExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isAccountsExpanded = !isAccountsExpanded }
                .padding(16.dp)
                .padding(top = 24.dp)
        ) {
            AnimatedContent(
                targetState = activeAccount,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400)) { height -> height })
                        .togetherWith(fadeOut(animationSpec = tween(400)) + slideOutVertically(animationSpec = tween(400)) { height -> -height })
                },
                label = "account_header_transition"
            ) { account ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(account?.profilePicUrl ?: "")
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(account?.displayName ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(account?.username ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 20.dp)) {
                Icon(
                    if (isAccountsExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Expand Accounts"
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                AnimatedVisibility(
                    visible = isAccountsExpanded,
                    enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                ) {
                    Column {
                        accounts.forEach { account ->
                            NavigationDrawerItem(
                                label = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(
                                            model = account.profilePicUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp).clip(CircleShape).background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(account.displayName, fontWeight = if(account.isActive) FontWeight.Bold else FontWeight.Normal)
                                    }
                                },
                                selected = account.isActive,
                                onClick = {
                                    viewModel.switchAccount(account.id)
                                    isAccountsExpanded = false
                                    scope.launch {
                                        kotlinx.coroutines.delay(400)
                                        onCloseDrawer()
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        if (accounts.size < 5) {
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Filled.Add, "Add Account") },
                                label = { Text("Add Account") },
                                selected = false,
                                onClick = { 
                                    viewModel.startAddAccount()
                                    onCloseDrawer() 
                                }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    }
                }
            }

            item {
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Person, "Profile") },
                    label = { Text("Profile") },
                    selected = false,
                    onClick = { 
                        navController.navigate("settings/profile")
                        onCloseDrawer() 
                    }
                )
            }
            item {
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.SmartToy, "Discover Bots") },
                    label = { Text("Discover Bots") },
                    selected = false,
                    onClick = { 
                        navController.navigate("discover_bots")
                        onCloseDrawer() 
                    }
                )
            }
            item {
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Group, "New Group") },
                    label = { Text("New Group") },
                    selected = false,
                    onClick = { onCreateGroupClick(); onCloseDrawer() }
                )
            }
            item {
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.PersonOutline, "Contacts") },
                    label = { Text("Contacts") },
                    selected = false,
                    onClick = { navController.navigate("contacts"); onCloseDrawer() }
                )
            }
            item {
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Call, "Calls") },
                    label = { Text("Calls") },
                    selected = false,
                    onClick = { navController.navigate("calls"); onCloseDrawer() }
                )
            }
            item {
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Bookmark, "Saved Messages") },
                    label = { Text("Saved Messages") },
                    selected = false,
                    onClick = { onCloseDrawer() }
                )
            }
            item {
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Settings, "Settings") },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = { 
                        navController.navigate("settings")
                        onCloseDrawer() 
                    }
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)) }
            item {
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.PersonAdd, "Invite Friends") },
                    label = { Text("Invite Friends") },
                    selected = false,
                    onClick = { onCloseDrawer() }
                )
            }
            item {
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Help, "Neon Messenger Features") },
                    label = { Text("Neon Messenger Features") },
                    selected = false,
                    onClick = { onCloseDrawer() }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedChatsScreen(viewModel: AppViewModel, navController: NavController) {
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val typingChats by viewModel.typingChats.collectAsState()
    val archivedChats = chats.filter { it.isArchived && !it.isBlocked }

    var showMenu by remember { mutableStateOf(false) }
    var showFAQ by remember { mutableStateOf(false) }

    if (showFAQ) {
        AlertDialog(
            onDismissRequest = { showFAQ = false },
            title = { Text("How does the Archive work?") },
            text = { 
                Text("Archived chats stay hidden from the main list. If a new message arrives in an archived chat with notifications enabled, it will be unarchived. If notifications are disabled, it remains in the archive.") 
            },
            confirmButton = {
                TextButton(onClick = { showFAQ = false }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archived Chats") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Settings")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Archive Settings") },
                                onClick = { 
                                    showMenu = false
                                    navController.navigate("archive_settings")
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Settings, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("FAQ") },
                                onClick = { 
                                    showMenu = false
                                    showFAQ = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Help, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp)
        ) {
            items(archivedChats, key = { it.id }) { chat ->
                SwipeableChatListItem(
                    chat = chat, 
                    isTyping = typingChats.contains(chat.id),
                    viewModel = viewModel,
                    onClick = { navController.navigate("chat/${chat.id}") },
                    onAvatarClick = { navController.navigate("profile/${chat.id}") }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveSettingsScreen(navController: NavController) {
    var alwaysKeepInArchive by remember { mutableStateOf(false) }
    var autoArchive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archive Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Chats with enabled notifications",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Always keep in archive", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = alwaysKeepInArchive,
                        onCheckedChange = { alwaysKeepInArchive = it }
                    )
                }
                Text(
                    "Keep chats in the archive, even if they have notifications enabled and a new message arrives.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )
                
                Text(
                    "New chats with strangers",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Automatically archive", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = autoArchive,
                        onCheckedChange = { autoArchive = it }
                    )
                }
                Text(
                    "Automatically mute new chats, groups, and channels from non-contacts and move them to the archive.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
