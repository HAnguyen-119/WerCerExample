package com.example.modeltest

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WerCerAlignment(
    reference: String,
    hypothesis: String,
    isWer: Boolean,
    modifier: Modifier = Modifier,
    normalize: Boolean = true,
    replacements: Map<String, String> = emptyMap()
) {
    val alignment = remember(
        reference, hypothesis, isWer, replacements
    ) {
        if (isWer) {
            alignWer(reference, hypothesis, normalize = normalize, replacements = replacements)
        } else {
            alignCer(reference, hypothesis, normalize = normalize, replacements = replacements)
        }
    }

    val lines = remember(alignment, isWer) {
        val ref = StringBuilder()
        val hyp = StringBuilder()
        val markers = StringBuilder()

        for (item in alignment) {
            val marker = when (item.type) {
                AlignmentType.MATCH -> ""
                AlignmentType.SUBSTITUTION -> "S"
                AlignmentType.INSERTION -> "I"
                AlignmentType.DELETION -> "D"
                AlignmentType.IGNORED -> ""
            }

            if (isWer) {
                // WER: one aligned column per word.
                val width = maxOf(
                    item.reference?.length ?: 0,
                    item.hypothesis?.length ?: 0,
                    marker.length,
                    1
                ) + 1

                ref.append((item.reference ?: "").padEnd(width))
                hyp.append((item.hypothesis ?: "").padEnd(width))

                // Align the marker to the beginning of the column.
                markers.append(marker)
                markers.append(" ".repeat(width - marker.length))
            } else {
                // CER: one compact column per character.
                ref.append(item.reference ?: " ")
                hyp.append(item.hypothesis ?: " ")
                markers.append(marker.ifEmpty { " " })
            }
        }

        Triple(ref.toString(), hyp.toString(), markers.toString())
    }

    val textStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        letterSpacing = 0.sp
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        AlignmentRow(
            label = "REF:",
            value = lines.first,
            style = textStyle
        )

        AlignmentRow(
            label = "HYP:",
            value = lines.second,
            style = textStyle
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "",
                modifier = Modifier.width(40.dp),
                style = textStyle
            )

            BasicTextField(
                value = lines.third,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                textStyle = textStyle
            )
        }
    }
}

@Composable
private fun AlignmentRow(
    label: String,
    value: String,
    style: TextStyle
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.width(40.dp),
            style = style
        )

        BasicTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = style
        )
    }
}