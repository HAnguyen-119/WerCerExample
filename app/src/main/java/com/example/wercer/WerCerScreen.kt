package com.example.modeltest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WerCerScreen() {
    val reference = "Ở cánh đồng nọ có hai anh em"
    val hypothesis = "Ờ cánh đồng nạ có hai anh em"

    val werResult = calculateWer(reference, hypothesis)
    val cerResult = calculateCer(reference, hypothesis)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Word Error Rate (WER)",
            style = MaterialTheme.typography.titleMedium
        )

        Text("WER: ${"%.2f".format(werResult.wer * 100)}%")

        Text(
            text = "Match: ${werResult.match} | " +
                    "Substitution: ${werResult.substitution} | " +
                    "Insertion: ${werResult.insertion} | " +
                    "Deletion: ${werResult.deletion}"
        )

        WerCerAlignment(
            reference = reference,
            hypothesis = hypothesis,
            isWer = true
        )

        HorizontalDivider()

        Text(
            text = "Character Error Rate (CER)",
            style = MaterialTheme.typography.titleMedium
        )

        Text("CER: ${"%.2f".format(cerResult.cer * 100)}%")

        Text(
            text = "Match: ${cerResult.match} | " +
                    "Substitution: ${cerResult.substitution} | " +
                    "Insertion: ${cerResult.insertion} | " +
                    "Deletion: ${cerResult.deletion}"
        )

        WerCerAlignment(
            reference = reference,
            hypothesis = hypothesis,
            isWer = false
        )
    }
}