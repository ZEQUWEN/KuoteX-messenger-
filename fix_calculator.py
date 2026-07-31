import re

with open("app/src/main/java/com/example/utils/CacheCalculator.kt", "r") as f:
    content = f.read()

replacement = """    fun clearAppCache(context: Context, selectedCategories: List<String>) {
        try {
            // Mapping categories to typical file extensions
            val extsToDelete = mutableListOf<String>()
            if (selectedCategories.contains("Видео") || selectedCategories.contains("Истории")) {
                extsToDelete.addAll(listOf("mp4", "avi", "mkv", "webm"))
            }
            if (selectedCategories.contains("Фото") || selectedCategories.contains("Фото профиля")) {
                extsToDelete.addAll(listOf("jpg", "jpeg", "png", "webp", "heic"))
            }
            if (selectedCategories.contains("Музыка")) {
                extsToDelete.addAll(listOf("mp3", "wav", "ogg", "m4a"))
            }
            if (selectedCategories.contains("Стикеры и эмодзи")) {
                extsToDelete.addAll(listOf("tgs", "json", "gif"))
            }
            if (selectedCategories.contains("Файлы") || selectedCategories.contains("Прочее")) {
                extsToDelete.addAll(listOf("pdf", "doc", "docx", "xls", "txt", "zip", "rar", "tmp"))
            }
            
            val dirs = listOf(context.cacheDir, context.filesDir, context.externalCacheDir)
            
            // If they select all main categories, just wipe everything
            val isWipeAll = selectedCategories.containsAll(listOf("Стикеры и эмодзи", "Видео", "Фото профиля", "Файлы", "Другое"))
            
            dirs.forEach { dir ->
                if (dir != null && dir.exists()) {
                    if (isWipeAll || extsToDelete.isEmpty()) {
                        // Clear all
                        dir.listFiles()?.forEach { deleteRecursively(it) }
                    } else {
                        // Clear specific types
                        deleteFilesByExtension(dir, extsToDelete)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun deleteFilesByExtension(file: File, extensions: List<String>) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteFilesByExtension(it, extensions) }
        } else {
            val ext = file.extension.lowercase()
            if (extensions.contains(ext) || extensions.contains("tmp")) {
                file.delete()
            }
        }
    }"""

start = content.find('    fun clearAppCache(context: Context) {')
end = content.find('    private fun deleteRecursively')
if start != -1 and end != -1:
    content = content[:start] + replacement + '\n\n' + content[end:]
    with open("app/src/main/java/com/example/utils/CacheCalculator.kt", "w") as f:
        f.write(content)
