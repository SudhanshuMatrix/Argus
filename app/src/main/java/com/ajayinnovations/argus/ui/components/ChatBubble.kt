package com.ajayinnovations.argus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ajayinnovations.argus.R
import com.ajayinnovations.argus.core.Mark
import com.ajayinnovations.argus.core.MarkKind
import com.ajayinnovations.argus.ui.theme.Alarm
import com.ajayinnovations.argus.ui.theme.BubbleText
import com.ajayinnovations.argus.ui.theme.Edge
import com.ajayinnovations.argus.ui.theme.Ink
import com.ajayinnovations.argus.ui.theme.Marker
import com.ajayinnovations.argus.ui.theme.Paper
import com.ajayinnovations.argus.ui.theme.Slate

/**
 * The received message as a chat bubble, sender above, with the dangerous parts the rules
 * found marked like a highlighter: Marker yellow behind, a thick Alarm line under (so it
 * works without colour too). Screen readers hear the message and what was marked.
 */
@Composable
fun ChatBubble(text: String, marks: List<Mark>, from: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val described = buildString {
        from?.let { append(it).append(". ") }
        append(context.getString(R.string.bubble_message, text))
        if (marks.isNotEmpty()) {
            val kinds = marks.map { it.kind }.distinct().joinToString(", ") { context.getString(markName(it)) }
            append(' ').append(context.getString(R.string.bubble_marked, kinds))
        }
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val underline = 3.dp
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = described },
    ) {
        from?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Slate) }
        // Squared top-left corner: the bubble's "tail", pointing at the sender above
        val shape = RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp)
        Text(
            text = highlighted(text, marks),
            style = BubbleText,
            color = Ink,
            onTextLayout = { layout = it },
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Paper)
                .border(1.dp, Edge, shape)
                .padding(horizontal = 18.dp, vertical = 14.dp)
                // After the text, so the yellow highlight doesn't paint over the line
                .drawWithContent {
                    drawContent()
                    val result = layout ?: return@drawWithContent
                    val thickness = underline.toPx()
                    marks.forEach { mark -> underlineRange(result, mark, thickness) { offset, size -> drawRect(Alarm, offset, size) } }
                },
        )
    }
}

/** Under each line the mark covers, one bar from its first to its last character. */
private fun underlineRange(result: TextLayoutResult, mark: Mark, thickness: Float, draw: (Offset, Size) -> Unit) {
    val length = result.layoutInput.text.length
    if (mark.start >= length) return
    val end = mark.end.coerceAtMost(length)
    val firstLine = result.getLineForOffset(mark.start)
    val lastLine = result.getLineForOffset((end - 1).coerceAtLeast(mark.start))
    for (line in firstLine..lastLine) {
        val from = if (line == firstLine) result.getHorizontalPosition(mark.start, true) else result.getLineLeft(line)
        val to = if (line == lastLine) result.getHorizontalPosition(end, true) else result.getLineRight(line)
        val y = result.getLineBaseline(line) + thickness
        draw(Offset(minOf(from, to), y), Size(kotlin.math.abs(to - from), thickness))
    }
}

private fun highlighted(text: String, marks: List<Mark>): AnnotatedString = buildAnnotatedString {
    var at = 0
    marks.sortedBy { it.start }.forEach { m ->
        if (m.start < at || m.end > text.length) return@forEach
        append(text.substring(at, m.start))
        withStyle(SpanStyle(background = Marker)) { append(text.substring(m.start, m.end)) }
        at = m.end
    }
    append(text.substring(at))
}

private fun markName(kind: MarkKind): Int = when (kind) {
    MarkKind.LINK -> R.string.mark_link
    MarkKind.SECRET -> R.string.mark_secret
    MarkKind.ACCOUNT -> R.string.mark_account
    MarkKind.MONEY -> R.string.mark_money
    MarkKind.NEW_NUMBER -> R.string.mark_new_number
    MarkKind.PRIZE -> R.string.mark_prize
    MarkKind.PHONE -> R.string.mark_phone
}
