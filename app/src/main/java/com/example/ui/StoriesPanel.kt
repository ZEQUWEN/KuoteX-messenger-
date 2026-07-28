package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

data class Story(val id: String, val author: String, val avatarUrl: String, val mediaUrl: String, val isViewed: Boolean = false)

val sampleStories = listOf(
    Story("1", "My Story", "https://i.pravatar.cc/150?u=my", "https://picsum.photos/400/800?random=1", false),
    Story("2", "Alice", "https://i.pravatar.cc/150?u=alice", "https://picsum.photos/400/800?random=2", false),
    Story("3", "Bob", "https://i.pravatar.cc/150?u=bob", "https://picsum.photos/400/800?random=3", false),
    Story("4", "Charlie", "https://i.pravatar.cc/150?u=charlie", "https://picsum.photos/400/800?random=4", true),
    Story("5", "Dave", "https://i.pravatar.cc/150?u=dave", "https://picsum.photos/400/800?random=5", true)
)

@Composable
fun StoriesPanel(onStorySwipe: (Boolean) -> Unit, onAvatarClick: (Story) -> Unit = {}) {
    var selectedStory by remember { mutableStateOf<Story?>(null) }
    
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    // Notify parent about swipe to hide text
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            onStorySwipe(true)
        } else if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
            onStorySwipe(false)
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(sampleStories) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { selectedStory = story }
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = if (story.isViewed) Color.Gray else MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .padding(4.dp)
                ) {
                    AsyncImage(
                        model = story.avatarUrl,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = story.author,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }

    if (selectedStory != null) {
        StoryViewerPopup(story = selectedStory!!, onAvatarClick = onAvatarClick) {
            selectedStory = null
        }
    }
}

@Composable
fun StoryViewerPopup(story: Story, onAvatarClick: (Story) -> Unit = {}, onDismiss: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(story) {
        val duration = 15000 // 15 seconds "Short" default
        val steps = 100
        val delayTime = (duration / steps).toLong()
        for (i in 1..steps) {
            delay(delayTime)
            progress = i.toFloat() / steps
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = story.mediaUrl,
                contentDescription = "Story Media",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        onDismiss()
                        onAvatarClick(story)
                    }.padding(end = 16.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    AsyncImage(
                        model = story.avatarUrl,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = story.author,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
            
            // Allow tap to skip
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onDismiss() }) // Tap left to close (or previous in real app)
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onDismiss() }) // Tap right to close (or next in real app)
            }
        }
    }
}
