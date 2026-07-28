package com.example.ui.botapi

import com.example.crypto.SignalProtocolManager
import com.example.data.MessengerRepository
import com.example.ui.Chat
import com.example.ui.Message
import java.util.UUID
import com.example.utils.MessageSanitizer

class BotFather : Bot {
    override val id: String = "botfather"
    override val name: String = "BotFather"
    override val description: String = "Create and manage your bots."
    override val category: String = "Platform"
    override val longDescription: String = "I am the BotFather. I can help you create and manage your bots."
    override val commands: List<BotCommand> = listOf(BotCommand("/newbot", "Create a new bot"), BotCommand("/mybots", "List your bots"))

    // State management: chatId -> State
    private val states = mutableMapOf<String, BotFatherState>()

    sealed class BotFatherState {
        object Idle : BotFatherState()
        object WaitingForBotName : BotFatherState()
        object WaitingForBotUsername : BotFatherState()
        data class ManagingBot(val botId: String) : BotFatherState()
        data class WaitingForChangeName(val botId: String) : BotFatherState()
        data class WaitingForChangeDescription(val botId: String) : BotFatherState()
        data class WaitingForChangeUsername(val botId: String) : BotFatherState()
        data class WaitingForAddBotDescription(val botId: String) : BotFatherState()
        data class WaitingForWebhookUrl(val botId: String) : BotFatherState()
        data class WaitingForTrafficLimit(val botId: String) : BotFatherState()
    }

    private var pendingBotName: String = ""

    override suspend fun onMessageReceived(
        messageText: String,
        chat: Chat,
        repository: MessengerRepository,
        signalProtocolManager: SignalProtocolManager
    ) {
        val state = states[chat.id] ?: BotFatherState.Idle
        
        when (state) {
            is BotFatherState.Idle -> {
                when (messageText.lowercase().trim()) {
                    "/start" -> {
                        states[chat.id] = BotFatherState.Idle
                        sendReplyWithButtons("I can help you create and manage Telegram bots. If you're new to the Bot API, please see the manual.\n\nYou can control me by sending these commands:\n\n/newbot - create a new bot\n/mybots - edit your bots\n/description - bot description", emptyList(), chat.id, repository, signalProtocolManager)
                    }
                    "/description" -> {
                        sendReplyWithButtons("BotFather is the one bot to rule them all. Use it to create new bot accounts and manage your existing bots.\n\nAbout Telegram bots:\nhttps://core.telegram.org/bots\nBot API manual:\nhttps://core.telegram.org/bots/api\n\nContact @BotSupport if you have questions about the Bot API.", emptyList(), chat.id, repository, signalProtocolManager)
                    }
                    "create bot", "/newbot", "/new bot" -> {
                        states[chat.id] = BotFatherState.WaitingForBotName
                        sendReplyWithButtons("Alright, a new bot. How are we going to call it? Please choose a name for your bot.", emptyList(), chat.id, repository, signalProtocolManager)
                    }
                    "manage bots", "/mybots", "/my bots" -> {
                        val customBots = BotRegistry.getCustomBots()
                        if (customBots.isEmpty()) {
                            sendReplyWithButtons("You don't have any bots yet. Create one with '/newbot'.", listOf("Create Bot"), chat.id, repository, signalProtocolManager)
                        } else {
                            val buttons = customBots.map { it.name }
                            sendReplyWithButtons("Choose a bot to manage:", buttons, chat.id, repository, signalProtocolManager)
                        }
                    }
                    else -> {
                        val customBot = BotRegistry.getCustomBots().find { it.name == messageText }
                        if (customBot != null) {
                            states[chat.id] = BotFatherState.ManagingBot(customBot.id)
                            sendBotManagementMenu(customBot as CustomBot, chat.id, repository, signalProtocolManager)
                        } else {
                            sendReplyWithButtons(
                                "I am the BotFather. You can create new bots and manage them here.",
                                listOf("Create Bot", "Manage Bots"),
                                chat.id, repository, signalProtocolManager
                            )
                        }
                    }
                }
            }
            is BotFatherState.WaitingForBotName -> {
                pendingBotName = messageText
                states[chat.id] = BotFatherState.WaitingForBotUsername
                sendReplyWithButtons("Good. Now let's choose a username for your bot. It must end in 'bot'. Like this: TetrisBot or tetris_bot.", emptyList(), chat.id, repository, signalProtocolManager)
            }
            is BotFatherState.WaitingForBotUsername -> {
                val username = messageText.replace(" ", "")
                if (!username.lowercase().endsWith("bot")) {
                    sendReplyWithButtons("Sorry, the username must end in 'bot'. Try again.", emptyList(), chat.id, repository, signalProtocolManager)
                    return
                }
                if (BotRegistry.getBot(username) != null) {
                    sendReplyWithButtons("Sorry, this username is already taken. Try again.", emptyList(), chat.id, repository, signalProtocolManager)
                    return
                }
                
                val newBot = CustomBot(
                    id = username,
                    name = pendingBotName,
                    description = "A custom bot.",
                    category = "Custom",
                    longDescription = "This is a custom bot created via BotFather."
                )
                BotRegistry.registerCustomBot(newBot)
                
                repository.insertChat(Chat(id = newBot.id, title = newBot.name, isBot = true, lastMessage = newBot.description))
                
                states[chat.id] = BotFatherState.Idle
                sendReplyWithButtons("Done! Congratulations on your new bot. You can manage it now.", listOf("Manage Bots", "Create Bot"), chat.id, repository, signalProtocolManager)
            }
            is BotFatherState.ManagingBot -> {
                val bot = BotRegistry.getBot(state.botId) as? CustomBot
                if (bot == null) {
                    states[chat.id] = BotFatherState.Idle
                    sendReplyWithButtons("Bot not found.", listOf("Create Bot", "Manage Bots"), chat.id, repository, signalProtocolManager)
                    return
                }
                
                when (messageText) {
                    "Change Name" -> {
                        states[chat.id] = BotFatherState.WaitingForChangeName(bot.id)
                        sendReplyWithButtons("Send me the new name for your bot.", emptyList(), chat.id, repository, signalProtocolManager)
                    }
                    "Change Description" -> {
                        states[chat.id] = BotFatherState.WaitingForChangeDescription(bot.id)
                        sendReplyWithButtons("Send me the new description for your bot.", emptyList(), chat.id, repository, signalProtocolManager)
                    }
                    "Add/Change Username" -> {
                        states[chat.id] = BotFatherState.WaitingForChangeUsername(bot.id)
                        sendReplyWithButtons("Send me the new username for your bot (must end in 'bot').", emptyList(), chat.id, repository, signalProtocolManager)
                    }
                    "Add Bot Description" -> {
                        states[chat.id] = BotFatherState.WaitingForAddBotDescription(bot.id)
                        sendReplyWithButtons("Send me the new long description (welcome message) for your bot.", emptyList(), chat.id, repository, signalProtocolManager)
                    }
                    "OAuth Auth" -> {
                        val msg = if (bot.oauthToken == null) {
                            "OAuth2 is not configured. Do you want to authenticate your service account?"
                        } else {
                            "OAuth2 is authenticated. Token: ${bot.oauthToken}\nDo you want to re-authenticate?"
                        }
                        sendReplyWithButtons(msg, listOf("Authenticate OAuth", "Revoke OAuth", "Back"), chat.id, repository, signalProtocolManager)
                    }
                    "Authenticate OAuth" -> {
                        bot.oauthToken = "oauth2_token_" + UUID.randomUUID().toString()
                        bot.logs.add(LogEntry(message = "Service account authenticated via OAuth2."))
                        sendReplyWithButtons("OAuth2 Authentication successful!\nToken: ${bot.oauthToken}", listOf("Back"), chat.id, repository, signalProtocolManager)
                    }
                    "Revoke OAuth" -> {
                        bot.oauthToken = null
                        bot.logs.add(LogEntry(message = "OAuth2 token revoked."))
                        sendReplyWithButtons("OAuth2 Token revoked successfully.", listOf("Back"), chat.id, repository, signalProtocolManager)
                    }
                    "Webhook" -> {
                        val msg = if (bot.webhookUrl.isNullOrEmpty()) "No webhook configured." else "Current Webhook: ${bot.webhookUrl}"
                        sendReplyWithButtons(msg, listOf("Set Webhook", "Test Webhook", "Clear Webhook", "Back"), chat.id, repository, signalProtocolManager)
                    }
                    "Set Webhook" -> {
                        states[chat.id] = BotFatherState.WaitingForWebhookUrl(bot.id)
                        sendReplyWithButtons("Send me the new webhook URL for your bot.", emptyList(), chat.id, repository, signalProtocolManager)
                    }
                    "Test Webhook" -> {
                        if (bot.webhookUrl.isNullOrEmpty()) {
                            sendReplyWithButtons("No webhook configured. Please set one first.", listOf("Set Webhook", "Back"), chat.id, repository, signalProtocolManager)
                        } else {
                            bot.logs.add(LogEntry(message = "Webhook TEST payload sent to ${bot.webhookUrl}"))
                            bot.logs.add(LogEntry(level = "SUCCESS", message = "200 OK from ${bot.webhookUrl}"))
                            sendReplyWithButtons("Test payload sent to ${bot.webhookUrl}.\nReceived response: 200 OK.", listOf("Back"), chat.id, repository, signalProtocolManager)
                        }
                    }
                    "Clear Webhook" -> {
                        bot.webhookUrl = null
                        sendReplyWithButtons("Webhook cleared.", listOf("Back"), chat.id, repository, signalProtocolManager)
                    }
                    "Traffic Limits" -> {
                        val msg = "Current traffic limit: ${bot.rateLimit} messages/min."
                        sendReplyWithButtons(msg, listOf("Set Limit", "Back"), chat.id, repository, signalProtocolManager)
                    }
                    "Set Limit" -> {
                        states[chat.id] = BotFatherState.WaitingForTrafficLimit(bot.id)
                        sendReplyWithButtons("Send me the new traffic limit (messages per minute) as a number.", emptyList(), chat.id, repository, signalProtocolManager)
                    }
                    "Dashboard" -> {
                        sendReplyWithButtons("Open the Dashboard from the main menu, or navigate to it directly to view statistics and configuration.", listOf("Back"), chat.id, repository, signalProtocolManager)
                    }
                    "Sandbox" -> {
                        sendReplyWithButtons("Open the Sandbox testing environment from the main menu, or navigate to it directly to test your bot locally.", listOf("Back"), chat.id, repository, signalProtocolManager)
                    }
                    "Delete Bot" -> {
                        BotRegistry.unregisterCustomBot(bot.id)
                        repository.deleteChat(bot.id)
                        states[chat.id] = BotFatherState.Idle
                        sendReplyWithButtons("Bot deleted.", listOf("Create Bot", "Manage Bots"), chat.id, repository, signalProtocolManager)
                    }
                    "Back" -> {
                        states[chat.id] = BotFatherState.Idle
                        sendReplyWithButtons("Main menu:", listOf("Create Bot", "Manage Bots"), chat.id, repository, signalProtocolManager)
                    }
                    else -> sendBotManagementMenu(bot, chat.id, repository, signalProtocolManager)
                }
            }
            is BotFatherState.WaitingForChangeName -> {
                val bot = BotRegistry.getBot(state.botId) as? CustomBot
                if (bot != null) {
                    bot.name = messageText
                    repository.insertChat(Chat(id = bot.id, title = bot.name, isBot = true, lastMessage = bot.description))
                    sendReplyWithButtons("Name updated successfully.", emptyList(), chat.id, repository, signalProtocolManager)
                    states[chat.id] = BotFatherState.ManagingBot(bot.id)
                    sendBotManagementMenu(bot, chat.id, repository, signalProtocolManager)
                }
            }
            is BotFatherState.WaitingForChangeDescription -> {
                val bot = BotRegistry.getBot(state.botId) as? CustomBot
                if (bot != null) {
                    bot.description = messageText
                    repository.insertChat(Chat(id = bot.id, title = bot.name, isBot = true, lastMessage = bot.description))
                    sendReplyWithButtons("Description updated successfully.", emptyList(), chat.id, repository, signalProtocolManager)
                    states[chat.id] = BotFatherState.ManagingBot(bot.id)
                    sendBotManagementMenu(bot, chat.id, repository, signalProtocolManager)
                }
            }
            is BotFatherState.WaitingForChangeUsername -> {
                val bot = BotRegistry.getBot(state.botId) as? CustomBot
                if (bot != null) {
                    val username = messageText.replace(" ", "")
                    if (!username.lowercase().endsWith("bot")) {
                        sendReplyWithButtons("Username must end in 'bot'. Try again.", emptyList(), chat.id, repository, signalProtocolManager)
                        return
                    }
                    if (BotRegistry.getBot(username) != null) {
                        sendReplyWithButtons("Username already taken. Try again.", emptyList(), chat.id, repository, signalProtocolManager)
                        return
                    }
                    BotRegistry.unregisterCustomBot(bot.id)
                    repository.deleteChat(bot.id)
                    bot.id = username
                    BotRegistry.registerCustomBot(bot)
                    repository.insertChat(Chat(id = bot.id, title = bot.name, isBot = true, lastMessage = bot.description))
                    
                    sendReplyWithButtons("Username updated successfully.", emptyList(), chat.id, repository, signalProtocolManager)
                    states[chat.id] = BotFatherState.ManagingBot(bot.id)
                    sendBotManagementMenu(bot, chat.id, repository, signalProtocolManager)
                }
            }
            is BotFatherState.WaitingForAddBotDescription -> {
                val bot = BotRegistry.getBot(state.botId) as? CustomBot
                if (bot != null) {
                    bot.longDescription = messageText
                    sendReplyWithButtons("Bot welcome description updated successfully.", emptyList(), chat.id, repository, signalProtocolManager)
                    states[chat.id] = BotFatherState.ManagingBot(bot.id)
                    sendBotManagementMenu(bot, chat.id, repository, signalProtocolManager)
                }
            }
            is BotFatherState.WaitingForWebhookUrl -> {
                val bot = BotRegistry.getBot(state.botId) as? CustomBot
                if (bot != null) {
                    if (messageText.startsWith("http://") || messageText.startsWith("https://")) {
                        bot.webhookUrl = messageText
                        sendReplyWithButtons("Webhook updated successfully.", emptyList(), chat.id, repository, signalProtocolManager)
                    } else {
                        sendReplyWithButtons("Invalid URL. Must start with http:// or https://. Webhook not updated.", emptyList(), chat.id, repository, signalProtocolManager)
                    }
                    states[chat.id] = BotFatherState.ManagingBot(bot.id)
                    sendBotManagementMenu(bot, chat.id, repository, signalProtocolManager)
                }
            }
            is BotFatherState.WaitingForTrafficLimit -> {
                val bot = BotRegistry.getBot(state.botId) as? CustomBot
                if (bot != null) {
                    val newLimit = messageText.toIntOrNull()
                    if (newLimit != null && newLimit > 0) {
                        bot.rateLimit = newLimit
                        sendReplyWithButtons("Traffic limit updated to $newLimit msgs/min.", emptyList(), chat.id, repository, signalProtocolManager)
                    } else {
                        sendReplyWithButtons("Invalid number. Limit not updated.", emptyList(), chat.id, repository, signalProtocolManager)
                    }
                    states[chat.id] = BotFatherState.ManagingBot(bot.id)
                    sendBotManagementMenu(bot, chat.id, repository, signalProtocolManager)
                }
            }
        }
    }

    private suspend fun sendBotManagementMenu(
        bot: CustomBot,
        chatId: String,
        repository: MessengerRepository,
        signalProtocolManager: SignalProtocolManager
    ) {
        val menu = "Managing bot: ${bot.name} (@${bot.id})\nDescription: ${bot.description}\nWhat would you like to do?"
        val buttons = listOf(
            "Change Name", "Change Description", "Add/Change Username", "Add Bot Description", "OAuth Auth", "Webhook", "Traffic Limits", "Dashboard::${bot.id}", "Sandbox::${bot.id}", "Delete Bot", "Back"
        )
        sendReplyWithButtons(menu, buttons, chatId, repository, signalProtocolManager)
    }

    private suspend fun sendReplyWithButtons(
        text: String,
        buttons: List<String>,
        chatId: String,
        repository: MessengerRepository,
        signalProtocolManager: SignalProtocolManager
    ) {
        val sanitizedText = MessageSanitizer.sanitize(text)
        val encryptedReply = signalProtocolManager.encryptMessage(sanitizedText)
        val buttonsData = if (buttons.isNotEmpty()) buttons.joinToString("||") else null
        val replyMsg = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = "bot_$id",
            text = encryptedReply,
            buttonsData = buttonsData,
            timestamp = System.currentTimeMillis()
        )
        repository.insertMessage(replyMsg)
    }
}

class CustomBot(
    override var id: String,
    override var name: String,
    override var description: String,
    override var category: String,
    override var longDescription: String,
    override var commands: List<BotCommand> = emptyList(),
    var oauthToken: String? = null,
    var webhookUrl: String? = null,
    var rateLimit: Int = 100,
    var code: String = "fun handleMessage(msg: String): String {\n    return \"Echo from \$name: \$msg\"\n}",
    var snapshots: MutableList<CodeSnapshot> = mutableListOf(),
    var collaborators: MutableList<Collaborator> = mutableListOf(),
    var stats: BotStats = BotStats(),
    val logs: MutableList<LogEntry> = mutableListOf()
) : Bot {
    override suspend fun onMessageReceived(
        messageText: String,
        chat: Chat,
        repository: MessengerRepository,
        signalProtocolManager: SignalProtocolManager
    ) {
        stats.totalMessages++
        stats.activeUsers = (stats.activeUsers + 1).coerceAtMost(100) // Simulated unique users
        stats.interactionRate = if (stats.activeUsers > 0) stats.totalMessages.toFloat() / stats.activeUsers else 0f
        
        logs.add(LogEntry(message = "Received message from ${chat.id}: $messageText"))
        
        if (!webhookUrl.isNullOrBlank()) {
            logs.add(LogEntry(message = "Dispatching to webhook: $webhookUrl"))
            try {
                val responseText = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val url = java.net.URL(webhookUrl)
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    
                    val payload = """
                    {
                      "update_type": "message",
                      "bot_id": "$id",
                      "message": {
                        "text": "$messageText",
                        "chat_id": "${chat.id}"
                      }
                    }
                    """.trimIndent()
                    
                    conn.outputStream.use { os ->
                        val input = payload.toByteArray(Charsets.UTF_8)
                        os.write(input, 0, input.size)
                    }
                    
                    val responseCode = conn.responseCode
                    if (responseCode in 200..299) {
                        conn.inputStream.bufferedReader().readText()
                    } else {
                        null
                    }
                }
                logs.add(LogEntry(message = "Webhook dispatched successfully."))
                if (!responseText.isNullOrBlank()) {
                    var reply = responseText
                    if (responseText!!.contains("\"text\":")) {
                        val textMatch = "\"text\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(responseText!!)
                        if (textMatch != null) {
                            reply = textMatch.groupValues[1]
                        }
                    }
                    logs.add(LogEntry(message = "Webhook reply: $reply"))
                    sendReply(reply, chat.id, repository, signalProtocolManager)
                }
                return
            } catch (e: Exception) {
                logs.add(LogEntry(level = "ERROR", message = "Webhook failed: ${e.message}"))
            }
        }

        // Simple echo for custom bots. In a real system, this would evaluate `code` or call `webhookUrl`.
        val reply = "Echo from $name: $messageText"
        logs.add(LogEntry(message = "Replied: $reply"))
        sendReply(reply, chat.id, repository, signalProtocolManager)
    }
}

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: String = "INFO",
    val message: String
)

data class BotStats(
    var activeUsers: Int = 0,
    var totalMessages: Int = 0,
    var interactionRate: Float = 0f
)

data class CodeSnapshot(
    val version: Int,
    val code: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Collaborator(
    val username: String,
    val role: String
)

fun generateApiKey(): String = "bot_" + UUID.randomUUID().toString().replace("-", "")
