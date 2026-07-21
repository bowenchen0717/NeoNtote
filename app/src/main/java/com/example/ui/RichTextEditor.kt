package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

data class CharStyle(
    val bold: Boolean? = null,
    val italic: Boolean? = null,
    val underline: Boolean? = null,
    val fontSize: Float? = null,
    val color: Color? = null,
    val backgroundColor: Color? = null,
    val fontFamily: String? = null
)

data class Span(
    val start: Int,
    val end: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val fontSize: Float? = null,
    val color: Color? = null,
    val backgroundColor: Color? = null,
    val fontFamily: String? = null
) {
    fun toCharStyle() = CharStyle(
        bold = bold,
        italic = italic,
        underline = underline,
        fontSize = fontSize,
        color = color,
        backgroundColor = backgroundColor,
        fontFamily = fontFamily
    )
}

data class Document(
    val id: Int?,
    val title: String,
    val spans: List<Span>
)

data class TextDiff(
    val start: Int,
    val oldEnd: Int,
    val newEnd: Int,
    val insertedText: String
)

fun computeTextDiff(
    oldText: String,
    newText: String,
    oldSelection: TextRange,
    newSelection: TextRange
): TextDiff {
    if (!oldSelection.collapsed) {
        val start = oldSelection.min
        val oldEnd = oldSelection.max
        val insLen = newText.length - (oldText.length - (oldEnd - start))
        val newEnd = start + insLen.coerceAtLeast(0)
        val insertedText = if (insLen > 0 && start <= newText.length && newEnd <= newText.length) {
            newText.substring(start, newEnd)
        } else {
            ""
        }
        return TextDiff(start, oldEnd, newEnd, insertedText)
    } else {
        if (newText.length > oldText.length) {
            val insLen = newText.length - oldText.length
            val start = oldSelection.start
            val newEnd = start + insLen
            val insertedText = if (start <= newText.length && newEnd <= newText.length) {
                newText.substring(start, newEnd)
            } else {
                ""
            }
            return TextDiff(start, start, newEnd, insertedText)
        } else {
            val delLen = oldText.length - newText.length
            val start = if (newSelection.start < oldSelection.start) {
                newSelection.start
            } else {
                oldSelection.start
            }
            val oldEnd = start + delLen
            return TextDiff(start, oldEnd, start, "")
        }
    }
}

fun applyDiffToSpans(spans: List<Span>, diff: TextDiff, activeStyle: CharStyle): List<Span> {
    val result = mutableListOf<Span>()
    val shift = diff.newEnd - diff.oldEnd
    
    spans.forEach { span ->
        if (span.end <= diff.start) {
            result.add(span)
        } else if (span.start >= diff.oldEnd) {
            result.add(span.copy(start = span.start + shift, end = span.end + shift))
        } else {
            val leftStart = span.start
            val leftEnd = diff.start
            val rightStart = diff.oldEnd
            val rightEnd = span.end
            
            if (leftStart < leftEnd) {
                result.add(span.copy(start = leftStart, end = leftEnd))
            }
            if (rightStart < rightEnd) {
                result.add(span.copy(start = rightStart + shift, end = rightEnd + shift))
            }
        }
    }
    
    if (diff.insertedText.isNotEmpty()) {
        val insertedSpan = Span(
            start = diff.start,
            end = diff.newEnd,
            bold = activeStyle.bold ?: false,
            italic = activeStyle.italic ?: false,
            underline = activeStyle.underline ?: false,
            fontSize = activeStyle.fontSize,
            color = activeStyle.color,
            backgroundColor = activeStyle.backgroundColor,
            fontFamily = activeStyle.fontFamily
        )
        result.add(insertedSpan)
    }
    
    return result
}

class Editor(initialText: String, initialSpans: List<Span>) {
    var text by mutableStateOf(initialText)
    var spans by mutableStateOf(initialSpans)
    var selection by mutableStateOf(TextRange.Zero)
    
    val cursor: Int
        get() = selection.start

    private val undoStack = java.util.Stack<EditorHistoryState>()
    private val redoStack = java.util.Stack<EditorHistoryState>()

    init {
        saveToHistory()
    }

    data class EditorHistoryState(
        val text: String,
        val spans: List<Span>,
        val selection: TextRange
    )

    fun saveToHistory() {
        val state = EditorHistoryState(text, spans, selection)
        if (undoStack.isEmpty() || undoStack.peek() != state) {
            undoStack.push(state)
            redoStack.clear()
        }
    }

    fun undo(): Boolean {
        if (undoStack.size > 1) {
            val currentState = EditorHistoryState(text, spans, selection)
            redoStack.push(currentState)
            undoStack.pop()
            val prevState = undoStack.peek()
            text = prevState.text
            spans = prevState.spans
            selection = prevState.selection
            return true
        }
        return false
    }

    fun redo(): Boolean {
        if (redoStack.isNotEmpty()) {
            val nextState = redoStack.pop()
            undoStack.push(EditorHistoryState(text, spans, selection))
            text = nextState.text
            spans = nextState.spans
            selection = nextState.selection
            return true
        }
        return false
    }

    fun insert(insertedText: String, atIndex: Int, activeStyle: CharStyle) {
        val safeIndex = atIndex.coerceIn(0, text.length)
        val diff = TextDiff(safeIndex, safeIndex, safeIndex + insertedText.length, insertedText)
        
        text = text.substring(0, safeIndex) + insertedText + text.substring(safeIndex)
        spans = applyDiffToSpans(spans, diff, activeStyle)
        mergeSpans()
        saveToHistory()
    }

    fun delete(start: Int, end: Int) {
        val s = start.coerceIn(0, text.length)
        val e = end.coerceIn(s, text.length)
        if (s == e) return

        val diff = TextDiff(s, e, s, "")
        text = text.substring(0, s) + text.substring(e)
        spans = applyDiffToSpans(spans, diff, CharStyle())
        mergeSpans()
        saveToHistory()
    }

    fun applyStyle(start: Int, end: Int, style: CharStyle) {
        val s = start.coerceIn(0, text.length)
        val e = end.coerceIn(s, text.length)
        if (s == e) return

        splitSpan(s)
        splitSpan(e)

        val updatedSpans = mutableListOf<Span>()
        spans.forEach { span ->
            if (span.start >= s && span.end <= e) {
                updatedSpans.add(span.copy(
                    bold = style.bold ?: span.bold,
                    italic = style.italic ?: span.italic,
                    underline = style.underline ?: span.underline,
                    fontSize = style.fontSize ?: span.fontSize,
                    color = style.color ?: span.color,
                    backgroundColor = style.backgroundColor ?: span.backgroundColor,
                    fontFamily = style.fontFamily ?: span.fontFamily
                ))
            } else {
                updatedSpans.add(span)
            }
        }

        val sortedSpansInRange = updatedSpans.filter { it.start >= s && it.end <= e }.sortedBy { it.start }
        var current = s
        val filledSpans = mutableListOf<Span>()
        for (span in sortedSpansInRange) {
            if (span.start > current) {
                filledSpans.add(Span(
                    start = current,
                    end = span.start,
                    bold = style.bold ?: false,
                    italic = style.italic ?: false,
                    underline = style.underline ?: false,
                    fontSize = style.fontSize,
                    color = style.color,
                    backgroundColor = style.backgroundColor,
                    fontFamily = style.fontFamily
                ))
            }
            current = span.end
        }
        if (current < e) {
            filledSpans.add(Span(
                start = current,
                end = e,
                bold = style.bold ?: false,
                italic = style.italic ?: false,
                underline = style.underline ?: false,
                fontSize = style.fontSize,
                color = style.color,
                backgroundColor = style.backgroundColor,
                fontFamily = style.fontFamily
            ))
        }

        spans = (updatedSpans + filledSpans).sortedBy { it.start }
        mergeSpans()
        saveToHistory()
    }

    fun toggleBinaryStyle(start: Int, end: Int, styleType: String) {
        val s = start.coerceIn(0, text.length)
        val e = end.coerceIn(s, text.length)
        if (s == e) return

        splitSpan(s)
        splitSpan(e)

        val spansInRange = spans.filter { it.start >= s && it.end <= e }
        val hasStyle = spansInRange.all {
            when (styleType) {
                "bold" -> it.bold
                "italic" -> it.italic
                "underline" -> it.underline
                else -> false
            }
        } && spansInRange.isNotEmpty()

        val turnOn = !hasStyle

        val updatedSpans = mutableListOf<Span>()
        spans.forEach { span ->
            if (span.start >= s && span.end <= e) {
                updatedSpans.add(when (styleType) {
                    "bold" -> span.copy(bold = turnOn)
                    "italic" -> span.copy(italic = turnOn)
                    "underline" -> span.copy(underline = turnOn)
                    else -> span
                })
            } else {
                updatedSpans.add(span)
            }
        }

        if (turnOn) {
            val sortedSpansInRange = updatedSpans.filter { it.start >= s && it.end <= e }.sortedBy { it.start }
            var current = s
            val filledSpans = mutableListOf<Span>()
            for (span in sortedSpansInRange) {
                if (span.start > current) {
                    filledSpans.add(when (styleType) {
                        "bold" -> Span(start = current, end = span.start, bold = true)
                        "italic" -> Span(start = current, end = span.start, italic = true)
                        "underline" -> Span(start = current, end = span.start, underline = true)
                        else -> Span(start = current, end = span.start)
                    })
                }
                current = span.end
            }
            if (current < e) {
                filledSpans.add(when (styleType) {
                    "bold" -> Span(start = current, end = e, bold = true)
                    "italic" -> Span(start = current, end = e, italic = true)
                    "underline" -> Span(start = current, end = e, underline = true)
                    else -> Span(start = current, end = e)
                })
            }
            spans = (updatedSpans + filledSpans).sortedBy { it.start }
        } else {
            spans = updatedSpans
        }

        mergeSpans()
        saveToHistory()
    }

    fun splitSpan(atIndex: Int) {
        if (atIndex <= 0 || atIndex >= text.length) return
        val updatedSpans = mutableListOf<Span>()
        spans.forEach { span ->
            if (span.start < atIndex && span.end > atIndex) {
                updatedSpans.add(span.copy(end = atIndex))
                updatedSpans.add(span.copy(start = atIndex))
            } else {
                updatedSpans.add(span)
            }
        }
        spans = updatedSpans.sortedBy { it.start }
    }

    fun mergeSpans() {
        if (spans.isEmpty()) return
        val textLength = text.length
        
        // 1. Normalize spans to be completely disjoint
        val boundaries = mutableSetOf<Int>()
        boundaries.add(0)
        boundaries.add(textLength)
        spans.forEach {
            boundaries.add(it.start.coerceIn(0, textLength))
            boundaries.add(it.end.coerceIn(0, textLength))
        }
        val sortedPoints = boundaries.sorted()
        val disjointSpans = mutableListOf<Span>()
        for (i in 0 until sortedPoints.size - 1) {
            val start = sortedPoints[i]
            val end = sortedPoints[i + 1]
            if (start >= end) continue
            
            val covering = spans.filter { it.start < end && it.end > start }
            if (covering.isNotEmpty()) {
                var bold = false
                var italic = false
                var underline = false
                var fontSize: Float? = null
                var color: Color? = null
                var backgroundColor: Color? = null
                var fontFamily: String? = null
                
                covering.forEach { span ->
                    if (span.bold) bold = true
                    if (span.italic) italic = true
                    if (span.underline) underline = true
                    if (span.fontSize != null) fontSize = span.fontSize
                    if (span.color != null) color = span.color
                    if (span.backgroundColor != null) backgroundColor = span.backgroundColor
                    if (span.fontFamily != null) fontFamily = span.fontFamily
                }
                
                disjointSpans.add(Span(
                    start = start,
                    end = end,
                    bold = bold,
                    italic = italic,
                    underline = underline,
                    fontSize = fontSize,
                    color = color,
                    backgroundColor = backgroundColor,
                    fontFamily = fontFamily
                ))
            }
        }

        if (disjointSpans.isEmpty()) {
            spans = emptyList()
            return
        }

        // 2. Merge adjacent disjoint spans with the same formatting
        val merged = mutableListOf<Span>()
        var current = disjointSpans[0]
        for (i in 1 until disjointSpans.size) {
            val next = disjointSpans[i]
            if (current.end == next.start && hasSameFormatting(current, next)) {
                current = current.copy(end = next.end)
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        spans = merged
    }

    private fun hasSameFormatting(a: Span, b: Span): Boolean {
        return a.bold == b.bold &&
                a.italic == b.italic &&
                a.underline == b.underline &&
                a.fontSize == b.fontSize &&
                a.color == b.color &&
                a.backgroundColor == b.backgroundColor &&
                a.fontFamily == b.fontFamily
    }
}

fun List<Span>.toAnnotatedString(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    this.forEach { span ->
        var style = SpanStyle()
        if (span.bold) {
            style = style.copy(fontWeight = FontWeight.Bold)
        }
        if (span.italic) {
            style = style.copy(fontStyle = FontStyle.Italic)
        }
        if (span.underline) {
            style = style.copy(textDecoration = TextDecoration.Underline)
        }
        if (span.fontSize != null) {
            style = style.copy(fontSize = span.fontSize.sp)
        }
        if (span.color != null) {
            style = style.copy(color = span.color)
        }
        if (span.backgroundColor != null) {
            style = style.copy(background = span.backgroundColor)
        }
        builder.addStyle(style, span.start, span.end)
    }
    return builder.toAnnotatedString()
}

fun AnnotatedString.toSpans(): List<Span> {
    val result = mutableListOf<Span>()
    this.spanStyles.forEach { range ->
        val bold = range.item.fontWeight == FontWeight.Bold
        val italic = range.item.fontStyle == FontStyle.Italic
        val underline = range.item.textDecoration == TextDecoration.Underline
        val fontSize = if (range.item.fontSize != androidx.compose.ui.unit.TextUnit.Unspecified) range.item.fontSize.value else null
        val color = if (range.item.color != androidx.compose.ui.graphics.Color.Unspecified) range.item.color else null
        val backgroundColor = if (range.item.background != androidx.compose.ui.graphics.Color.Unspecified) range.item.background else null
        
        result.add(Span(
            start = range.start,
            end = range.end,
            bold = bold,
            italic = italic,
            underline = underline,
            fontSize = fontSize,
            color = color,
            backgroundColor = backgroundColor
        ))
    }
    return result
}
