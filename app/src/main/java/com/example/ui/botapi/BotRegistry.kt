package com.example.ui.botapi

object BotRegistry {
    private val bots = mutableMapOf<String, Bot>()
    private val customBots = mutableMapOf<String, Bot>()

    init {
        // Register all available bots
        registerBot(WeatherBot())
        registerBot(ReminderBot())
        registerBot(EchoBot())
        registerBot(CryptoBot())
        registerBot(BotFather())
        registerBot(NewsBot())
    }

    fun registerBot(bot: Bot) {
        bots[bot.id] = bot
    }
    
    fun registerCustomBot(bot: Bot) {
        bots[bot.id] = bot
        customBots[bot.id] = bot
    }
    
    fun unregisterCustomBot(id: String) {
        bots.remove(id)
        customBots.remove(id)
    }
    
    fun getCustomBots(): List<Bot> {
        return customBots.values.toList()
    }

    fun getBot(id: String): Bot? {
        return bots[id]
    }

    fun getAllBots(): List<Bot> {
        return bots.values.toList().sortedBy { it.id }
    }
}
