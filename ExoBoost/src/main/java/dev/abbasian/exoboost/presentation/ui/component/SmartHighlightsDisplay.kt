package dev.abbasian.exoboost.presentation.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abbasian.exoboost.R
import dev.abbasian.exoboost.domain.model.HighlightReason
import dev.abbasian.exoboost.domain.model.HighlightSegment
import dev.abbasian.exoboost.domain.model.VideoChapter
import dev.abbasian.exoboost.domain.model.VideoHighlights
import dev.abbasian.exoboost.presentation.state.HighlightsState
import dev.abbasian.exoboost.util.formatTime

@Composable
fun smartHighlightsDisplay(
    highlightsState: HighlightsState,
    onPlayHighlights: () -> Unit,
    onJumpToHighlight: (Int) -> Unit,
    onJumpToChapter: (VideoChapter) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = highlightsState !is HighlightsState.Idle,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        when (highlightsState) {
            is HighlightsState.Analyzing -> {
                AnalyzingView(
                    progress = highlightsState.progress,
                    onClose = onClose,
                )
            }

            is HighlightsState.Success -> {
                SuccessView(
                    highlights = highlightsState.highlights,
                    onPlayHighlights = onPlayHighlights,
                    onJumpToHighlight = onJumpToHighlight,
                    onJumpToChapter = onJumpToChapter,
                    onClose = onClose,
                )
            }

            is HighlightsState.Error -> {
                ErrorView(
                    message = highlightsState.message,
                    onClose = onClose,
                )
            }

            else -> {}
        }
    }
}

@Composable
private fun AnalyzingView(
    progress: String,
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Box(modifier = Modifier.padding(24.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(72.dp)
                            .background(Color(0xFFFF6B6B).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(36.dp),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = context.getString(R.string.highlights_analyzing),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = progress,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFFF6B6B),
                    trackColor = Color.White.copy(alpha = 0.1f),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FeatureChip(Icons.Default.FlashOn, "Motion")
                    FeatureChip(Icons.Default.VolumeUp, "Audio")
                    FeatureChip(Icons.Default.MovieFilter, "Scenes")
                }
            }

            IconButton(
                onClick = onClose,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun FeatureChip(
    icon: ImageVector,
    label: String,
) {
    Surface(
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SuccessView(
    highlights: VideoHighlights,
    onPlayHighlights: () -> Unit,
    onJumpToHighlight: (Int) -> Unit,
    onJumpToChapter: (VideoChapter) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val sceneFocused =
        highlights.highlights.filter {
            it.reason == HighlightReason.SCENE_CHANGE || it.reason == HighlightReason.VISUAL_INTEREST
        }
    val motion = highlights.highlights.filter { it.reason == HighlightReason.HIGH_MOTION }
    val audio = highlights.highlights.filter { it.reason == HighlightReason.AUDIO_PEAK }
    val faces = highlights.highlights.filter { it.reason == HighlightReason.FACE_ACTIVITY }
    val combined = highlights.highlights.filter { it.reason == HighlightReason.COMBINED }

    val tabs =
        buildList {
            add("All (${highlights.highlights.size})")
            if (sceneFocused.isNotEmpty()) add("Scenes (${sceneFocused.size})")
            if (motion.isNotEmpty()) add("Motion (${motion.size})")
            if (audio.isNotEmpty()) add("Audio (${audio.size})")
            if (faces.isNotEmpty()) add("Faces (${faces.size})")
            if (combined.isNotEmpty()) add("Best (${combined.size})")
            if (highlights.chapters.isNotEmpty()) add("Chapters (${highlights.chapters.size})")
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53)),
                            ),
                        ).padding(20.dp),
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(52.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text =
                                    context.getString(
                                        R.string.highlights_found,
                                        highlights.highlights.size,
                                    ),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                InfoChip("${highlights.highlightDuration / 1000}s")
                                InfoChip("${(highlights.confidenceScore * 100).toInt()}%")
                            }
                        }

                        IconButton(
                            onClick = onClose,
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onPlayHighlights,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            context.getString(R.string.highlights_watch),
                            color = Color(0xFFFF6B6B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                indicator = {},
                divider = {},
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        modifier =
                            Modifier
                                .padding(horizontal = 3.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selectedTab == index) {
                                        Color(0xFFFF6B6B).copy(alpha = 0.2f)
                                    } else {
                                        Color.Transparent
                                    },
                                ),
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                            )
                        },
                    )
                }
            }

            // Content
            val currentHighlights =
                when {
                    selectedTab == 0 -> highlights.highlights
                    tabs[selectedTab].startsWith("Scenes") -> sceneFocused
                    tabs[selectedTab].startsWith("Motion") -> motion
                    tabs[selectedTab].startsWith("Audio") -> audio
                    tabs[selectedTab].startsWith("Faces") -> faces
                    tabs[selectedTab].startsWith("Best") -> combined
                    tabs[selectedTab].startsWith("Chapters") -> null
                    else -> highlights.highlights
                }

            if (currentHighlights != null) {
                HighlightsList(currentHighlights, onJumpToHighlight)
            } else {
                ChaptersList(highlights.chapters, onJumpToChapter)
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.25f),
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun HighlightsList(
    highlights: List<HighlightSegment>,
    onJumpToHighlight: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(highlights) { index, highlight ->
            HighlightItem(highlight, index, onJumpToHighlight)
        }
    }
}

@Composable
private fun HighlightItem(
    highlight: HighlightSegment,
    index: Int,
    onJumpToHighlight: (Int) -> Unit,
) {
    val (icon, color, label) =
        when (highlight.reason) {
            HighlightReason.HIGH_MOTION ->
                Triple(
                    Icons.Default.FlashOn,
                    Color(0xFFFFB74D),
                    "High Motion",
                )

            HighlightReason.AUDIO_PEAK ->
                Triple(
                    Icons.Default.VolumeUp,
                    Color(0xFF64B5F6),
                    "Audio Peak",
                )

            HighlightReason.FACE_ACTIVITY ->
                Triple(
                    Icons.Default.Face,
                    Color(0xFFBA68C8),
                    "Face Activity",
                )

            HighlightReason.SCENE_CHANGE ->
                Triple(
                    Icons.Default.MovieFilter,
                    Color(0xFF4DD0E1),
                    "Scene Change",
                )

            HighlightReason.VISUAL_INTEREST ->
                Triple(
                    Icons.Default.Colorize,
                    Color(0xFFAED581),
                    "Visual Interest",
                )

            HighlightReason.COMBINED -> Triple(Icons.Default.Star, Color(0xFFFFD54F), "Best Moment")
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onJumpToHighlight(index) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = highlight.startTimeMs.formatTime(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Surface(
                        color = color.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(5.dp),
                    ) {
                        Text(
                            text = label,
                            color = color,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${highlight.durationMs / 1000}s duration",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                )

                if (highlight.keyFeatures.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(highlight.keyFeatures.take(3)) { feature ->
                            Surface(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(5.dp),
                            ) {
                                Text(
                                    text = feature.replace('_', ' ').lowercase(),
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { highlight.score },
                    modifier = Modifier.size(46.dp),
                    color = color,
                    trackColor = Color.White.copy(alpha = 0.1f),
                    strokeWidth = 3.dp,
                )
                Text(
                    text = "${(highlight.score * 100).toInt()}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ChaptersList(
    chapters: List<VideoChapter>,
    onJumpToChapter: (VideoChapter) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(chapters) { chapter ->
            ChapterItem(chapter, onClick = { onJumpToChapter(chapter) })
        }
    }
}

@Composable
private fun ChapterItem(
    chapter: VideoChapter,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(Color(0xFF4DD0E1).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = Color(0xFF4DD0E1),
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${chapter.startTimeMs.formatTime()} - ${chapter.endTimeMs.formatTime()}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                )
            }

            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Jump to chapter",
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Box(modifier = Modifier.padding(24.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(72.dp)
                            .background(Color(0xFFF44336).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(36.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = context.getString(R.string.highlights_error),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }

            IconButton(
                onClick = onClose,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
