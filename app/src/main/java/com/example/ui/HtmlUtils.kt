package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import java.util.Stack

// WYSIWYG HTML Parser and Serializer for Jetpack Compose AnnotatedString
fun htmlToAnnotatedString(html: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    val n = html.length

    class Tag(val name: String, val start: Int, val attribute: String? = null, val size: String? = null, val color: String? = null)
    val tagStack = Stack<Tag>()

    while (i < n) {
        val c = html[i]
        if (c == '<') {
            val endTagIndex = html.indexOf('>', i)
            if (endTagIndex != -1) {
                val tagContent = html.substring(i + 1, endTagIndex)
                i = endTagIndex + 1

                if (tagContent.startsWith("/")) {
                    val tagName = tagContent.substring(1).trim().lowercase()
                    var foundTagIndex = -1
                    for (j in tagStack.indices.reversed()) {
                        if (tagStack[j].name == tagName) {
                            foundTagIndex = j
                            break
                        }
                    }
                    if (foundTagIndex != -1) {
                        val closedTags = mutableListOf<Tag>()
                        while (tagStack.size > foundTagIndex) {
                            closedTags.add(tagStack.pop())
                        }
                        val targetTag = closedTags.first()
                        val startPos = targetTag.start
                        val endPos = builder.length

                        if (endPos > startPos) {
                            when (targetTag.name) {
                                "b", "strong" -> {
                                    builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), startPos, endPos)
                                }
                                "i", "em" -> {
                                    builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), startPos, endPos)
                                }
                                "u" -> {
                                    builder.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), startPos, endPos)
                                }
                                "a" -> {
                                    val href = targetTag.attribute ?: ""
                                    builder.addStyle(SpanStyle(color = Color(0xFF6750A4), textDecoration = TextDecoration.Underline), startPos, endPos)
                                    builder.addStringAnnotation("URL", href, startPos, endPos)
                                }
                                "font" -> {
                                    val sizeVal = targetTag.size
                                    val colorVal = targetTag.color
                                    var style = SpanStyle()
                                    if (sizeVal != null) {
                                        val s = sizeVal.toFloatOrNull()
                                        if (s != null) {
                                            style = style.copy(fontSize = s.sp)
                                        }
                                    }
                                    if (colorVal != null) {
                                        val c = parseHexColor(colorVal)
                                        if (c != null) {
                                            style = style.copy(color = c)
                                        }
                                    }
                                    builder.addStyle(style, startPos, endPos)
                                }
                            }
                        }

                        for (k in closedTags.indices.reversed()) {
                            val tag = closedTags[k]
                            if (tag != targetTag) {
                                tagStack.push(Tag(tag.name, endPos, tag.attribute, tag.size, tag.color))
                            }
                        }
                    }
                } else {
                    val parts = tagContent.split(" ", limit = 2)
                    val tagName = parts[0].trim().lowercase()
                    if (tagName == "br") {
                        builder.append("\n")
                    } else {
                        var attribute: String? = null
                        var size: String? = null
                        var color: String? = null
                        if (tagName == "a" && parts.size > 1) {
                            val attrContent = parts[1]
                            val hrefIndex = attrContent.indexOf("href=")
                            if (hrefIndex != -1) {
                                val startQuote = attrContent.indexOf('"', hrefIndex + 5)
                                if (startQuote != -1) {
                                    val endQuote = attrContent.indexOf('"', startQuote + 1)
                                    if (endQuote != -1) {
                                        attribute = attrContent.substring(startQuote + 1, endQuote)
                                    }
                                }
                            }
                        } else if (tagName == "font" && parts.size > 1) {
                            val attrContent = parts[1]
                            val sizeIndex = attrContent.indexOf("size=")
                            if (sizeIndex != -1) {
                                val startQuote = attrContent.indexOf('"', sizeIndex + 5)
                                if (startQuote != -1) {
                                    val endQuote = attrContent.indexOf('"', startQuote + 1)
                                    if (endQuote != -1) {
                                        size = attrContent.substring(startQuote + 1, endQuote)
                                    }
                                }
                            }
                            val colorIndex = attrContent.indexOf("color=")
                            if (colorIndex != -1) {
                                val startQuote = attrContent.indexOf('"', colorIndex + 6)
                                if (startQuote != -1) {
                                    val endQuote = attrContent.indexOf('"', startQuote + 1)
                                    if (endQuote != -1) {
                                        color = attrContent.substring(startQuote + 1, endQuote)
                                    }
                                }
                            }
                        }
                        tagStack.push(Tag(tagName, builder.length, attribute, size, color))
                    }
                }
            } else {
                builder.append("<")
                i++
            }
        } else if (c == '&') {
            if (html.startsWith("&amp;", i)) {
                builder.append("&")
                i += 5
            } else if (html.startsWith("&lt;", i)) {
                builder.append("<")
                i += 4
            } else if (html.startsWith("&gt;", i)) {
                builder.append(">")
                i += 4
            } else {
                builder.append("&")
                i++
            }
        } else {
            builder.append(c)
            i++
        }
    }

    while (!tagStack.isEmpty()) {
        val tag = tagStack.pop()
        val startPos = tag.start
        val endPos = builder.length
        if (endPos > startPos) {
            when (tag.name) {
                "b", "strong" -> builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), startPos, endPos)
                "i", "em" -> builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), startPos, endPos)
                "u" -> builder.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), startPos, endPos)
                "a" -> {
                    val href = tag.attribute ?: ""
                    builder.addStyle(SpanStyle(color = Color(0xFF6750A4), textDecoration = TextDecoration.Underline), startPos, endPos)
                    builder.addStringAnnotation("URL", href, startPos, endPos)
                }
                "font" -> {
                    val sizeVal = tag.size
                    val colorVal = tag.color
                    var style = SpanStyle()
                    if (sizeVal != null) {
                        val s = sizeVal.toFloatOrNull()
                        if (s != null) {
                            style = style.copy(fontSize = s.sp)
                        }
                    }
                    if (colorVal != null) {
                        val c = parseHexColor(colorVal)
                        if (c != null) {
                            style = style.copy(color = c)
                        }
                    }
                    builder.addStyle(style, startPos, endPos)
                }
            }
        }
    }

    return builder.toAnnotatedString()
}

fun AnnotatedString.toHtml(): String {
    val text = this.text
    val sb = StringBuilder()

    val bolds = this.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
    val italics = this.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
    val underlines = this.spanStyles.filter { it.item.textDecoration == TextDecoration.Underline }
    val links = this.getStringAnnotations(tag = "URL", start = 0, end = text.length)
    val fontSpans = this.spanStyles.filter {
        it.item.fontSize != androidx.compose.ui.unit.TextUnit.Unspecified ||
        (it.item.color != androidx.compose.ui.graphics.Color.Unspecified && it.item.color != Color(0xFF6750A4))
    }.groupBy { it.start to it.end }.map { (range, spans) ->
        var mergedStyle = SpanStyle()
        spans.forEach { span ->
            if (span.item.fontSize != androidx.compose.ui.unit.TextUnit.Unspecified) {
                mergedStyle = mergedStyle.copy(fontSize = span.item.fontSize)
            }
            if (span.item.color != androidx.compose.ui.graphics.Color.Unspecified) {
                mergedStyle = mergedStyle.copy(color = span.item.color)
            }
        }
        AnnotatedString.Range(mergedStyle, range.first, range.second)
    }

    for (i in text.indices) {
        bolds.forEach { if (it.start == i) sb.append("<b>") }
        italics.forEach { if (it.start == i) sb.append("<i>") }
        underlines.forEach { if (it.start == i) sb.append("<u>") }
        links.forEach { if (it.start == i) sb.append("<a href=\"${it.item}\">") }
        fontSpans.forEach {
            if (it.start == i) {
                val sizeAttr = if (it.item.fontSize != androidx.compose.ui.unit.TextUnit.Unspecified) " size=\"${it.item.fontSize.value}\"" else ""
                val colorAttr = if (it.item.color != androidx.compose.ui.graphics.Color.Unspecified) " color=\"${it.item.color.toHex()}\"" else ""
                sb.append("<font$sizeAttr$colorAttr>")
            }
        }

        val c = text[i]
        when (c) {
            '&' -> sb.append("&amp;")
            '<' -> sb.append("&lt;")
            '>' -> sb.append("&gt;")
            '\n' -> sb.append("<br>")
            else -> sb.append(c)
        }

        fontSpans.forEach { if (it.end == i + 1) sb.append("</font>") }
        links.forEach { if (it.end == i + 1) sb.append("</a>") }
        underlines.forEach { if (it.end == i + 1) sb.append("</u>") }
        italics.forEach { if (it.end == i + 1) sb.append("</i>") }
        bolds.forEach { if (it.end == i + 1) sb.append("</b>") }
    }

    return sb.toString()
}

fun addRange(ranges: List<Pair<Int, Int>>, newStart: Int, newEnd: Int): List<Pair<Int, Int>> {
    if (newStart >= newEnd) return ranges
    val result = mutableListOf<Pair<Int, Int>>()
    val allRanges = (ranges + Pair(newStart, newEnd)).sortedBy { it.first }
    if (allRanges.isEmpty()) return result
    var current = allRanges[0]
    for (i in 1 until allRanges.size) {
        val next = allRanges[i]
        if (next.first <= current.second) {
            current = Pair(current.first, maxOf(current.second, next.second))
        } else {
            result.add(current)
            current = next
        }
    }
    result.add(current)
    return result
}

fun subtractRange(ranges: List<Pair<Int, Int>>, subStart: Int, subEnd: Int): List<Pair<Int, Int>> {
    if (subStart >= subEnd) return ranges
    val result = mutableListOf<Pair<Int, Int>>()
    for (range in ranges) {
        val rStart = range.first
        val rEnd = range.second
        if (rEnd <= subStart || rStart >= subEnd) {
            result.add(range)
        } else {
            if (rStart < subStart) {
                result.add(Pair(rStart, subStart))
            }
            if (rEnd > subEnd) {
                result.add(Pair(subEnd, rEnd))
            }
        }
    }
    return result
}

fun toggleBinaryStyle(
    annotatedString: AnnotatedString,
    start: Int,
    end: Int,
    styleType: String,
    setActive: Boolean? = null
): AnnotatedString {
    if (start >= end) return annotatedString

    val otherSpans = annotatedString.spanStyles.filter { span ->
        when (styleType) {
            "bold" -> span.item.fontWeight != FontWeight.Bold
            "italic" -> span.item.fontStyle != FontStyle.Italic
            "underline" -> span.item.textDecoration != TextDecoration.Underline
            else -> true
        }
    }

    val existingRanges = annotatedString.spanStyles.filter { span ->
        when (styleType) {
            "bold" -> span.item.fontWeight == FontWeight.Bold
            "italic" -> span.item.fontStyle == FontStyle.Italic
            "underline" -> span.item.textDecoration == TextDecoration.Underline
            else -> false
        }
    }.map { Pair(it.start, it.end) }

    val shouldEnable = setActive ?: existingRanges.none { it.first < end && it.second > start }
    
    val newRanges = if (shouldEnable) {
        addRange(existingRanges, start, end)
    } else {
        subtractRange(existingRanges, start, end)
    }

    val builder = AnnotatedString.Builder(annotatedString.text)
    annotatedString.getStringAnnotations(0, annotatedString.length).forEach { ann ->
        builder.addStringAnnotation(ann.tag, ann.item, ann.start, ann.end)
    }
    
    otherSpans.forEach { span ->
        builder.addStyle(span.item, span.start, span.end)
    }
    
    val style = when (styleType) {
        "bold" -> SpanStyle(fontWeight = FontWeight.Bold)
        "italic" -> SpanStyle(fontStyle = FontStyle.Italic)
        "underline" -> SpanStyle(textDecoration = TextDecoration.Underline)
        else -> SpanStyle()
    }
    newRanges.forEach { range ->
        builder.addStyle(style, range.first, range.second)
    }

    return builder.toAnnotatedString()
}

fun applyFontSize(
    annotatedString: AnnotatedString,
    start: Int,
    end: Int,
    size: Float?
): AnnotatedString {
    val otherSpans = annotatedString.spanStyles.filter { span ->
        span.item.fontSize == androidx.compose.ui.unit.TextUnit.Unspecified
    }

    val existingSizeSpans = annotatedString.spanStyles.filter { span ->
        span.item.fontSize != androidx.compose.ui.unit.TextUnit.Unspecified
    }

    val adjustedSpans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
    existingSizeSpans.forEach { span ->
        val ranges = subtractRange(listOf(Pair(span.start, span.end)), start, end)
        ranges.forEach { r ->
            adjustedSpans.add(AnnotatedString.Range(span.item, r.first, r.second))
        }
    }

    val builder = AnnotatedString.Builder(annotatedString.text)
    annotatedString.getStringAnnotations(0, annotatedString.length).forEach { ann ->
        builder.addStringAnnotation(ann.tag, ann.item, ann.start, ann.end)
    }

    otherSpans.forEach { span ->
        builder.addStyle(span.item, span.start, span.end)
    }

    adjustedSpans.forEach { span ->
        builder.addStyle(span.item, span.start, span.end)
    }

    if (size != null) {
        builder.addStyle(SpanStyle(fontSize = size.sp), start, end)
    }

    return builder.toAnnotatedString()
}

fun applyFontColor(
    annotatedString: AnnotatedString,
    start: Int,
    end: Int,
    color: Color?
): AnnotatedString {
    val otherSpans = annotatedString.spanStyles.filter { span ->
        span.item.color == androidx.compose.ui.graphics.Color.Unspecified || span.item.color == Color(0xFF6750A4)
    }

    val existingColorSpans = annotatedString.spanStyles.filter { span ->
        span.item.color != androidx.compose.ui.graphics.Color.Unspecified && span.item.color != Color(0xFF6750A4)
    }

    val adjustedSpans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
    existingColorSpans.forEach { span ->
        val ranges = subtractRange(listOf(Pair(span.start, span.end)), start, end)
        ranges.forEach { r ->
            adjustedSpans.add(AnnotatedString.Range(span.item, r.first, r.second))
        }
    }

    val builder = AnnotatedString.Builder(annotatedString.text)
    annotatedString.getStringAnnotations(0, annotatedString.length).forEach { ann ->
        builder.addStringAnnotation(ann.tag, ann.item, ann.start, ann.end)
    }

    otherSpans.forEach { span ->
        builder.addStyle(span.item, span.start, span.end)
    }

    adjustedSpans.forEach { span ->
        builder.addStyle(span.item, span.start, span.end)
    }

    if (color != null && color != Color.Unspecified) {
        builder.addStyle(SpanStyle(color = color), start, end)
    }

    return builder.toAnnotatedString()
}

fun toggleStyleInRange(
    annotatedString: AnnotatedString,
    start: Int,
    end: Int,
    styleToToggle: String
): AnnotatedString {
    return toggleBinaryStyle(annotatedString, start, end, styleToToggle)
}

fun applySizeInRange(
    annotatedString: AnnotatedString,
    start: Int,
    end: Int,
    size: Float
): AnnotatedString {
    return applyFontSize(annotatedString, start, end, size)
}

fun applyColorInRange(
    annotatedString: AnnotatedString,
    start: Int,
    end: Int,
    color: Color
): AnnotatedString {
    return applyFontColor(annotatedString, start, end, color)
}

fun adjustOffsetForLineShifts(
    offset: Int,
    lineRanges: List<IntRange>,
    lineShifts: IntArray
): Int {
    var shiftSum = 0
    lineRanges.forEachIndexed { index, range ->
        if (offset > range.endInclusive) {
            shiftSum += lineShifts[index]
        } else if (offset >= range.start) {
            if (lineShifts[index] > 0 && offset > range.start) {
                shiftSum += lineShifts[index]
            } else if (lineShifts[index] < 0) {
                if (offset >= range.start + 2) {
                    shiftSum += lineShifts[index]
                } else if (offset > range.start) {
                    shiftSum -= (offset - range.start)
                }
            }
        }
    }
    return (offset + shiftSum).coerceIn(0, Int.MAX_VALUE)
}

fun Color.toHex(): String {
    val a = (this.alpha * 255).toInt().coerceIn(0, 255)
    val r = (this.red * 255).toInt().coerceIn(0, 255)
    val g = (this.green * 255).toInt().coerceIn(0, 255)
    val b = (this.blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X%02X", a, r, g, b)
}

fun parseHexColor(colorStr: String): Color? {
    val clean = colorStr.trim().removePrefix("#")
    return try {
        if (clean.length == 6) {
            Color(clean.toInt(16) or 0xFF000000.toInt())
        } else if (clean.length == 8) {
            Color(clean.toLong(16).toInt())
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

data class TextEdit(
    val start: Int,
    val oldEnd: Int,
    val newEnd: Int
)

fun findTextEdit(oldText: String, newText: String): TextEdit {
    val prefixLen = oldText.zip(newText).takeWhile { it.first == it.second }.count()
    val suffixLen = oldText.reversed().zip(newText.reversed()).takeWhile { it.first == it.second }.count()
    
    val maxSuffix = (oldText.length - prefixLen).coerceAtMost(newText.length - prefixLen)
    val clampedSuffixLen = suffixLen.coerceAtMost(maxSuffix)
    
    return TextEdit(
        start = prefixLen,
        oldEnd = oldText.length - clampedSuffixLen,
        newEnd = newText.length - clampedSuffixLen
    )
}

fun mapSpanRange(sStart: Int, sEnd: Int, edit: TextEdit): Pair<Int, Int>? {
    val delStart = edit.start
    val delEnd = edit.oldEnd
    val insLen = edit.newEnd - edit.start
    
    val afterDelStart = mapPointDelete(sStart, delStart, delEnd)
    val afterDelEnd = mapPointDelete(sEnd, delStart, delEnd)
    
    if (afterDelStart >= afterDelEnd) {
        return null
    }
    
    val finalStart = mapPointInsert(afterDelStart, delStart, insLen, isStart = true)
    val finalEnd = mapPointInsert(afterDelEnd, delStart, insLen, isStart = false)
    
    if (finalStart >= finalEnd) {
        return null
    }
    return Pair(finalStart, finalEnd)
}

fun mapPointDelete(p: Int, delStart: Int, delEnd: Int): Int {
    return if (p <= delStart) {
        p
    } else if (p >= delEnd) {
        p - (delEnd - delStart)
    } else {
        delStart
    }
}

fun mapPointInsert(p: Int, insStart: Int, insLen: Int, isStart: Boolean): Int {
    return if (p < insStart) {
        p
    } else if (p > insStart) {
        p + insLen
    } else {
        if (isStart) {
            p + insLen
        } else {
            p + insLen
        }
    }
}

fun applyEditToSpans(
    oldAnnotated: AnnotatedString,
    newText: String,
    edit: TextEdit
): AnnotatedString {
    val builder = AnnotatedString.Builder(newText)

    oldAnnotated.getStringAnnotations(0, oldAnnotated.length).forEach { ann ->
        val mapped = mapSpanRange(ann.start, ann.end, edit)
        if (mapped != null) {
            builder.addStringAnnotation(ann.tag, ann.item, mapped.first, mapped.second)
        }
    }

    oldAnnotated.spanStyles.forEach { span ->
        val mapped = mapSpanRange(span.start, span.end, edit)
        if (mapped != null) {
            builder.addStyle(span.item, mapped.first, mapped.second)
        }
    }

    return builder.toAnnotatedString()
}
