package dev.abbasian.exoboost.presentation.ui.component

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abbasian.exoboost.data.local.store.EqualizerPreferencesManager
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun glassyEqualizer(
    modifier: Modifier = Modifier,
    config: MediaPlayerConfig.GlassyUIConfig = MediaPlayerConfig.GlassyUIConfig(),
    barCount: Int = 8,
    onEqualizerChange: ((List<Float>) -> Unit)? = null,
    preferencesManager: EqualizerPreferencesManager = koinInject(),
    frequencyLabels: List<String> =
        listOf(
            "60Hz",
            "170Hz",
            "310Hz",
            "600Hz",
            "1kHz",
            "3kHz",
            "6kHz",
            "12kHz",
        ),
) {
    val scope = rememberCoroutineScope()

    val equalizerValues =
        remember {
            mutableStateListOf<Float>().apply {
                repeat(barCount) { add(0.5f) }
            }
        }

    val customPresets by preferencesManager.customPresets.collectAsState(initial = emptyList())
    val currentPresetName by preferencesManager.currentPresetName.collectAsState(initial = "")
    val storedValues by preferencesManager.currentValues.collectAsState(initial = List(8) { 0.5f })

    var showCustomPresetDialog by remember { mutableStateOf(false) }
    var selectedPresetForEdit by remember { mutableStateOf<CustomPreset?>(null) }

    LaunchedEffect(storedValues) {
        if (storedValues.isNotEmpty()) {
            storedValues.forEachIndexed { index, value ->
                if (index < equalizerValues.size) {
                    equalizerValues[index] = value
                }
            }
            onEqualizerChange?.invoke(storedValues)
        }
    }

    glassyContainer(
        config =
            config.copy(
                backgroundOpacity = config.backgroundOpacity * 0.8f,
            ),
        contentPadding = 16.dp,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                glassyActionButton(
                    text = "Custom",
                    onClick = {
                        scope.launch {
                            preferencesManager.saveCurrentPresetName("")
                        }
                        showCustomPresetDialog = true
                    },
                )
                glassyActionButton(
                    text = "Save Preset",
                    onClick = {
                        showCustomPresetDialog = true
                    },
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(barCount) { index ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        // frequency label
                        Text(
                            text = frequencyLabels.getOrElse(index) { "${index + 1}" },
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            maxLines = 1,
                        )

                        // vertical slider
                        Box(
                            modifier =
                                Modifier
                                    .width(40.dp)
                                    .height(120.dp)
                                    .background(
                                        brush =
                                            Brush.verticalGradient(
                                                colors =
                                                    listOf(
                                                        Color.White.copy(alpha = 0.1f),
                                                        Color.White.copy(alpha = 0.05f),
                                                    ),
                                            ),
                                        shape = RoundedCornerShape(20.dp),
                                    ).border(
                                        width = 0.5.dp,
                                        color = Color.White.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(20.dp),
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            verticalSlider(
                                value = equalizerValues[index],
                                onValueChange = { newValue ->
                                    equalizerValues[index] = newValue
                                    val updatedValues = equalizerValues.toList()
                                    onEqualizerChange?.invoke(updatedValues)

                                    scope.launch {
                                        preferencesManager.saveEqualizerState(
                                            presetName = "",
                                            values = updatedValues,
                                            customPresets = customPresets,
                                        )
                                    }
                                },
                                valueRange = 0f..1f,
                                modifier =
                                    Modifier
                                        .height(100.dp)
                                        .width(20.dp),
                            )
                        }

                        // dB
                        val dbValue = ((equalizerValues[index] - 0.5f) * 24).toInt()
                        Text(
                            text = "${if (dbValue > 0) "+" else ""}${dbValue}dB",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf("Flat", "Rock", "Pop", "Jazz").forEach { preset ->
                    glassyPresetButton(
                        text = preset,
                        onClick = {
                            val presetValues = getPresetValues(preset)
                            presetValues.forEachIndexed { index, value ->
                                if (index < equalizerValues.size) {
                                    equalizerValues[index] = value
                                }
                            }

                            onEqualizerChange?.invoke(presetValues)

                            scope.launch {
                                preferencesManager.saveEqualizerState(
                                    presetName = preset,
                                    values = presetValues,
                                    customPresets = customPresets,
                                )
                            }
                        },
                    )
                }
            }

            if (customPresets.isNotEmpty()) {
                Text(
                    text = "Custom Presets",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp),
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    items(customPresets) { preset ->
                        customPresetItem(
                            preset = preset,
                            onApply = {
                                preset.values.forEachIndexed { index, value ->
                                    if (index < equalizerValues.size) {
                                        equalizerValues[index] = value
                                    }
                                }
                                onEqualizerChange?.invoke(equalizerValues.toList())
                            },
                            onEdit = {
                                selectedPresetForEdit = preset
                                showCustomPresetDialog = true
                            },
                            onDelete = {
                                scope.launch {
                                    val updatedPresets =
                                        customPresets.toMutableList().apply {
                                            remove(preset)
                                        }
                                    val newPresetName =
                                        if (currentPresetName == preset.name) "" else currentPresetName
                                    preferencesManager.saveEqualizerState(
                                        presetName = newPresetName,
                                        values = equalizerValues.toList(),
                                        customPresets = updatedPresets,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showCustomPresetDialog) {
        customPresetDialog(
            currentValues = equalizerValues.toList(),
            existingPreset = selectedPresetForEdit,
            onSave = { name, values ->
                scope.launch {
                    val updatedPresets = customPresets.toMutableList()

                    if (selectedPresetForEdit != null) {
                        val index =
                            updatedPresets.indexOfFirst { it.name == selectedPresetForEdit!!.name }
                        if (index >= 0) {
                            updatedPresets[index] = CustomPreset(name, values)
                        }
                    } else {
                        updatedPresets.add(CustomPreset(name, values))
                    }

                    preferencesManager.saveEqualizerState(
                        presetName = name,
                        values = values,
                        customPresets = updatedPresets,
                    )
                }

                showCustomPresetDialog = false
                selectedPresetForEdit = null
            },
            onDismiss = {
                showCustomPresetDialog = false
                selectedPresetForEdit = null
            },
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun verticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier =
            modifier
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            val heightPx = size.height.toFloat()
                            val normalizedY = (heightPx - offset.y) / heightPx
                            val newValue =
                                (valueRange.start + normalizedY * (valueRange.endInclusive - valueRange.start))
                                    .coerceIn(valueRange.start, valueRange.endInclusive)
                            onValueChange(newValue)
                        },
                        onDragEnd = {
                            isDragging = false
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                    ) { _, dragAmount ->
                        val heightPx = size.height.toFloat()
                        val deltaValue =
                            -dragAmount.y / heightPx * (valueRange.endInclusive - valueRange.start)
                        val newValue =
                            (value + deltaValue).coerceIn(valueRange.start, valueRange.endInclusive)
                        onValueChange(newValue)
                    }
                }.pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val heightPx = size.height.toFloat()
                        val normalizedY = (heightPx - offset.y) / heightPx
                        val newValue =
                            (valueRange.start + normalizedY * (valueRange.endInclusive - valueRange.start))
                                .coerceIn(valueRange.start, valueRange.endInclusive)
                        onValueChange(newValue)
                    }
                },
    ) {
        // Background track
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(10.dp),
                    ).border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.3f),
                        RoundedCornerShape(10.dp),
                    ),
        )

        // Active fill
        val normalizedValue =
            (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
        val thumbY = maxHeight * (1f - normalizedValue)
        val activeHeight = maxHeight - thumbY

        if (activeHeight > 0.dp) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .width(maxWidth)
                        .height(activeHeight)
                        .background(
                            brush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            Color.White.copy(alpha = 0.6f),
                                            Color.White.copy(alpha = 0.3f),
                                        ),
                                ),
                            shape = RoundedCornerShape(10.dp),
                        ),
            )
        }

        // Thumb
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = thumbY - 12.dp)
                    .size(24.dp)
                    .background(
                        brush =
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        Color.White.copy(alpha = if (isDragging) 1f else 0.9f),
                                        Color.White.copy(alpha = if (isDragging) 0.8f else 0.7f),
                                    ),
                            ),
                        shape = CircleShape,
                    ).border(
                        width = if (isDragging) 2.dp else 1.dp,
                        color = Color.White.copy(alpha = if (isDragging) 1f else 0.8f),
                        shape = CircleShape,
                    ).shadow(
                        elevation = if (isDragging) 8.dp else 4.dp,
                        shape = CircleShape,
                    ),
        )
    }
}

private fun getPresetValues(preset: String): List<Float> =
    when (preset) {
        "Flat" -> listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
        "Rock" -> listOf(0.6f, 0.5f, 0.4f, 0.4f, 0.5f, 0.7f, 0.8f, 0.8f)
        "Pop" -> listOf(0.4f, 0.6f, 0.7f, 0.7f, 0.5f, 0.4f, 0.5f, 0.6f)
        "Jazz" -> listOf(0.7f, 0.6f, 0.5f, 0.4f, 0.4f, 0.5f, 0.6f, 0.7f)
        else -> listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
    }

@Composable
private fun customPresetItem(
    preset: CustomPreset,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        glassyPresetButton(
            text = preset.name,
            onClick = onApply,
            onLongClick = { showMenu = true },
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier =
                Modifier.background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Black.copy(alpha = 0.9f),
                                    Color.Black.copy(alpha = 0.8f),
                                ),
                        ),
                    shape = RoundedCornerShape(8.dp),
                ),
        ) {
            DropdownMenuItem(
                text = { Text("Edit", color = Color.White) },
                onClick = {
                    showMenu = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete", color = Color.Red.copy(alpha = 0.8f)) },
                onClick = {
                    showMenu = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun customPresetDialog(
    currentValues: List<Float>,
    existingPreset: CustomPreset?,
    onSave: (String, List<Float>) -> Unit,
    onDismiss: () -> Unit,
) {
    var presetName by remember {
        mutableStateOf(existingPreset?.name ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingPreset != null) "Edit Preset" else "Save Preset",
                color = Color.White,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Enter a name for this equalizer preset:",
                    color = Color.White.copy(alpha = 0.8f),
                )

                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    placeholder = { Text("Preset name", color = Color.White.copy(alpha = 0.5f)) },
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.7f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        ),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            glassyActionButton(
                text = "Save",
                onClick = {
                    if (presetName.isNotBlank()) {
                        onSave(presetName, currentValues)
                    }
                },
            )
        },
        dismissButton = {
            glassyActionButton(
                text = "Cancel",
                onClick = onDismiss,
            )
        },
        containerColor = Color.Black.copy(alpha = 0.9f),
    )
}

@Composable
private fun glassyPresetButton(
    text: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(
                    brush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.White.copy(alpha = 0.05f),
                                ),
                        ),
                    shape = RoundedCornerShape(12.dp),
                ).border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                ).then(
                    if (onLongClick != null) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onClick() },
                                onLongPress = { onLongClick() },
                            )
                        }
                    } else {
                        Modifier.clickable { onClick() }
                    },
                ).padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun glassyActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(
                    brush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.2f),
                                    Color.White.copy(alpha = 0.1f),
                                ),
                        ),
                    shape = RoundedCornerShape(8.dp),
                ).border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                ).clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

data class CustomPreset(
    val name: String,
    val values: List<Float>,
)
