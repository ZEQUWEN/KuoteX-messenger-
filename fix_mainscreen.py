import re
with open("app/src/main/java/com/example/ui/MainScreen.kt", "r") as f:
    text = f.read()

text = text.replace(
    ".background(Color(0xFF27272A))",
    ".background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)"
)

text = text.replace(
    ".background(Color(0xFF3B82F6))",
    ".background(androidx.compose.material3.MaterialTheme.colorScheme.primary)"
)

text = text.replace(
    ".background(Color(0xFF10B981))",
    ".background(androidx.compose.material3.MaterialTheme.colorScheme.secondary)"
)

text = text.replace(
    ".background(Color.DarkGray)",
    ".background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)"
)

text = text.replace(
    ".background(Color.Gray)",
    ".background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)"
)

with open("app/src/main/java/com/example/ui/MainScreen.kt", "w") as f:
    f.write(text)
