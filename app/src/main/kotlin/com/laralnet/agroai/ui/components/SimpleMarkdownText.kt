package com.laralnet.agroai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Renders a subset of Markdown: # headers, **bold**, - bullet lists, and plain paragraphs.
 * Intended for displaying Gemma responses which may use these basic formatting tokens.
 */
@Composable
fun SimpleMarkdownText(text: String, modifier: Modifier = Modifier) {
    val lines = text.lines()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            when {
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# "),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row {
                        Text("• ", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = parseInlineStyles(line.drop(2)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                line.isBlank() -> {
                    Spacer(Modifier.height(4.dp))
                }
                else -> {
                    Text(
                        text = parseInlineStyles(line),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            i++
        }
    }
}

private fun parseInlineStyles(text: String) = buildAnnotatedString {
    val boldRegex = Regex("""\*\*(.+?)\*\*""")
    var last = 0
    boldRegex.findAll(text).forEach { match ->
        append(text.substring(last, match.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(match.groupValues[1])
        }
        last = match.range.last + 1
    }
    append(text.substring(last))
}
