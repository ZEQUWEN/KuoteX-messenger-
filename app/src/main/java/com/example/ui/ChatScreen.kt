package com.example.ui
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.util.Date
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(viewModel: AppViewModel, chatId: String, navController: NavController) {
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val chat = chats.find { it.id == chatId } ?: Chat(
        id = chatId,
        title = if (chatId.toIntOrNull() != null) "User $chatId" else chatId,
        lastMessage = "",
        isGroup = false,
        isChannel = false,
        isBot = false
    )
    
        val messages by viewModel.getMessages(chatId).collectAsStateWithLifecycle(initialValue = emptyList())
    
    DisposableEffect(chatId) {
        viewModel.setActiveChat(chatId)
        onDispose {
            viewModel.setActiveChat(null)
        }
    }

    val groupMembers by if (chat.isGroup) viewModel.getGroupMembers(chatId).collectAsState(initial = emptyList()) else remember { mutableStateOf(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchSender by remember { mutableStateOf("") }
    var searchMediaOnly by remember { mutableStateOf(false) }
    var showSearchFilters by remember { mutableStateOf(false) }
    var searchStartDate by remember { mutableStateOf<Long?>(null) }
    var searchEndDate by remember { mutableStateOf<Long?>(null) }
    
    val filteredMessages = if (!isSearchMode) messages else messages.filter { msg ->
        val queryMatch = searchQuery.isBlank() || msg.text.contains(searchQuery, ignoreCase = true) || msg.senderId.contains(searchQuery, ignoreCase = true)
        val senderMatch = searchSender.isBlank() || msg.senderId.contains(searchSender, ignoreCase = true)
        val mediaMatch = if (searchMediaOnly) (msg.mediaPath != null || msg.audioPath != null || msg.documentData != null) else true
        val startDateMatch = if (searchStartDate != null) msg.timestamp >= searchStartDate!! else true
        val endDateMatch = if (searchEndDate != null) msg.timestamp <= searchEndDate!! else true
        queryMatch && senderMatch && mediaMatch && startDateMatch && endDateMatch
    }

    val activeAccount = LocalActiveAccount.current
    
    var showDisappearingDialog by remember { mutableStateOf(false) }
    var expiresIn by remember { mutableStateOf<Long?>(null) }
    
    var showReactionDialogFor by remember { mutableStateOf<String?>(null) }
    
    var isRecording by remember { mutableStateOf(false) }
    
    
    val userPresences by viewModel.userPresences.collectAsStateWithLifecycle()
    val presence = userPresences[chatId]

    val typingChats by viewModel.typingChats.collectAsState()
    val isTyping = typingChats.contains(chatId)
    
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedFileUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedFileSize by remember { mutableStateOf(0L) }
    var selectedFileMime by remember { mutableStateOf("") }
    var showFilePreviewDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        uri?.let {
            selectedFileUri = it
            var name = "Unknown File"
            var size = 0L
            var mime = context.contentResolver.getType(it) ?: "application/octet-stream"
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1) name = cursor.getString(nameIdx)
                    val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                }
            }
            if (size > 5L * 1024 * 1024 * 1024) { // 5 GB limit
                android.widget.Toast.makeText(context, "File exceeds 5 GB limit", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                selectedFileName = name
                selectedFileSize = size
                selectedFileMime = mime
                showFilePreviewDialog = true
            }
        }
    }
    val activity = remember(context) {
        var currentContext = context
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is android.app.Activity) {
                break
            }
            currentContext = currentContext.baseContext
        }
        currentContext as? android.app.Activity
    }
    
    DisposableEffect(chat.isSecret) {
        if (chat.isSecret) {
            activity?.window?.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        onDispose {
            if (chat.isSecret) {
                activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
    
    LaunchedEffect(chatId) {
        viewModel.markMessagesAsRead(chatId, activeAccount?.id ?: "")
        val draft = viewModel.getDraft(chatId)
        if (draft != null) {
            inputText = draft.text
        }
    }

    DisposableEffect(chatId) {
        onDispose {
            viewModel.saveDraft(chatId, inputText)
        }
    }
    
    var showAttachmentMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (isSearchMode) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            placeholder = { Text("Search messages...") },
                            singleLine = true
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { isSearchMode = false; searchQuery = "" }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close Search")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearchFilters = true }) {
                            Icon(Icons.Filled.FilterList, contentDescription = "Filters")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                )
            } else {
                TopAppBar(
                title = { 
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!chat.isGroup && !chat.isChannel) {
                                    navController.navigate("profile/${chat.id}")
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(chat.title)
                                                val isOnline = presence?.isOnline == true
                        val lastSeen = presence?.lastSeen ?: 0L
                        val statusText = when {
                            isTyping -> "typing..."
                            isOnline -> "Online"
                            lastSeen > 0 -> {
                                val diff = System.currentTimeMillis() - lastSeen
                                if (diff < 60_000) "last seen just now"
                                else if (diff < 3600_000) "last seen ${diff / 60_000} minutes ago"
                                else "last seen ${diff / 3600_000} hours ago"
                            }
                            else -> "last seen recently"
                        }
                        Text(
                            text = statusText, 
                            style = MaterialTheme.typography.labelSmall, 
                            color = if(isTyping) MaterialTheme.colorScheme.primary else if(isOnline) Color.Green else Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("call/${chat.id}?isVideo=false") }) {
                        Icon(Icons.Filled.Call, contentDescription = "Voice Call")
                    }
                    IconButton(onClick = { navController.navigate("call/${chat.id}?isVideo=true") }) {
                        Icon(Icons.Filled.VideoCall, contentDescription = "Video Call")
                    }
                    if (chat.isGroup) {
                        IconButton(onClick = { navController.navigate("group_admin/${chat.id}") }) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Admin Dashboard")
                        }
                    }
                    IconButton(onClick = { showDisappearingDialog = true }) {
                        Icon(Icons.Filled.Timer, contentDescription = "Disappearing Messages")
                    }
                    IconButton(onClick = { isSearchMode = true }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search Messages")
                    }
                    
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        if (!chat.isGroup && !chat.isChannel) {
                            DropdownMenuItem(
                                text = { Text("Delete Chat", color = MaterialTheme.colorScheme.error) },
                                onClick = { 
                                    expanded = false
                                    viewModel.deleteChat(chatId)
                                    navController.popBackStack()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear History") },
                                onClick = { 
                                    expanded = false
                                    viewModel.clearHistory(chatId)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Block User", color = MaterialTheme.colorScheme.error) },
                                onClick = { 
                                    expanded = false
                                    viewModel.blockUser(chatId)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Export History") },
                            onClick = { 
                                expanded = false
                                viewModel.exportMessageHistory(chatId)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                )
            )
            }
        },
        bottomBar = {
            if (chat.isBlocked) {
                Surface(modifier = Modifier.fillMaxWidth().padding(16.dp), color = Color.Transparent) {
                    Text(
                        "You blocked this user.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var attachmentMenuExpanded by remember { mutableStateOf(false) }
                    var botMenuExpanded by remember { mutableStateOf(false) }
                    
                    if (chat.isBot || chat.title.contains("BotFather", ignoreCase = true)) {
                        Box {
                            IconButton(onClick = { botMenuExpanded = true }) {
                                Icon(Icons.Filled.Terminal, contentDescription = "Bot Commands", tint = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(
                                expanded = botMenuExpanded,
                                onDismissRequest = { botMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("/start - start/refresh bot") },
                                    onClick = {
                                        botMenuExpanded = false
                                        viewModel.sendMessage(chatId, activeAccount?.id ?: "", "/start", null, expiresIn)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("/newbot - create a new bot") },
                                    onClick = {
                                        botMenuExpanded = false
                                        viewModel.sendMessage(chatId, activeAccount?.id ?: "", "/newbot", null, expiresIn)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("/description - bot description") },
                                    onClick = {
                                        botMenuExpanded = false
                                        viewModel.sendMessage(chatId, activeAccount?.id ?: "", "/description", null, expiresIn)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("/mybots - show a list of all bots") },
                                    onClick = {
                                        botMenuExpanded = false
                                        viewModel.sendMessage(chatId, activeAccount?.id ?: "", "/mybots", null, expiresIn)
                                    }
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(onClick = { attachmentMenuExpanded = true }) {
                            Icon(Icons.Filled.AttachFile, contentDescription = "Attach")
                        }
                        DropdownMenu(
                            expanded = attachmentMenuExpanded,
                            onDismissRequest = { attachmentMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Camera") },
                                onClick = { 
                                    attachmentMenuExpanded = false
                                    viewModel.sendMessage(chatId, activeAccount?.id ?: "", "📷 Sent a photo", null, expiresIn)
                                },
                                leadingIcon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("File") },
                                onClick = { 
                                    attachmentMenuExpanded = false
                                    filePickerLauncher.launch(arrayOf("*/*"))
                                },
                                leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Location") },
                                onClick = { 
                                    attachmentMenuExpanded = false
                                    viewModel.sendMessage(chatId, activeAccount?.id ?: "", "📍 Location: 37.4221° N, 122.0841° W", null, expiresIn)
                                },
                                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) }
                            )
                        }
                    }

                    var showEmojiPicker by remember { mutableStateOf(false) }
                    IconButton(onClick = { showEmojiPicker = !showEmojiPicker }) {
                        Text("😀", style = MaterialTheme.typography.titleLarge)
                    }
                    if (showEmojiPicker) {
                        DropdownMenu(
                            expanded = showEmojiPicker,
                            onDismissRequest = { showEmojiPicker = false }
                        ) {
                            val emojis = listOf("😀", "😂", "🥰", "😎", "🤔", "👍", "❤️", "🔥")
                            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                emojis.forEach { emoji ->
                                    Text(
                                        text = emoji,
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.clickable { 
                                            inputText += emoji
                                            showEmojiPicker = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(

                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Message...") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        )
                    )
                    if (inputText.isNotBlank()) {
                        IconButton(onClick = {
                            viewModel.sendMessage(chatId, activeAccount?.id ?: "", inputText, null, expiresIn)
                            inputText = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(
                            onClick = { 
                                isRecording = !isRecording
                                if (!isRecording) {
                                    // Simulate sending an audio message
                                    viewModel.sendMessage(chatId, activeAccount?.id ?: "", "🎤 Audio Message", "audio_path.mp3", expiresIn)
                                }
                            }
                        ) {
                            Icon(
                                if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic, 
                                contentDescription = "Record",
                                tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        

        if (showFilePreviewDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showFilePreviewDialog = false },
                title = { Text("Send File") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val icon = if (selectedFileMime.startsWith("image/")) Icons.Filled.Image
                            else if (selectedFileMime.startsWith("video/")) Icons.Filled.VideoFile
                            else if (selectedFileMime.startsWith("audio/")) Icons.Filled.AudioFile
                            else Icons.Filled.InsertDriveFile
                            
                        Icon(
                            icon,
                            contentDescription = "File Icon",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(selectedFileName, style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("Size: ${selectedFileSize / 1024} KB", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            label = { Text("Add a caption...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showFilePreviewDialog = false
                        val documentJson = org.json.JSONObject().apply {
                            put("uri", selectedFileUri.toString())
                            put("name", selectedFileName)
                            put("size", selectedFileSize)
                            put("mimeType", selectedFileMime)
                        }.toString()
                        viewModel.sendMessage(
                            chatId = chatId,
                            senderId = activeAccount?.id ?: "",
                            text = inputText, // caption
                            audioPath = null,
                            expiresIn = expiresIn,
                            documentData = documentJson
                        )
                        inputText = ""
                    }) {
                        Text("Send")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFilePreviewDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        Column(modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            if (!chat.isGroup && !chat.isChannel && !chat.isBot && !chat.isContact && !chat.isBlocked && !chat.isActionMenuDismissed) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { viewModel.addToContacts(chatId) }) {
                            Text("Add to Contacts")
                        }
                        TextButton(
                            onClick = { viewModel.blockUser(chatId) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Block User")
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { viewModel.dismissActionMenu(chatId) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                }
            }
            if (chat.pinnedMessageId != null) {
                val pinnedMessage = messages.find { it.id == chat.pinnedMessageId }
                if (pinnedMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.8f), 
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Pinned Message", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(pinnedMessage.text, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()
            val canScrollForward by remember { derivedStateOf { listState.canScrollForward } }
            
            // Auto scroll to bottom when new messages arrive if we were already at bottom
            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty() && !listState.canScrollForward) {
                    listState.scrollToItem(messages.size - 1)
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = false
                ) {
                    items(filteredMessages, key = { it.id }) { message ->
                        val visibleState = androidx.compose.runtime.remember { 
                            androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } 
                        }
                        androidx.compose.animation.AnimatedVisibility(
                            visibleState = visibleState,
                            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it / 2 }),
                            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { -it / 2 }),
                            modifier = Modifier.animateItem()
                        ) {
                        val isMe = message.senderId == activeAccount?.id
                        var senderName: String? = null
                        var senderStatus: String? = null
                        var isBot = false
                        if (!isMe) {
                            if (chat.isGroup) {
                                val member = groupMembers.find { it.userId == message.senderId }
                                senderName = member?.userName ?: message.senderId
                                isBot = message.senderId.startsWith("bot_")
                            } else {
                                if (chat.isBot) {
                                    senderName = chat.title
                                    isBot = true
                                } else {
                                    senderName = chat.title
                                }
                            }
                        } else {
                            senderName = activeAccount?.displayName
                            senderStatus = activeAccount?.customStatus
                        }
                        MessageBubble(
                            senderName = senderName,
                            senderStatus = senderStatus,
                            isBot = isBot,
                            message = message, 
                            isMe = isMe,
                            onLongClick = { showReactionDialogFor = message.id },
                            onButtonClick = { buttonText ->
                                if (buttonText.startsWith("Sandbox::")) {
                                    val botId = buttonText.substringAfter("::")
                                    navController.navigate("sandbox/$botId")
                                } else if (buttonText.startsWith("Dashboard::")) {
                                    val botId = buttonText.substringAfter("::")
                                    navController.navigate("dashboard/$botId")
                                } else {
                                    viewModel.sendMessage(chatId, activeAccount?.id ?: "", buttonText, null, expiresIn)
                                }
                            }
                        )
                        }
                    }
                }
                
                androidx.compose.animation.AnimatedVisibility(
                    visible = canScrollForward,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    enter = androidx.compose.animation.scaleIn(),
                    exit = androidx.compose.animation.scaleOut()
                ) {
                    FloatingActionButton(
                        onClick = { 
                            coroutineScope.launch { 
                                if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) 
                            } 
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Scroll to bottom")
                    }
                }
            }
        }
        
        
        if (showSearchFilters) {
            AlertDialog(
                onDismissRequest = { showSearchFilters = false },
                title = { Text("Search Filters") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = searchSender,
                            onValueChange = { searchSender = it },
                            label = { Text("Sender ID/Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = searchMediaOnly,
                                onCheckedChange = { searchMediaOnly = it }
                            )
                            Text("Has Media (Photos/Voice/Files)")
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { searchStartDate = if (searchStartDate == null) System.currentTimeMillis() - 86400000L * 7 else null }) {
                                Text(if (searchStartDate == null) "Start: 7 Days Ago" else "Clear Start")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { searchEndDate = if (searchEndDate == null) System.currentTimeMillis() else null }) {
                                Text(if (searchEndDate == null) "End: Now" else "Clear End")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSearchFilters = false }) { Text("Apply") }
                }
            )
        }
        
        if (showDisappearingDialog) {
            AlertDialog(
                onDismissRequest = { showDisappearingDialog = false },
                title = { Text("Disappearing Messages") },
                text = {
                    Column {
                        Text("Set a timer for new messages to disappear automatically.")
                        Spacer(Modifier.height(8.dp))
                        val options = listOf(
                            "10 Seconds" to 10_000L,
                            "1 Minute" to 60_000L,
                            "1 Hour" to 3_600_000L,
                            "1 Day" to 86_400_000L,
                            "1 Week" to 604_800_000L
                        )
                        options.forEach { (label, duration) ->
                            TextButton(onClick = { 
                                expiresIn = duration
                                showDisappearingDialog = false
                            }) { Text(label) }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { 
                        expiresIn = null
                        showDisappearingDialog = false
                    }) { Text("Off") }
                }
            )
        }
        
        if (showReactionDialogFor != null) {
            val selectedMessage = messages.find { it.id == showReactionDialogFor }
            AlertDialog(
                onDismissRequest = { showReactionDialogFor = null },
                title = { Text("Message Action") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                            val emojis = listOf("👍", "❤️", "😂", "😮", "😢", "🔥")
                            for (emoji in emojis) {
                                Text(
                                    text = emoji,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .combinedClickable(onClick = {
                                            viewModel.addReaction(showReactionDialogFor!!, emoji)
                                            showReactionDialogFor = null
                                        }),
                                    style = LocalTextStyle.current.copy(fontSize = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp))
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        OutlinedButton(
                            onClick = {
                                if (selectedMessage?.isPinned == true) {
                                    viewModel.unpinMessage(chatId, showReactionDialogFor!!)
                                } else {
                                    viewModel.pinMessage(chatId, showReactionDialogFor!!)
                                }
                                showReactionDialogFor = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (selectedMessage?.isPinned == true) "Unpin Message" else "Pin Message")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                selectedMessage?.let {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(it.text))
                                }
                                showReactionDialogFor = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Copy Text")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.deleteMessage(showReactionDialogFor!!)
                                showReactionDialogFor = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete Message", color = MaterialTheme.colorScheme.error)
                        }

                        if (!chat.isSecret) {
                            OutlinedButton(
                                onClick = {
                                    // Forward functionality - essentially just copies it to the draft of another chat or opens a picker
                                    // For simplicity, let's just insert it to input text and close dialog
                                    if (selectedMessage != null) {
                                        inputText = "Fwd: ${selectedMessage.text}"
                                    }
                                    showReactionDialogFor = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Forward Quote")
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MessageBubble(message: Message, isMe: Boolean, senderName: String? = null, senderStatus: String? = null, isBot: Boolean = false, onLongClick: () -> Unit, onButtonClick: ((String) -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (senderName != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 2.dp, start = if(isMe) 0.dp else 8.dp, end = if(isMe) 8.dp else 0.dp)) {
                if (isBot) {
                    Icon(Icons.Filled.SmartToy, contentDescription = "Bot", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                }
                Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                    Text(senderName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    if (senderStatus != null && senderStatus.isNotEmpty()) {
                        Text(senderStatus, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .background(
                    if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(
                        topStart = 16.dp, 
                        topEnd = 16.dp, 
                        bottomStart = if (isMe) 16.dp else 0.dp, 
                        bottomEnd = if (isMe) 0.dp else 16.dp
                    )
                )
                .combinedClickable(
                    onClick = { /* play audio if applicable */ },
                    onLongClick = onLongClick
                )
                .padding(12.dp)
        ) {
            Column {
                if (message.documentData != null) {
                    val documentData = message.documentData
                    val parsedData = androidx.compose.runtime.remember(documentData) {
                        var name = "File"
                        var size = 0L
                        var mime = ""
                        try {
                            val json = org.json.JSONObject(documentData)
                            name = json.optString("name", "File")
                            size = json.optLong("size", 0L)
                            mime = json.optString("mimeType", "")
                        } catch(e: Exception) {}
                        Triple(name, size, mime)
                    }
                    val (name, size, mime) = parsedData

                    
                    val icon = if (mime.startsWith("image/")) Icons.Filled.Image
                        else if (mime.startsWith("video/")) Icons.Filled.VideoFile
                        else if (mime.startsWith("audio/")) Icons.Filled.AudioFile
                        else Icons.AutoMirrored.Filled.InsertDriveFile

                    var showDownloadDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                    if (showDownloadDialog) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showDownloadDialog = false },
                            title = { Text("File Preview") },
                            text = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(16.dp))
                                    Text(name, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    Text("${size / 1024} KB", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(16.dp))
                                    Text("Open or download this file to your device?", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showDownloadDialog = false }) { Text("Download") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDownloadDialog = false }) { Text("Cancel") }
                            }
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .clickable { showDownloadDialog = true }
                                .padding(8.dp)
                        ) {
                            Icon(icon, contentDescription = "File", tint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(name, color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${size / 1024} KB", color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (message.text.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (message.isE2EEncrypted) {
                                    Icon(Icons.Filled.Lock, contentDescription = "Encrypted", modifier = Modifier.size(12.dp), tint = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha=0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f))
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(message.text, color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                } else if (message.audioPath != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play Audio", tint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text(message.text, color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (message.isE2EEncrypted) {
                            Icon(Icons.Filled.Lock, contentDescription = "Encrypted", modifier = Modifier.size(12.dp), tint = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha=0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(message.text, color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
                
                if (message.reaction != null) {
                    Box(modifier = Modifier.offset(y = 8.dp).background(Color.DarkGray, CircleShape).padding(4.dp)) {
                        Text(message.reaction)
                    }
                }
                
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.End)) {
                    Text(
                        formatTime(message.timestamp), 
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), 
                        color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha=0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f)
                    )
                    if (message.expiresAt != null) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.Timer, contentDescription = "Disappearing", modifier = Modifier.size(12.dp), tint = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha=0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f))
                    }
                    if (isMe) {
                        Spacer(Modifier.width(4.dp))
                        if (message.isRead) {
                            Icon(Icons.Filled.DoneAll, contentDescription = "Read", modifier = Modifier.size(14.dp), tint = Color(0xFF4CAF50))
                        } else if (message.isDelivered) {
                            Icon(Icons.Filled.DoneAll, contentDescription = "Delivered", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary.copy(alpha=0.7f))
                        } else {
                            Icon(Icons.Filled.Check, contentDescription = "Sent", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary.copy(alpha=0.7f))
                        }
                    }
                }
            }
        }
        
        if (!message.buttonsData.isNullOrEmpty()) {
            Spacer(Modifier.height(4.dp))
            val buttons = message.buttonsData.split("||")
            FlowRow(
                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
            ) {
                buttons.forEach { buttonText ->
                    OutlinedButton(
                        onClick = { onButtonClick?.invoke(buttonText) },
                        modifier = Modifier.padding(end = 4.dp, bottom = 4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        val display = if (buttonText.contains("::")) buttonText.substringBefore("::") else buttonText
                        Text(display, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}


private val timeFormatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
fun formatTime(timestamp: Long): String {
    val date = Date(timestamp)
    return timeFormatter.format(date)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: AppViewModel, chatId: String, navController: NavController) {
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val chat = chats.find { it.id == chatId } ?: Chat(
        id = chatId,
        title = "User $chatId",
        lastMessage = "",
        isGroup = false,
        isChannel = false,
        isBot = false
    )
        
    
    val userPresences by viewModel.userPresences.collectAsStateWithLifecycle()
    val presence = userPresences[chatId]

    
    var showAvatarViewer by remember { mutableStateOf(false) }
    val avatars = remember(chatId) {
        listOf(
            "https://picsum.photos/seed/${chatId}/400",
            "https://picsum.photos/seed/${chatId}_1/400",
            "https://picsum.photos/seed/${chatId}_2/400"
        )
    }

    if (showAvatarViewer) {
        AvatarViewerDialog(
            avatars = avatars,
            initialPage = 0,
            onDismiss = { showAvatarViewer = false }
        )
    }
    
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* more options */ }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showAvatarViewer = true },
                    contentAlignment = Alignment.Center
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    coil.compose.AsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(avatars.first())
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(chat.title, style = MaterialTheme.typography.headlineMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                
                val chatUsername = if (chat.title.startsWith("@")) chat.title else "@" + chat.title.lowercase().replace(" ", "_")
                Text(chatUsername, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                
                val isOnline = presence?.isOnline == true
                val lastSeen = presence?.lastSeen ?: 0L

                val statusText = if (isOnline) {
                    "online"
                } else if (lastSeen > 0) {
                    val diff = System.currentTimeMillis() - lastSeen
                    if (diff < 60_000) "last seen just now"
                    else if (diff < 3600_000) "last seen ${diff / 60_000} minutes ago"
                    else "last seen ${diff / 3600_000} hours ago"
                } else {
                    "last seen recently"
                }
                
                val statusColor = if (isOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant

                
                Text(statusText, style = MaterialTheme.typography.bodyMedium, color = statusColor)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        // If it's a mock chat, you might want to create it, but for UI purpose just navigate

                        navController.navigate("chat/${chatId}")
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Message, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Написать сообщение")
                }
            }
        }
    }
}
