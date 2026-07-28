import re
with open("app/src/main/java/com/example/ui/NeonCanvases.kt", "r") as f:
    text = f.read()

text = text.replace(
    "Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {",
    "val bgColor = androidx.compose.material3.MaterialTheme.colorScheme.background\n    val pColor = androidx.compose.material3.MaterialTheme.colorScheme.primary\n    val sColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary\n    Canvas(modifier = Modifier.fillMaxSize().background(bgColor)) {"
)

text = text.replace(
    "Color(0xFFFF007F).copy(alpha = 0.20f * opacity)",
    "pColor.copy(alpha = 0.20f * opacity)"
)

text = text.replace(
    "Color(0xFF7F00FF).copy(alpha = 0.20f * opacity)",
    "sColor.copy(alpha = 0.20f * opacity)"
)

with open("app/src/main/java/com/example/ui/NeonCanvases.kt", "w") as f:
    f.write(text)
