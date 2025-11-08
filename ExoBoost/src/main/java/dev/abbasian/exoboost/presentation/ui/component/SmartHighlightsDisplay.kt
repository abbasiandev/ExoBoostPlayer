package dev.abbasian.exoboost.presentation.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    onJumpToChapter: (VideoChapter) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = highlightsState !is HighlightsState.Idle,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier,
    ) {
        when (highlightsState) {
            is HighlightsState.Analyzing -> {
                highlightsAnalyzing(
                    progress = highlightsState.progress,
                    onClose = onClose,
                )
            }

            is HighlightsState.Success -> {
                highlightsSuccess(
                    highlights = highlightsState.highlights,
                    onPlayHighlights = onPlayHighlights,
                    onJumpToChapter = onJumpToChapter,
                    onClose = onClose,
                )
            }

            is HighlightsState.Error -> {
                highlightsError(
                    message = highlightsState.message,
                    onClose = onClose,
                )
            }

            else -> {}
        }
    }
}

@Composable
private fun highlightsAnalyzing(
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
        colors =
            CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.95f),
            ),
    ) {
        Box(
            modifier = Modifier.padding(24.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = context.getString(R.string.highlights_analyzing),
                    tint = Color(0xFFFF6B6B),
                    modifier = Modifier.size(56.dp),
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = context.getString(R.string.highlights_analyzing),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = progress,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFFF6B6B),
                    trackColor = Color.White.copy(alpha = 0.2f),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = context.getString(R.string.highlights_analyzing_description),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                )
            }

            IconButton(
                onClick = onClose,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun highlightsSuccess(
    highlights: VideoHighlights,
    onPlayHighlights: () -> Unit,
    onJumpToChapter: (VideoChapter) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs =
        listOf(
            context.getString(R.string.highlights_tab_highlights),
            context.getString(R.string.highlights_tab_chapters),
        )

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.95f),
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors =
                                    listOf(
                                        Color(0xFFFF6B6B),
                                        Color(0xFFFF8E53),
                                    ),
                            ),
                        )
                        .padding(20.dp),
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )

                        Spacer(modifier = Modifier.width(12.dp))

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

                            Text(
                                text =
                                    context.getString(
                                        R.string.highlights_summary,
                                        highlights.highlightDuration / 1000,
                                        highlights.originalDuration / 1000,
                                    ),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                            )
                        }

                        IconButton(
                            onClick = onClose,
                            modifier =
                                Modifier.background(
                                    Color.White.copy(alpha = 0.2f),
                                    CircleShape,
                                ),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onPlayHighlights,
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                            ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFFFF6B6B),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            context.getString(R.string.highlights_watch),
                            color = Color(0xFFFF6B6B),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }

            when (selectedTab) {
                0 -> highlightsList(highlights.highlights)
                1 -> chaptersList(highlights.chapters, onJumpToChapter)
            }
        }
    }
}

@Composable
private fun highlightsList(highlights: List<HighlightSegment>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(highlights) { highlight ->
            highlightItem(highlight)
        }
    }
}

@Composable
private fun highlightItem(highlight: HighlightSegment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f),
            ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon =
                when (highlight.reason) {
                    HighlightReason.HIGH_MOTION -> Icons.Default.FlashOn
                    HighlightReason.AUDIO_PEAK -> Icons.Default.VolumeUp
                    HighlightReason.FACE_ACTIVITY -> Icons.Default.Face
                    HighlightReason.SCENE_CHANGE -> Icons.Default.MovieFilter
                    HighlightReason.VISUAL_INTEREST -> Icons.Default.Colorize
                    HighlightReason.COMBINED -> Icons.Default.Star
                }

            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(32.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = highlight.startTimeMs.formatTime(),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = "${highlight.durationMs / 1000}s • ${
                        highlight.reason.name.lowercase().replace('_', ' ')
                    }",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )

                if (highlight.keyFeatures.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(highlight.keyFeatures) { feature ->
                            Text(
                                text = feature.replace('_', ' '),
                                color = Color(0xFFFF6B6B),
                                fontSize = 10.sp,
                                modifier =
                                    Modifier
                                        .background(
                                            Color(0xFFFF6B6B).copy(alpha = 0.2f),
                                            RoundedCornerShape(4.dp),
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            CircularProgressIndicator(
                progress = { highlight.score },
                modifier = Modifier.size(40.dp),
                color = Color(0xFFFF6B6B),
                trackColor = Color.White.copy(alpha = 0.2f),
                strokeWidth = 4.dp,
            )
        }
    }
}

@Composable
private fun chaptersList(
    chapters: List<VideoChapter>,
    onJumpToChapter: (VideoChapter) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(chapters) { chapter ->
            chapterItem(chapter, onClick = { onJumpToChapter(chapter) })
        }
    }
}

@Composable
private fun chapterItem(
    chapter: VideoChapter,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f),
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Bookmark,
                contentDescription = null,
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )

                Text(
                    text = "${chapter.startTimeMs.formatTime()} - ${chapter.endTimeMs.formatTime()}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
            }

            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Jump to chapter",
                tint = Color.White.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun highlightsError(
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
        colors =
            CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.95f),
            ),
    ) {
        Box(
            modifier = Modifier.padding(24.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = context.getString(R.string.highlights_error),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                )
            }

            IconButton(
                onClick = onClose,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                )
            }
        }
    }
}
