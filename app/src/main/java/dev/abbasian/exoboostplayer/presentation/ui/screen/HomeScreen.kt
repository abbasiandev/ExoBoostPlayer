package dev.abbasian.exoboostplayer.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboostplayer.presentation.viewmodel.MediaItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMediaSelected: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val sampleVideos = listOf(
        MediaItem(
            title = "Big Buck Bunny (MP4)",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            description = "ویدیوی تست کیفیت HD"
        ),
        MediaItem(
            title = "Classical Piano (MP3)",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            description = "قطعه پیانو کلاسیک برای تست"
        ),
        MediaItem(
            title = "HLS Big Buck Bunny",
            url = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
            description = "Public HLS stream from Apple for testing"
        ),
        MediaItem(
            title = "DASH Stream",
            url = "https://bitmovin-a.akamaihd.net/content/sintel/sintel.mpd",
            description = "DASH adaptive stream"
        ),
        MediaItem(
            title = "Elephant Dream (MP4)",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            description = "انیمیشن کوتاه با کیفیت بالا"
        ),
        MediaItem(
            title = "Tears of Steel (MP4)",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            description = "فیلم کوتاه علمی-تخیلی"
        ),
        MediaItem(
            title = "Sintel (MP4)",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            description = "انیمیشن فانتزی کوتاه"
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        TopAppBar(
            title = {
                Text(
                    "ExoBoost Library Player",
                    fontWeight = FontWeight.Bold
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "یکی از ویدیوهای زیر را برای تست انتخاب کنید:",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sampleVideos) { media ->
                MediaItemCard(
                    media = media,
                    onClick = { onMediaSelected(media) }
                )
            }

            item {
                FeatureCard()
            }
        }
    }
}

@Composable
private fun MediaItemCard(
    media: MediaItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = media.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = media.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = media.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun FeatureCard() {
    Spacer(modifier = Modifier.height(16.dp))

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "ویژگی‌های این پخش کننده:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            val features = listOf(
                "✅ معماری Clean Architecture با Koin DI",
                "✅ حل مشکلات SSL و شبکه",
                "✅ مدیریت خطاها و تلاش مجدد خودکار",
                "✅ کنترل‌های زیبا و انیمیشن‌دار",
                "✅ پشتیبانی از کش برای عملکرد بهتر",
                "✅ کنترل حجم و روشنایی با ژست",
                "✅ سازگار با چرخه زندگی Android",
                "✅ رابط کاربری مدرن با Material 3",
                "✅ جداسازی کامل Presentation، Domain، Data"
            )

            features.forEach { feature ->
                Text(
                    text = feature,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}