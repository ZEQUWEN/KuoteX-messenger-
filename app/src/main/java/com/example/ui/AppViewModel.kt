package com.example.ui
import kotlinx.coroutines.delay

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.InboundEvent
import androidx.compose.ui.graphics.Color
import com.example.crypto.SignalProtocolManager
import com.example.data.MessengerRepository

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import com.example.utils.MessageSanitizer
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

enum class AppTheme {
    DEFAULT,
    NEON_SNOWFLAKES,
    NEON_CHERRY_BLOSSOM,
    NEON_CONFETTI,
    NEON_MOON,
    NEON_ROOM_FOG
}

@Entity(tableName = "accounts")
data class UserAccount(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val profilePicUrl: String, // Or gif URL
    val is2FAEnabled: Boolean = false,
    val isActive: Boolean = false,
    val bio: String = "",
    val sessionToken: String? = null,
    val customStatus: String = "",
    val encryptedPasscode: String? = null,
    val phoneNumber: String = ""
)

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String?,
    val isRegistered: Boolean = true
)

@Entity(    tableName = "messages",
    indices = [androidx.room.Index("chatId"), androidx.room.Index("senderId")]
)
data class Message(
    @PrimaryKey val id: String,
    val chatId: String = "",
    val senderId: String,
    val text: String,
    val audioPath: String? = null,
    val isE2EEncrypted: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val reaction: String? = null,
    val expiresAt: Long? = null,
    val isDelivered: Boolean = false,
    val isRead: Boolean = false,
    val isPinned: Boolean = false,
    val mediaPath: String? = null,
    val mediaType: String? = null,
    val documentData: String? = null,
    val locationData: String? = null,
    val buttonsData: String? = null
)

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey val id: String,
    val title: String,
    val isChannel: Boolean = false,
    val isGroup: Boolean = false,
    val isBot: Boolean = false,
    val isSecret: Boolean = false,
    val lastMessage: String,
    val lastMessageTimestamp: Long = 0L,
    val lastMessageSenderName: String? = null,
    val unreadCount: Int = 0,
    val pinnedMessageId: String? = null,
    val isContact: Boolean = false,
    val isBlocked: Boolean = false,
    val isActionMenuDismissed: Boolean = false,
    val isArchived: Boolean = false
)

@Entity(
    tableName = "group_members",
    primaryKeys = ["chatId", "userId"],
    indices = [androidx.room.Index("chatId"), androidx.room.Index("userId")]
)
data class GroupMember(
    val chatId: String,
    val userId: String,
    val userName: String,
    val isAdmin: Boolean = false,
    val canReadMessages: Boolean = true,
    val canSendMessages: Boolean = true
)

@Entity(
    tableName = "drafts",
    indices = [androidx.room.Index("chatId")]
)
data class Draft(
    @PrimaryKey val chatId: String,
    val text: String
)

enum class ConnectionStatus {
    ONLINE, OFFLINE, CONNECTING
}


data class UserPresence(
    val userId: String,
    val isOnline: Boolean,
    val lastSeen: Long
)

class AppViewModel(private val repository: MessengerRepository, val userPrefs: com.example.data.UserPreferencesRepository) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class)
    val debouncedSearchQuery = _searchQuery
        .debounce(300L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val signalProtocolManager = SignalProtocolManager()

    private val _theme = MutableStateFlow(
        run {
            val savedTheme = repository.getTheme()
            if (savedTheme != null) {
                try {
                    AppTheme.valueOf(savedTheme)
                } catch (e: Exception) {
                    AppTheme.DEFAULT
                }
            } else {
                AppTheme.DEFAULT
            }
        }
    )
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    val isDarkThemeEnabled: StateFlow<Boolean> = userPrefs.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _isAutoThemeEnabled = MutableStateFlow(repository.getAutoThemeSwitcherEnabled())
    val isAutoThemeEnabled: StateFlow<Boolean> = _isAutoThemeEnabled.asStateFlow()

    private val _customPrimaryColor = MutableStateFlow<Long?>(repository.getCustomPrimaryColor())
    val customPrimaryColor: StateFlow<Long?> = _customPrimaryColor.asStateFlow()

    private val _customSecondaryColor = MutableStateFlow<Long?>(repository.getCustomSecondaryColor())
    val customSecondaryColor: StateFlow<Long?> = _customSecondaryColor.asStateFlow()

    private val _favoriteThemes = MutableStateFlow(repository.getFavoriteThemes())
    val favoriteThemes: StateFlow<Set<String>> = _favoriteThemes.asStateFlow()

    val batterySaverEnabled: StateFlow<Boolean> = userPrefs.batterySaverEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isQrSnowflakesEnabled = MutableStateFlow(repository.getQrSnowflakesEnabled())
    val isQrSnowflakesEnabled: StateFlow<Boolean> = _isQrSnowflakesEnabled.asStateFlow()

        private val _maxCacheSizeIndex = MutableStateFlow(3) // 0: 5GB, 1: 16GB, 2: 32GB, 3: Infinity
    val maxCacheSizeIndex: StateFlow<Int> = _maxCacheSizeIndex.asStateFlow()

    fun setMaxCacheSizeIndex(index: Int) {
        _maxCacheSizeIndex.value = index
    }

    private fun getColorForCategory(name: String): Color {
        return when (name) {
            "Видео" -> Color(0xFF2196F3)
            "Файлы" -> Color(0xFF4CAF50)
            "Сообщения" -> Color(0xFFFF9800)
            "Фото" -> Color(0xFF9C27B0)
            else -> Color.Gray
        }
    }

    private fun recalculateAll(map: Map<NetworkType, NetworkStatsModel>): NetworkStatsModel {
        val mobile = map[NetworkType.MOBILE]
        val wifi = map[NetworkType.WIFI]
        val roaming = map[NetworkType.ROAMING]
        
        val sent = (mobile?.sentBytes ?: 0L) + (wifi?.sentBytes ?: 0L) + (roaming?.sentBytes ?: 0L)
        val received = (mobile?.receivedBytes ?: 0L) + (wifi?.receivedBytes ?: 0L) + (roaming?.receivedBytes ?: 0L)
        
        val categories = listOf("Видео", "Файлы", "Сообщения", "Фото").map { catName ->
            val sum = listOfNotNull(mobile, wifi, roaming).sumOf { model ->
                model.categories.find { it.categoryName == catName }?.sizeBytes ?: 0L
            }
            NetworkCategoryStats(catName, sum, getColorForCategory(catName))
        }
        return NetworkStatsModel(NetworkType.ALL, sent, received, categories)
    }

    private val _networkStats = MutableStateFlow<Map<NetworkType, NetworkStatsModel>>(
        run {
            val map = mutableMapOf(
                NetworkType.MOBILE to NetworkStatsModel(NetworkType.MOBILE, 1900000L, 2900000L, listOf(
                    NetworkCategoryStats("Видео", 2900000L, Color(0xFF2196F3)),
                    NetworkCategoryStats("Файлы", 1100000L, Color(0xFF4CAF50)),
                    NetworkCategoryStats("Сообщения", 800000L, Color(0xFFFF9800)),
                    NetworkCategoryStats("Фото", 0L, Color(0xFF9C27B0))
                )),
                NetworkType.WIFI to NetworkStatsModel(NetworkType.WIFI, 9800000L, 112200000L, listOf(
                    NetworkCategoryStats("Видео", 112900000L, Color(0xFF2196F3)),
                    NetworkCategoryStats("Файлы", 4000000L, Color(0xFF4CAF50)),
                    NetworkCategoryStats("Сообщения", 4000000L, Color(0xFFFF9800)),
                    NetworkCategoryStats("Фото", 1100000L, Color(0xFF9C27B0))
                )),
                NetworkType.ROAMING to NetworkStatsModel(NetworkType.ROAMING, 0L, 0L, listOf(
                    NetworkCategoryStats("Видео", 0L, Color(0xFF2196F3)),
                    NetworkCategoryStats("Файлы", 0L, Color(0xFF4CAF50)),
                    NetworkCategoryStats("Сообщения", 0L, Color(0xFFFF9800)),
                    NetworkCategoryStats("Фото", 0L, Color(0xFF9C27B0))
                ))
            )
            map[NetworkType.ALL] = recalculateAll(map)
            map
        }
    )
    val networkStats = _networkStats.asStateFlow()

    private val _storageStats = MutableStateFlow(
        StorageStatsModel(
            maxCacheSizeBytes = -1L,
            categories = listOf(
                StorageCategoryStats("Стикеры и эмодзи", 131900000L, Color(0xFFFF9800)),
                StorageCategoryStats("Видео", 56100000L, Color(0xFF2196F3)),
                StorageCategoryStats("Фото профиля", 55100000L, Color(0xFF00BFA5)),
                StorageCategoryStats("Файлы", 26300000L, Color(0xFF4CAF50)),
                StorageCategoryStats("Другое", 23200000L, Color(0xFFFFC107), subCategories = listOf(
                    StorageCategoryStats("Фото", 12100000L, Color(0xFF2196F3)),
                    StorageCategoryStats("Прочее", 10600000L, Color(0xFF9C27B0)),
                    StorageCategoryStats("Истории", 511200L, Color(0xFFF44336)),
                    StorageCategoryStats("Музыка", 17500L, Color(0xFF673AB7))
                ))
            )
        )
    )
    val storageStats = _storageStats.asStateFlow()

    
    fun addNetworkUsage(type: NetworkType, sentBytes: Long, receivedBytes: Long, categoryName: String) {
        val currentMap = _networkStats.value.toMutableMap()
        
        // Update specific network type
        val currentStats = currentMap[type] ?: return
        val newCategories = currentStats.categories.map {
            if (it.categoryName == categoryName) {
                it.copy(sizeBytes = it.sizeBytes + sentBytes + receivedBytes)
            } else {
                it
            }
        }
        currentMap[type] = currentStats.copy(
            sentBytes = currentStats.sentBytes + sentBytes,
            receivedBytes = currentStats.receivedBytes + receivedBytes,
            categories = newCategories
        )
        currentMap[NetworkType.ALL] = recalculateAll(currentMap)
        _networkStats.value = currentMap
    }

    fun resetNetworkStats(type: NetworkType) {
        val currentMap = _networkStats.value.toMutableMap()
        if (type == NetworkType.ALL) {
            NetworkType.values().forEach { t ->
                currentMap[t] = NetworkStatsModel(t, 0L, 0L, listOf(
                    NetworkCategoryStats("Видео", 0L, Color(0xFF2196F3)),
                    NetworkCategoryStats("Файлы", 0L, Color(0xFF4CAF50)),
                    NetworkCategoryStats("Сообщения", 0L, Color(0xFFFF9800)),
                    NetworkCategoryStats("Фото", 0L, Color(0xFF9C27B0))
                ))
            }
        } else {
            currentMap[type] = NetworkStatsModel(type, 0L, 0L, listOf(
                NetworkCategoryStats("Видео", 0L, Color(0xFF2196F3)),
                NetworkCategoryStats("Файлы", 0L, Color(0xFF4CAF50)),
                NetworkCategoryStats("Сообщения", 0L, Color(0xFFFF9800)),
                NetworkCategoryStats("Фото", 0L, Color(0xFF9C27B0))
            ))
            currentMap[NetworkType.ALL] = recalculateAll(currentMap)
        }
        _networkStats.value = currentMap
    }

    fun clearCache(selectedCategoryNames: List<String>) {
        val currentStats = _storageStats.value
        val newCategories = currentStats.categories.map { category ->
            if (category.subCategories != null) {
                val newSubCategories = category.subCategories.map { sub ->
                    if (selectedCategoryNames.contains(sub.categoryName)) {
                        sub.copy(sizeBytes = 0L)
                    } else {
                        sub
                    }
                }
                category.copy(
                    sizeBytes = newSubCategories.sumOf { it.sizeBytes },
                    subCategories = newSubCategories
                )
            } else {
                if (selectedCategoryNames.contains(category.categoryName)) {
                    category.copy(sizeBytes = 0L)
                } else {
                    category
                }
            }
        }
        _storageStats.value = currentStats.copy(categories = newCategories)
    }

    fun toggleStorageCategory(categoryName: String, isSubCategory: Boolean = false, parentCategoryName: String? = null) {
        val currentStats = _storageStats.value
        val newCategories = currentStats.categories.map { category ->
            if (isSubCategory && category.categoryName == parentCategoryName) {
                category.copy(
                    subCategories = category.subCategories?.map { sub ->
                        if (sub.categoryName == categoryName) sub.copy(isSelected = !sub.isSelected) else sub
                    }
                )
            } else if (!isSubCategory && category.categoryName == categoryName) {
                val newSelection = !category.isSelected
                category.copy(
                    isSelected = newSelection,
                    subCategories = category.subCategories?.map { it.copy(isSelected = newSelection) }
                )
            } else {
                category
            }
        }
        _storageStats.value = currentStats.copy(categories = newCategories)
    }

    fun toggleStorageCategoryExpand(categoryName: String) {
        val currentStats = _storageStats.value
        val newCategories = currentStats.categories.map {
            if (it.categoryName == categoryName) {
                it.copy(isExpanded = !it.isExpanded)
            } else {
                it
            }
        }
        _storageStats.value = currentStats.copy(categories = newCategories)
    }

    
    private val _highlightEvent = MutableStateFlow<String?>(null)
    val highlightEvent: kotlinx.coroutines.flow.StateFlow<String?> = _highlightEvent.asStateFlow()

    fun setHighlightEvent(id: String?) {
        _highlightEvent.value = id
    }

    val themeOpacity: StateFlow<Float> = userPrefs.themeOpacity
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.ONLINE)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _requires2FA = MutableStateFlow<String?>(null)
    val requires2FA: StateFlow<String?> = _requires2FA.asStateFlow()

    private val _confirmationCode = MutableStateFlow<String?>(null)
    val confirmationCode: StateFlow<String?> = _confirmationCode.asStateFlow()

    private val _pendingEmail = MutableStateFlow<String?>(null)
    val pendingEmail: StateFlow<String?> = _pendingEmail.asStateFlow()

    fun requestEmailConfirmation(email: String) {
        val code = (100000..999999).random().toString()
        _confirmationCode.value = code
        _pendingEmail.value = email
    }

    fun verifyEmailConfirmation(code: String): Boolean {
        val currentCode = _confirmationCode.value
        if (currentCode != null && currentCode == code) {
            val email = _pendingEmail.value
            _confirmationCode.value = null
            _pendingEmail.value = null
            
            // Update the email in active account
            viewModelScope.launch {
                val account = repository.allAccounts.firstOrNull()?.firstOrNull { it.isActive }
                if (account != null && email != null) {
                    repository.insertAccount(account.copy(username = email))
                }
            }
            
            return true
        }
        return false
    }

    private val _isAddingAccount = MutableStateFlow(false)
    val isAddingAccount: StateFlow<Boolean> = _isAddingAccount.asStateFlow()

    fun startAddAccount() {
        _isAddingAccount.value = true
        viewModelScope.launch { repository.logoutAll() }
    }

    fun clearAddingAccount() {
        _isAddingAccount.value = false
    }
    val accounts: StateFlow<List<UserAccount>> = repository.allAccounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val chats: StateFlow<List<Chat>> = repository.allChats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val contacts: StateFlow<List<Contact>> = repository.allContacts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isE2EEnabled = MutableStateFlow(true)
    val isE2EEnabled: StateFlow<Boolean> = _isE2EEnabled.asStateFlow()


    private val _userPresences = MutableStateFlow<Map<String, UserPresence>>(emptyMap())
    val userPresences: StateFlow<Map<String, UserPresence>> = _userPresences.asStateFlow()

    init {
        viewModelScope.launch {
            repository.webSocketManager.events.collect { event ->
                if (event is InboundEvent.PresenceUpdate) {
                    _userPresences.update { current ->
                        current + (event.userId to UserPresence(event.userId, event.isOnline, event.lastSeen))
                    }
                }
            }
        }

        viewModelScope.launch {
            // Periodic cleanup of expired messages
            launch {
                while (true) {
                    val now = System.currentTimeMillis()
                    repository.deleteExpiredMessages(now)
                    kotlinx.coroutines.delay(1000) // check every second
                }
            }
            
            // Seed initial data if empty
            val accs = repository.allAccounts.firstOrNull(); if (accs.isNullOrEmpty()) {
                
                    repository.insertAccount(UserAccount("1", "@neo_hacker", "Neo", "https://i.pravatar.cc/150?img=11", true, true, phoneNumber = "+7 (922) 669-26-82"))
                    repository.insertAccount(UserAccount("2", "@synth_wave", "Synth Wave", "https://i.pravatar.cc/150?img=33", false, false, phoneNumber = "+7 (999) 111-22-33"))
                    repository.insertAccount(UserAccount("3", "@cyber_punk", "Cyber P.", "https://i.pravatar.cc/150?img=55", false, false, phoneNumber = "+7 (777) 444-55-66"))
                    
                    repository.insertChat(Chat("c1", "Neon Coders", isGroup = true, lastMessage = "Let's build in Compose! \uD83D\uDD25", unreadCount = 4))
                    repository.insertChat(Chat("botfather", "BotFather", isBot = true, lastMessage = "", unreadCount = 0))
                    repository.insertChat(Chat("c2", "Cyberpunk Daily", isChannel = true, lastMessage = "", unreadCount = 0))
                    repository.insertChat(Chat("c3", "SynthBot", isBot = true, lastMessage = "", unreadCount = 0))
                    repository.insertChat(Chat("c4", "@trinity", isGroup = false, lastMessage = "", unreadCount = 0))

                    repository.insertGroupMember(GroupMember("c1", "u1", "Sarah Connor", isAdmin = true))
                    repository.insertGroupMember(GroupMember("c1", "u2", "John Doe", isAdmin = false))
                    repository.insertGroupMember(GroupMember("c1", "u3", "Crypto Alpha", isAdmin = false))
                    repository.insertGroupMember(GroupMember("c1", "u4", "Neon Hacker", isAdmin = false))
                }
        }
        // Cache Manager Service
        viewModelScope.launch {
            while(true) {
                delay(5000) // Check every 5 seconds
                val limitIndex = _maxCacheSizeIndex.value
                val limitMb = when(limitIndex) {
                    0 -> 5000f // 5GB
                    1 -> 16000f // 16GB
                    2 -> 32000f // 32GB
                    else -> Float.MAX_VALUE
                }
                
                // Simulate cleaning logic
                if (limitMb != Float.MAX_VALUE) {
                    println("Cache Service: Checking limit... Max allowed: $limitMb MB")
                }
            }
        }
    }


    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun syncMessages() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // Simulate network sync delay
            delay(1500)
            _isRefreshing.value = false
        }
    }

    fun getMessages(chatId: String) = repository.getMessages(chatId).map { messages ->
        messages.map { msg ->
            msg.copy(text = signalProtocolManager.decryptMessage(msg.text))
        }
    }
    fun getGroupMembers(chatId: String) = repository.getGroupMembers(chatId)
    
    suspend fun getDraft(chatId: String) = repository.getDraft(chatId)

    fun updateAdminStatus(chatId: String, userId: String, isAdmin: Boolean) {
        viewModelScope.launch {
            repository.updateAdminStatus(chatId, userId, isAdmin)
        }
    }

    fun blockUser(chatId: String) {
        viewModelScope.launch {
            repository.updateBlockedStatus(chatId, true)
        }
    }

    fun unblockUser(chatId: String) {
        viewModelScope.launch {
            repository.updateBlockedStatus(chatId, false)
        }
    }

    fun clearHistory(chatId: String) {
        viewModelScope.launch {
            repository.clearHistory(chatId)
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
        }
    }

    
    fun syncContacts(deviceContacts: List<Contact>) {
        viewModelScope.launch {
            // For simplicity in UI, we can just insert them all into the repository.
            // Since this is a demo, let's pretend some are registered if they have a phone number.
            deviceContacts.forEach { contact ->
                // random or deterministic check for isRegistered
                val isReg = contact.phoneNumber?.hashCode()?.rem(2) == 0
                repository.insertContact(contact.copy(isRegistered = isReg))
            }
        }
    }

    fun addToContacts(chatId: String) {
        viewModelScope.launch {
            repository.updateContactStatus(chatId, true)
        }
    }

    fun dismissActionMenu(chatId: String) {
        viewModelScope.launch {
            repository.updateActionMenuDismissed(chatId, true)
        }
    }

    private val _typingChats = MutableStateFlow<Set<String>>(emptySet())
    val typingChats: StateFlow<Set<String>> = _typingChats.asStateFlow()

    fun simulateTyping(chatId: String) {
        viewModelScope.launch {
            _typingChats.update { it + chatId }
            kotlinx.coroutines.delay(3000)
            _typingChats.update { it - chatId }
        }
    }

    fun exportMessageHistory(chatId: String) {
        viewModelScope.launch {
            val messages = repository.getMessages(chatId).firstOrNull() ?: emptyList()
            val text = messages.joinToString("\n") { msg ->
                val decrypted = signalProtocolManager.decryptMessage(msg.text)
                "[${java.util.Date(msg.timestamp)}] ${msg.senderId}: $decrypted"
            }
            val encryptedBackup = signalProtocolManager.encryptMessage(text)
            // Simulating saving to a file. In a real app we'd use FileOutputStream to Context.filesDir.
            println("Exported history for $chatId: \n$encryptedBackup")
        }
    }
    fun updateProfile(id: String, username: String, displayName: String, bio: String, profilePicUrl: String, customStatus: String = "") {
        viewModelScope.launch {
            repository.updateProfile(id, username, displayName, bio, profilePicUrl, customStatus)
        }
    }
    
    fun addGroupMember(chatId: String, userId: String, userName: String, isAdmin: Boolean) {
        viewModelScope.launch {
            repository.insertGroupMember(com.example.ui.GroupMember(chatId, userId, userName, isAdmin))
        }
    }

    fun updateBotPermissions(chatId: String, userId: String, canRead: Boolean, canSend: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val member = repository.getGroupMemberSync(chatId, userId)
            if (member != null) {
                repository.insertGroupMember(member.copy(canReadMessages = canRead, canSendMessages = canSend))
            }
        }
    }

    fun removeGroupMember(chatId: String, userId: String) {
        viewModelScope.launch {
            repository.removeMember(chatId, userId)
        }
    }

    fun sendMessage(chatId: String, senderId: String, text: String, audioPath: String? = null, expiresIn: Long? = null, documentData: String? = null) {
        viewModelScope.launch {
            val sanitizedText = MessageSanitizer.sanitize(text)
            val encryptedMsg = signalProtocolManager.encryptMessage(sanitizedText)
            
            // If offline, message stays locally pending
            val isOnline = _connectionStatus.value == ConnectionStatus.ONLINE
            val msg = Message(
                id = java.util.UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = senderId,
                text = encryptedMsg,
                audioPath = audioPath,
                timestamp = System.currentTimeMillis(),
                expiresAt = if (expiresIn != null) System.currentTimeMillis() + expiresIn else null,
                documentData = documentData,
                isDelivered = isOnline // if offline, it stays pending (not delivered)
            )
            repository.insertMessageAndUpdateChat(msg, sanitizedText, "You")
            
            if (isOnline) {
                // Simulate reply if online
                kotlinx.coroutines.delay(1000)
                simulateTyping(chatId)
                
                val chat = repository.allChats.firstOrNull()?.find { it.id == chatId }
                if (chat != null) {
                    if (chat.isBot || (chat.isGroup && sanitizedText.contains("@"))) {
                        BotService.handleMessage(sanitizedText, chat, repository, signalProtocolManager)
                    } else {
                        kotlinx.coroutines.delay(1500)
                        val replyText = "Got it: $sanitizedText"
                        val reply = Message(
                            id = java.util.UUID.randomUUID().toString(),
                            chatId = chatId,
                            senderId = "other_user",
                            text = signalProtocolManager.encryptMessage(replyText),
                            timestamp = System.currentTimeMillis(),
                            isDelivered = true
                        )
                        repository.insertMessageAndUpdateChat(reply, replyText, chat.title)
                    }
                }
            }
        }
    }

    fun setConnectionStatus(status: ConnectionStatus) {
        _connectionStatus.value = status
        if (status == ConnectionStatus.ONLINE) {
            processOfflineQueue()
        }
    }
    
    private fun processOfflineQueue() {
        viewModelScope.launch {
            val chats = repository.allChats.firstOrNull() ?: emptyList()
            for (chat in chats) {
                val messages = repository.getMessages(chat.id).firstOrNull() ?: emptyList()
                val pendingMessages = messages.filter { !it.isDelivered && it.senderId != "other_user" }
                for (msg in pendingMessages) {
                    // Mark as delivered
                    repository.updateMessageDelivery(msg.id, true)
                    
                    // Simulate reply
                    kotlinx.coroutines.delay(1000)
                    simulateTyping(chat.id)
                    kotlinx.coroutines.delay(1500)
                    val decryptedText = signalProtocolManager.decryptMessage(msg.text)
                    val replyText = "Offline msg received: $decryptedText"
                    val reply = Message(
                        id = java.util.UUID.randomUUID().toString(),
                        chatId = chat.id,
                        senderId = "other_user",
                        text = signalProtocolManager.encryptMessage(replyText),
                        timestamp = System.currentTimeMillis(),
                        isDelivered = true
                    )
                    repository.insertMessageAndUpdateChat(reply, replyText, chat.title)
                }
            }
        }
    }

    fun toggle2FA(accountId: String, currentEnabled: Boolean) {
        viewModelScope.launch {
            repository.update2FA(accountId, !currentEnabled)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun deleteAccount(accountId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAccount(accountId)
            onDeleted()
        }
    }

    // --- Search functionality context ---

    fun setActiveChat(chatId: String?) {
        repository.currentActiveChatId = chatId
    }
    
    fun markMessagesAsRead(chatId: String, myUserId: String) {
        viewModelScope.launch {
            repository.markAsRead(chatId, myUserId)
        }
    }
    fun saveDraft(chatId: String, text: String?) {
        viewModelScope.launch {
            repository.updateDraft(chatId, text)
        }
    }
    fun addReaction(messageId: String, reaction: String) {
        viewModelScope.launch {
            repository.updateReaction(messageId, reaction)
        }
    }
    fun pinMessage(chatId: String, messageId: String) {
        viewModelScope.launch {
            repository.updatePinStatus(messageId, true)
        }
    }
    fun unpinMessage(chatId: String, messageId: String) {
        viewModelScope.launch {
            repository.updatePinStatus(messageId, false)
        }
    }
    fun switchAccount(accountId: String) {
        viewModelScope.launch {
            repository.switchActiveAccount(accountId)
        }
    }
    fun createAccount(phoneNumber: String, username: String, displayName: String, bio: String = "", profilePicUrl: String = "", customStatus: String = "") {
        viewModelScope.launch {
            val account = UserAccount(
                id = java.util.UUID.randomUUID().toString(),
                phoneNumber = phoneNumber,
                username = username,
                displayName = displayName,
                bio = bio,
                profilePicUrl = profilePicUrl,
                customStatus = customStatus,
                isActive = true
            )
            repository.logoutAll()
            repository.insertAccount(account)
        }
    }
    fun addAccountAction() {
        _isAddingAccount.value = true
    }
    fun verify2FA(code: String) {
        _requires2FA.value = null
    }
    fun cancel2FA() {
        _requires2FA.value = null
    }
    fun createSecretChat(contactId: String) {
        viewModelScope.launch {
            val chat = Chat(id = java.util.UUID.randomUUID().toString(), title = "Secret Chat", isGroup = false, isSecret = true, lastMessage = "")
            repository.insertChat(chat)
        }
    }
    fun createChat(name: String, desc: String, photo: String, isPrivate: Boolean, linkOrUsername: String, isGroup: Boolean = false, isChannel: Boolean = false) {
        viewModelScope.launch {
            val chat = Chat(id = java.util.UUID.randomUUID().toString(), title = name, isGroup = isGroup, isSecret = false, isChannel = isChannel, lastMessage = "")
            repository.insertChat(chat)
        }
    }
    fun toggleArchive(chatId: String, isArchived: Boolean) {
        viewModelScope.launch {
            val chat = repository.allChats.firstOrNull()?.find { it.id == chatId }
            if (chat != null) {
                repository.updateArchiveStatus(chatId, !chat.isArchived)
            }
        }
    }
    fun setAutoThemeEnabled(enabled: Boolean) {
        _isAutoThemeEnabled.value = enabled
        repository.saveAutoThemeSwitcherEnabled(enabled)
    }
    fun setDarkThemeEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefs.saveDarkTheme(enabled) }
    }
    fun setBatterySaverEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefs.saveBatterySaverEnabled(enabled) }
    }
    fun setQrSnowflakesEnabled(enabled: Boolean) {
        _isQrSnowflakesEnabled.value = enabled
        repository.saveQrSnowflakesEnabled(enabled)
    }

    fun setThemeOpacity(opacity: Float) {
        viewModelScope.launch { userPrefs.saveThemeOpacity(opacity) }
    }
    fun setCustomPrimaryColor(color: Long?) {
        _customPrimaryColor.value = color
        if (color != null) repository.saveCustomPrimaryColor(color)
    }
    fun setCustomSecondaryColor(color: Long?) {
        _customSecondaryColor.value = color
        if (color != null) repository.saveCustomSecondaryColor(color)
    }
    fun switchTheme(theme: AppTheme) {
        _theme.value = theme
        repository.saveTheme(theme.name)
    }
    fun toggleFavoriteTheme(themeName: String) {
        val current = _favoriteThemes.value.toMutableSet()
        if (current.contains(themeName)) current.remove(themeName) else current.add(themeName)
        _favoriteThemes.value = current
        repository.saveFavoriteThemes(current)
    }
    fun importTheme(themeCode: String) {
        try {
            val parts = themeCode.substringAfter("Neon Messenger Theme Code: ").split("-")
            if (parts.size >= 3) {
                val themeName = parts[0]
                val primaryStr = parts[1]
                val secondaryStr = parts[2]
                switchTheme(AppTheme.valueOf(themeName))
                setCustomPrimaryColor(if (primaryStr != "def") primaryStr.toLongOrNull() else null)
                setCustomSecondaryColor(if (secondaryStr != "def") secondaryStr.toLongOrNull() else null)
                setAutoThemeEnabled(false)
            }
        } catch (e: Exception) {}
    }
    fun resetTheme() {
        switchTheme(AppTheme.DEFAULT)
        setCustomPrimaryColor(null)
        setCustomSecondaryColor(null)
        setAutoThemeEnabled(false)
    }
    fun logout() {
        viewModelScope.launch {
            repository.logoutAll()
        }
    }
    fun checkAutoTheme() {}
    fun addBot(chat: Chat) { viewModelScope.launch { repository.insertChat(chat) } }

    fun updatePasscode(accountId: String, newPasscode: String?) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val account = accounts.value.find { it.id == accountId }
            if (account != null) {
                val encrypted = newPasscode?.let { com.example.data.CryptoManager.encrypt(it) }
                repository.insertAccount(account.copy(encryptedPasscode = encrypted, is2FAEnabled = if (encrypted != null) false else account.is2FAEnabled))
            }
        }
    }
}
