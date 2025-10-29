package dev.abbasian.exoboost.util

fun Long.formatTime(): String {
    val seconds = (this / 1000) % 60
    val minutes = (this / 1000 / 60) % 60
    val hours = this / 1000 / 60 / 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
