import re

with open("app/src/main/java/com/example/ui/AppViewModel.kt", "r") as f:
    content = f.read()

# Update initial network stats to have ALL calculated correctly
replacement_init = """    private fun getColorForCategory(name: String): Color {
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
    )"""

start_idx = content.find('    private val _networkStats = MutableStateFlow(')
end_idx = content.find('    val networkStats = _networkStats.asStateFlow()')
if start_idx != -1 and end_idx != -1:
    content = content[:start_idx] + replacement_init + '\n' + content[end_idx:]


replacement_add = """    fun addNetworkUsage(type: NetworkType, sentBytes: Long, receivedBytes: Long, categoryName: String) {
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
    }"""
    
start_add = content.find('    fun addNetworkUsage(')
end_add = content.find('    fun resetNetworkStats(')
if start_add != -1 and end_add != -1:
    content = content[:start_add] + replacement_add + '\n\n' + content[end_add:]


replacement_reset = """    fun resetNetworkStats(type: NetworkType) {
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
    }"""
    
start_reset = content.find('    fun resetNetworkStats(')
end_reset = content.find('    fun clearCache(')
if start_reset != -1 and end_reset != -1:
    content = content[:start_reset] + replacement_reset + '\n\n' + content[end_reset:]

with open("app/src/main/java/com/example/ui/AppViewModel.kt", "w") as f:
    f.write(content)
