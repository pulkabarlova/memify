package com.codekotliners.memify.features.create.presentation.viewmodel

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import com.codekotliners.memify.core.theme.FontFamilyImpact
import com.codekotliners.memify.features.create.domain.CanvasElement
import com.codekotliners.memify.features.create.domain.ColoredLine
import com.codekotliners.memify.features.create.domain.TextElement
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

@Suppress("detekt.TooManyFunctions")
@Stable
@HiltViewModel
open class CanvasViewModel @Inject constructor() : ViewModel() {
    private val history = mutableStateListOf<List<CanvasElement>>()
    private val future = mutableStateListOf<List<CanvasElement>>()

    val canvasElements = mutableStateListOf<CanvasElement>()
    val currentLine = mutableStateListOf<Offset>()
    val currentLineWidth = mutableFloatStateOf(50f)
    val currentLineColor = mutableStateOf(Color.Black)

    var currentText by mutableStateOf("")
    var currentTextColor = mutableStateOf(Color.Black)
    var currentTextSize = mutableFloatStateOf(24f)
    val currentFontFamily: MutableState<FontFamily> = mutableStateOf(FontFamilyImpact)
    val currentFontWeight = mutableStateOf(FontWeight.Normal)
    val currentTextAlign = mutableStateOf(TextAlign.Center)
    var currentTextHasOutline by mutableStateOf(false)

    var selectedElementId by mutableStateOf<Long?>(null)
        private set

    var editingTextId by mutableStateOf<Long?>(null)
        private set

    private var pendingTextPosition = Offset.Zero

    var showTextInput by mutableStateOf(false)
    var showTextPreview by mutableStateOf(false)
    var showColors by mutableStateOf(false)
    var showFonts by mutableStateOf(false)
    var showWeights by mutableStateOf(false)

    var imageWidth by mutableFloatStateOf(1f)
    var imageHeight by mutableFloatStateOf(1f)

    var isPaintingEnabled by mutableStateOf(false)
    var isWritingEnabled by mutableStateOf(false)

    var imageUrl by mutableStateOf<String?>(null)

    val imagePickerLauncher = mutableStateOf<ActivityResultLauncher<Intent>?>(null)

    val selectedElement: CanvasElement?
        get() {
            val id = selectedElementId ?: return null
            return canvasElements.find { it.id == id }
        }

    val selectedTextElement: TextElement?
        get() = selectedElement as? TextElement

    val selectedDrawingElement: ColoredLine?
        get() = selectedElement as? ColoredLine

    fun addPointToCurrentLine(point: Offset) {
        currentLine.add(point)
    }

    fun finalizeCurrentLine() {
        if (currentLine.size > 1) {
            pushHistory()
            canvasElements.add(
                ColoredLine(
                    points = currentLine.toList(),
                    color = currentLineColor.value,
                    strokeWidth = currentLineWidth.floatValue,
                ),
            )
        }
        currentLine.clear()
    }

    fun togglePaintingMode() {
        if (isPaintingEnabled) {
            isPaintingEnabled = false
            deselectElement()
            showColors = false
        } else {
            isPaintingEnabled = true
            isWritingEnabled = false
            deselectElement()
            showColors = false
            showFonts = false
            showWeights = false
        }
    }

    fun toggleWritingMode() {
        if (isWritingEnabled) {
            isWritingEnabled = false
            deselectElement()
            showColors = false
            showFonts = false
            showWeights = false
        } else {
            isWritingEnabled = true
            isPaintingEnabled = false
        }
    }

    fun startWriting(position: Offset? = null) {
        if (selectedElementId != null) {
            deselectElement()
            return
        }
        isPaintingEnabled = false
        isWritingEnabled = true
        showColors = false
        showFonts = false
        showWeights = false
        editingTextId = null
        currentText = ""
        pendingTextPosition = position ?: Offset(imageWidth / 4f, imageHeight / 3f)
        showTextInput = true
    }

    fun startEditingSelectedText() {
        val element = selectedTextElement ?: return
        editingTextId = element.id
        currentText = element.text
        showTextInput = true
    }

    fun selectElement(element: CanvasElement) {
        selectedElementId = element.id
        showColors = false
        showFonts = false
        showWeights = false
        when (element) {
            is TextElement -> {
                isWritingEnabled = true
                isPaintingEnabled = false
                currentTextColor.value = element.color
                currentTextSize.floatValue = element.size
                currentFontFamily.value = element.fontFamily
                currentFontWeight.value = element.fontWeight
                currentTextAlign.value = element.textAlign
                currentTextHasOutline = element.hasOutline
            }
            is ColoredLine -> {
                isPaintingEnabled = true
                isWritingEnabled = false
                currentLineColor.value = element.color
            }
        }
    }

    fun deselectElement() {
        selectedElementId = null
    }

    fun clearCanvas() {
        pushHistory()
        canvasElements.clear()
        currentLine.clear()
        deselectElement()
    }

    fun undo() {
        if (history.isNotEmpty()) {
            future.add(canvasElements.toList())
            canvasElements.clear()
            canvasElements.addAll(history.removeAt(history.lastIndex))
            deselectElement()
        }
    }

    fun redo() {
        if (future.isNotEmpty()) {
            history.add(canvasElements.toList())
            canvasElements.clear()
            canvasElements.addAll(future.removeAt(future.lastIndex))
            deselectElement()
        }
    }

    fun finishWriting() {
        val textWithoutBlankLines =
            currentText
                .lineSequence()
                .filter { it.isNotBlank() }
                .joinToString("\n")
        val id = editingTextId
        if (id != null) {
            val index = canvasElements.indexOfFirst { it.id == id }
            val existing = index.takeIf { it >= 0 }?.let { canvasElements[it] as? TextElement }
            if (existing != null && textWithoutBlankLines.isNotEmpty() && textWithoutBlankLines != existing.text) {
                pushHistory()
                canvasElements[index] = existing.copy(text = textWithoutBlankLines)
            }
        } else if (textWithoutBlankLines.isNotEmpty()) {
            pushHistory()
            val newElement =
                TextElement(
                    text = textWithoutBlankLines,
                    color = currentTextColor.value,
                    size = currentTextSize.floatValue,
                    fontFamily = currentFontFamily.value,
                    fontWeight = currentFontWeight.value,
                    textAlign = currentTextAlign.value,
                    hasOutline = currentTextHasOutline,
                    position = pendingTextPosition,
                )
            canvasElements.add(newElement)
            selectedElementId = newElement.id
        }
        showTextInput = false
        editingTextId = null
        currentText = ""
    }

    fun setTextColor(color: Color) {
        currentTextColor.value = color
        updateSelectedText { it.copy(color = color) }
    }

    fun setFontFamily(fontFamily: FontFamily) {
        currentFontFamily.value = fontFamily
        updateSelectedText { it.copy(fontFamily = fontFamily) }
    }

    fun setFontWeight(fontWeight: FontWeight) {
        currentFontWeight.value = fontWeight
        updateSelectedText { it.copy(fontWeight = fontWeight) }
    }

    fun setTextAlign(textAlign: TextAlign) {
        currentTextAlign.value = textAlign
        updateSelectedText { it.copy(textAlign = textAlign) }
    }

    fun toggleTextOutline() {
        val newValue = !currentTextHasOutline
        currentTextHasOutline = newValue
        updateSelectedText { it.copy(hasOutline = newValue) }
    }

    fun setLineColor(color: Color) {
        currentLineColor.value = color
        val id = selectedElementId ?: return
        val index = canvasElements.indexOfFirst { it.id == id }
        if (index >= 0) {
            val existing = canvasElements[index] as? ColoredLine ?: return
            canvasElements[index] = existing.copy(color = color)
        }
    }

    fun transformText(
        elementId: Long,
        positionDelta: Offset,
        zoom: Float,
        rotationDelta: Float,
    ) {
        val index = canvasElements.indexOfFirst { it.id == elementId }
        if (index < 0) return
        val existing = canvasElements[index] as? TextElement ?: return
        val newSize = (existing.size * zoom).coerceIn(MIN_TEXT_SIZE, MAX_TEXT_SIZE)
        val positionDeltaInCanvas = rotateOffset(positionDelta, existing.rotationDegrees)
        canvasElements[index] =
            existing.copy(
                position = existing.position + positionDeltaInCanvas,
                size = newSize,
                rotationDegrees = normalizeRotation(existing.rotationDegrees + rotationDelta),
            )
        if (selectedElementId == elementId) {
            currentTextSize.floatValue = newSize
        }
    }

    fun transformDrawing(elementId: Long, positionDelta: Offset, zoom: Float) {
        val index = canvasElements.indexOfFirst { it.id == elementId }
        if (index < 0) return
        val existing = canvasElements[index] as? ColoredLine ?: return
        val newScale = (existing.scale * zoom).coerceIn(MIN_DRAWING_SCALE, MAX_DRAWING_SCALE)
        canvasElements[index] =
            existing.copy(
                position = existing.position + positionDelta,
                scale = newScale,
            )
    }

    fun rotateText(elementId: Long, rotationDelta: Float) {
        val index = canvasElements.indexOfFirst { it.id == elementId }
        if (index < 0) return
        val existing = canvasElements[index] as? TextElement ?: return
        canvasElements[index] =
            existing.copy(
                rotationDegrees = normalizeRotation(existing.rotationDegrees + rotationDelta),
            )
    }

    fun deleteSelectedElement() {
        val id = selectedElementId ?: return
        val index = canvasElements.indexOfFirst { it.id == id }
        if (index >= 0) {
            pushHistory()
            canvasElements.removeAt(index)
        }
        selectedElementId = null
    }

    fun duplicateSelectedText() {
        val existing = selectedTextElement ?: return
        pushHistory()
        val copy =
            existing.copy(
                id = System.currentTimeMillis(),
                position = existing.position + Offset(DUPLICATE_OFFSET_PX, DUPLICATE_OFFSET_PX),
            )
        canvasElements.add(copy)
        selectedElementId = copy.id
    }

    private fun updateSelectedText(transform: (TextElement) -> TextElement) {
        val id = selectedElementId ?: return
        val index = canvasElements.indexOfFirst { it.id == id }
        if (index >= 0) {
            val existing = canvasElements[index] as? TextElement ?: return
            canvasElements[index] = transform(existing)
        }
    }

    private fun pushHistory() {
        history.add(canvasElements.toList())
        future.clear()
    }

    fun pickImageFromGallery() {
        val intent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
            }
        imagePickerLauncher.value?.launch(intent)
    }

    fun handleImageSelection(uri: Uri?) {
        uri?.let {
            imageUrl = it.toString()
        }
    }

    companion object {
        private const val MIN_TEXT_SIZE = 8f
        private const val MAX_TEXT_SIZE = 220f
        private const val DUPLICATE_OFFSET_PX = 28f
        private const val MIN_DRAWING_SCALE = 0.2f
        private const val MAX_DRAWING_SCALE = 6f
        private const val FULL_ROTATION_DEGREES = 360f

        private fun normalizeRotation(rotationDegrees: Float): Float =
            ((rotationDegrees % FULL_ROTATION_DEGREES) + FULL_ROTATION_DEGREES) % FULL_ROTATION_DEGREES

        private fun rotateOffset(offset: Offset, rotationDegrees: Float): Offset {
            val radians = Math.toRadians(rotationDegrees.toDouble())
            val cosine = cos(radians).toFloat()
            val sine = sin(radians).toFloat()
            return Offset(
                x = offset.x * cosine - offset.y * sine,
                y = offset.x * sine + offset.y * cosine,
            )
        }
    }
}
