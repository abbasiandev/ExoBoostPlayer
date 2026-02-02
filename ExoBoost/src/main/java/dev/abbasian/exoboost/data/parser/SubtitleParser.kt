package dev.abbasian.exoboost.data.parser

import dev.abbasian.exoboost.domain.model.ParsedSubtitle
import dev.abbasian.exoboost.domain.model.SubtitleCue
import dev.abbasian.exoboost.domain.model.SubtitleFormat
import dev.abbasian.exoboost.util.ExoBoostLogger
import java.util.regex.Pattern

class SubtitleParser(
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "SubtitleParser"
    }

    fun parse(
        content: String,
        format: SubtitleFormat,
    ): ParsedSubtitle? =
        try {
            when (format) {
                SubtitleFormat.SRT -> parseSrt(content)
                SubtitleFormat.VTT -> parseVtt(content)
                SubtitleFormat.ASS, SubtitleFormat.SSA -> parseAss(content)
                SubtitleFormat.TTML -> parseTtml(content)
            }
        } catch (e: Exception) {
            logger.error(TAG, "Failed to parse subtitle format: $format", e)
            null
        }

    private fun parseSrt(content: String): ParsedSubtitle {
        val cues = mutableListOf<SubtitleCue>()
        val blocks = content.trim().split("\n\n")

        for (block in blocks) {
            if (block.isBlank()) continue

            val lines = block.trim().split("\n")
            if (lines.size < 3) continue

            try {
                val timingLine = lines[1]
                val timePattern =
                    Pattern.compile(
                        "(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})",
                    )
                val matcher = timePattern.matcher(timingLine)

                if (matcher.find()) {
                    val startTime =
                        parseTime(
                            matcher.group(1).toInt(),
                            matcher.group(2).toInt(),
                            matcher.group(3).toInt(),
                            matcher.group(4).toInt(),
                        )
                    val endTime =
                        parseTime(
                            matcher.group(5).toInt(),
                            matcher.group(6).toInt(),
                            matcher.group(7).toInt(),
                            matcher.group(8).toInt(),
                        )

                    val text = lines.drop(2).joinToString("\n").trim()

                    if (text.isNotEmpty()) {
                        cues.add(
                            SubtitleCue(
                                startTimeMs = startTime,
                                endTimeMs = endTime,
                                text = text,
                            ),
                        )
                    }
                }
            } catch (e: Exception) {
                logger.warning(TAG, "Failed to parse SRT block: ${e.message}")
            }
        }

        return ParsedSubtitle(cues = cues, format = SubtitleFormat.SRT)
    }

    private fun parseVtt(content: String): ParsedSubtitle {
        val cues = mutableListOf<SubtitleCue>()
        val lines = content.lines()
        var i = 0

        while (i < lines.size && !lines[i].contains("-->")) {
            i++
        }

        while (i < lines.size) {
            val line = lines[i].trim()

            if (line.contains("-->")) {
                try {
                    val timePattern =
                        Pattern.compile(
                            "(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{3})",
                        )
                    val matcher = timePattern.matcher(line)

                    if (matcher.find()) {
                        val startTime =
                            parseTime(
                                matcher.group(1).toInt(),
                                matcher.group(2).toInt(),
                                matcher.group(3).toInt(),
                                matcher.group(4).toInt(),
                            )
                        val endTime =
                            parseTime(
                                matcher.group(5).toInt(),
                                matcher.group(6).toInt(),
                                matcher.group(7).toInt(),
                                matcher.group(8).toInt(),
                            )

                        i++
                        val textLines = mutableListOf<String>()
                        while (i < lines.size && lines[i].trim().isNotEmpty()) {
                            textLines.add(lines[i].trim())
                            i++
                        }

                        val text = textLines.joinToString("\n")
                        if (text.isNotEmpty()) {
                            cues.add(
                                SubtitleCue(
                                    startTimeMs = startTime,
                                    endTimeMs = endTime,
                                    text = text,
                                ),
                            )
                        }
                    }
                } catch (e: Exception) {
                    logger.warning(TAG, "Failed to parse VTT cue: ${e.message}")
                }
            }
            i++
        }

        return ParsedSubtitle(cues = cues, format = SubtitleFormat.VTT)
    }

    private fun parseAss(content: String): ParsedSubtitle {
        val cues = mutableListOf<SubtitleCue>()
        val lines = content.lines()

        for (line in lines) {
            if (line.startsWith("Dialogue:")) {
                try {
                    // Format: Dialogue: 0,0:00:10.50,0:00:13.00,Default,,0,0,0,,Text
                    val parts = line.substringAfter("Dialogue:").split(",")
                    if (parts.size < 10) continue

                    val startTime = parseAssTime(parts[1].trim())
                    val endTime = parseAssTime(parts[2].trim())
                    val text =
                        parts
                            .drop(9)
                            .joinToString(",")
                            .trim()
                            .replace("\\N", "\n")
                            .replace("\\n", "\n")

                    if (text.isNotEmpty()) {
                        cues.add(
                            SubtitleCue(
                                startTimeMs = startTime,
                                endTimeMs = endTime,
                                text = removeAssTags(text),
                            ),
                        )
                    }
                } catch (e: Exception) {
                    logger.warning(TAG, "Failed to parse ASS line: ${e.message}")
                }
            }
        }

        return ParsedSubtitle(cues = cues, format = SubtitleFormat.ASS)
    }

    private fun parseTtml(content: String): ParsedSubtitle {
        val cues = mutableListOf<SubtitleCue>()

        val pattern =
            Pattern.compile(
                "<p[^>]*begin=\"([^\"]+)\"[^>]*end=\"([^\"]+)\"[^>]*>([^<]+)</p>",
                Pattern.DOTALL,
            )
        val matcher = pattern.matcher(content)

        while (matcher.find()) {
            try {
                val startTime = parseTtmlTime(matcher.group(1))
                val endTime = parseTtmlTime(matcher.group(2))
                val text = matcher.group(3).trim()

                if (text.isNotEmpty()) {
                    cues.add(
                        SubtitleCue(
                            startTimeMs = startTime,
                            endTimeMs = endTime,
                            text = text,
                        ),
                    )
                }
            } catch (e: Exception) {
                logger.warning(TAG, "Failed to parse TTML cue: ${e.message}")
            }
        }

        return ParsedSubtitle(cues = cues, format = SubtitleFormat.TTML)
    }

    private fun parseTime(
        hours: Int,
        minutes: Int,
        seconds: Int,
        millis: Int,
    ): Long = (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L) + millis

    private fun parseAssTime(time: String): Long {
        val parts = time.split(":")
        if (parts.size != 3) return 0L

        val hours = parts[0].toIntOrNull() ?: 0
        val minutes = parts[1].toIntOrNull() ?: 0
        val secondsParts = parts[2].split(".")
        val seconds = secondsParts[0].toIntOrNull() ?: 0
        val centiseconds = secondsParts.getOrNull(1)?.toIntOrNull() ?: 0

        return (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L) + (centiseconds * 10L)
    }

    private fun parseTtmlTime(time: String): Long =
        when {
            time.endsWith("s") -> {
                val seconds = time.dropLast(1).toDoubleOrNull() ?: 0.0
                (seconds * 1000).toLong()
            }

            time.contains(":") -> {
                val parts = time.split(":")
                val hours = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val seconds = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000).toLong()
            }

            else -> {
                0L
            }
        }

    private fun removeAssTags(text: String): String = text.replace(Regex("\\{[^}]*\\}"), "").trim()

    fun detectFormat(content: String): SubtitleFormat? =
        when {
            content.trim().startsWith("WEBVTT") -> SubtitleFormat.VTT
            content.contains("[Script Info]") -> SubtitleFormat.ASS
            content.contains("<?xml") && content.contains("tt") -> SubtitleFormat.TTML
            content.contains(Regex("\\d+\n\\d{2}:\\d{2}:\\d{2},\\d{3}")) -> SubtitleFormat.SRT
            else -> null
        }
}
