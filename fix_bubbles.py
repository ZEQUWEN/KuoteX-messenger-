import re
with open("app/src/main/java/com/example/ui/ChatScreen.kt", "r") as f:
    text = f.read()

text = text.replace(
    "if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) \n                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)",
    "if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant"
)

text = text.replace(
    "Text(message.text, color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)",
    "Text(message.text, color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)"
)

text = text.replace(
    "Icon(icon, contentDescription = \"File\", tint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)",
    "Icon(icon, contentDescription = \"File\", tint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)"
)

text = text.replace(
    "Text(name, color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant",
    "Text(name, color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface"
)

with open("app/src/main/java/com/example/ui/ChatScreen.kt", "w") as f:
    f.write(text)
