package com.example.modeltest

import java.text.Normalizer
import java.util.Locale

// ------------------------------------------------------------
// Result classes
// ------------------------------------------------------------

data class WerResult(
    val wer: Double,
    val match: Int,
    val substitution: Int,
    val insertion: Int,
    val deletion: Int
)

data class CerResult(
    val cer: Double,
    val match: Int,
    val substitution: Int,
    val insertion: Int,
    val deletion: Int
)

// ------------------------------------------------------------
// Text normalization
// ------------------------------------------------------------

object Normalize {

    fun lowercase(text: String): String =
        text.lowercase(Locale.ROOT)

    fun normalizeUnicode(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFC)

    fun removePunctuation(text: String): String =
        text.replace(Regex("""[\p{P}\p{S}]+"""), " ")

    fun trimWhitespaces(text: String): String =
        text.trim().replace(Regex("""\s+"""), " ")

    /**
     * Replaces whole words or phrases.
     *
     * Example:
     * replaceWords("I have two apples", mapOf("two" to "2"))
     * -> "I have 2 apples"
     */
    fun replaceWords(
        text: String,
        replacements: Map<String, String>
    ): String {
        var result = text

        // Replace longer keys first to prioritize phrases.
        for ((key, value) in replacements.entries.sortedByDescending {
            it.key.length
        }) {
            if (key.isEmpty()) continue

            val pattern = Regex(
                "(?<![\\p{L}\\p{N}_])" +
                        Regex.escape(key) +
                        "(?![\\p{L}\\p{N}_])",
                RegexOption.IGNORE_CASE
            )

            result = pattern.replace(result) { value }
        }

        return result
    }

    fun normalize(
        text: String,
        lowercase: Boolean = true,
        removePunctuation: Boolean = true,
        trimWhitespaces: Boolean = true,
        replacements: Map<String, String> = emptyMap()
    ): String {
        var result = normalizeUnicode(text)

        if (lowercase) {
            result = lowercase(result)
        }

        if (removePunctuation) {
            result = removePunctuation(result)
        }

        if (replacements.isNotEmpty()) {
            result = replaceWords(result, replacements)
        }

        if (trimWhitespaces) {
            result = trimWhitespaces(result)
        }

        return result
    }
}

// ------------------------------------------------------------
// Alignment data
// ------------------------------------------------------------

enum class AlignmentType {
    MATCH,
    SUBSTITUTION,
    INSERTION,
    DELETION,
    IGNORED
}

data class AlignmentItem(
    val reference: String?,
    val hypothesis: String?,
    val type: AlignmentType
)

private data class EditCounts(
    val match: Int,
    val substitution: Int,
    val insertion: Int,
    val deletion: Int
) {
    val distance: Int
        get() = substitution + insertion + deletion
}

// ------------------------------------------------------------
// Levenshtein alignment
// ------------------------------------------------------------

/**
 * Creates an edit-distance matrix and backtracks to produce
 * an alignment between reference and hypothesis sequences.
 *
 * Tie-breaking priority:
 * 1. Match/substitution
 * 2. Deletion
 * 3. Insertion
 */
private fun alignSequences(
    reference: List<String>,
    hypothesis: List<String>
): List<AlignmentItem> {
    val n = reference.size
    val m = hypothesis.size

    val dp = Array(n + 1) { IntArray(m + 1) }

    for (i in 0..n) {
        dp[i][0] = i
    }

    for (j in 0..m) {
        dp[0][j] = j
    }

    for (i in 1..n) {
        for (j in 1..m) {
            val cost = if (reference[i - 1] == hypothesis[j - 1]) {
                0
            } else {
                1
            }

            dp[i][j] = minOf(
                dp[i - 1][j - 1] + cost,
                dp[i - 1][j] + 1,
                dp[i][j - 1] + 1
            )
        }
    }

    val reversed = mutableListOf<AlignmentItem>()
    var i = n
    var j = m

    while (i > 0 || j > 0) {
        // Match or substitution
        if (i > 0 && j > 0) {
            val cost = if (reference[i - 1] == hypothesis[j - 1]) {
                0
            } else {
                1
            }

            if (dp[i][j] == dp[i - 1][j - 1] + cost) {
                val ref = reference[i - 1]
                val hyp = hypothesis[j - 1]

                reversed.add(
                    AlignmentItem(
                        reference = ref,
                        hypothesis = hyp,
                        type = if (ref == hyp) {
                            AlignmentType.MATCH
                        } else {
                            AlignmentType.SUBSTITUTION
                        }
                    )
                )

                i--
                j--
                continue
            }
        }

        // Deletion: reference token is missing from hypothesis
        if (i > 0 && dp[i][j] == dp[i - 1][j] + 1) {
            reversed.add(
                AlignmentItem(
                    reference = reference[i - 1],
                    hypothesis = null,
                    type = AlignmentType.DELETION
                )
            )

            i--
            continue
        }

        // Insertion: extra hypothesis token
        if (j > 0) {
            reversed.add(
                AlignmentItem(
                    reference = null,
                    hypothesis = hypothesis[j - 1],
                    type = AlignmentType.INSERTION
                )
            )

            j--
        }
    }

    return reversed.asReversed()
}

private fun countEdits(
    alignment: List<AlignmentItem>
): EditCounts {
    var match = 0
    var substitution = 0
    var insertion = 0
    var deletion = 0

    for (item in alignment) {
        when (item.type) {
            AlignmentType.MATCH -> match++
            AlignmentType.SUBSTITUTION -> substitution++
            AlignmentType.INSERTION -> insertion++
            AlignmentType.DELETION -> deletion++
            AlignmentType.IGNORED -> Unit
        }
    }

    return EditCounts(
        match = match,
        substitution = substitution,
        insertion = insertion,
        deletion = deletion
    )
}

private fun calculateRate(
    referenceLength: Int,
    counts: EditCounts
): Double {
    if (referenceLength == 0) {
        return if (counts.distance == 0) 0.0 else Double.POSITIVE_INFINITY
    }

    return counts.distance.toDouble() / referenceLength
}

// ------------------------------------------------------------
// Word Error Rate (WER)
// ------------------------------------------------------------

fun alignWer(
    reference: String,
    hypothesis: String,
    normalize: Boolean = true,
    replacements: Map<String, String> = emptyMap()
): List<AlignmentItem> {
    val ref = if (normalize) {
        Normalize.normalize(reference, replacements = replacements)
    } else {
        reference
    }

    val hyp = if (normalize) {
        Normalize.normalize(hypothesis, replacements = replacements)
    } else {
        hypothesis
    }

    val refTokens = ref.split(Regex("""\s+""")).filter { it.isNotEmpty() }
    val hypTokens = hyp.split(Regex("""\s+""")).filter { it.isNotEmpty() }

    return alignSequences(refTokens, hypTokens)
}

fun calculateWer(
    reference: String,
    hypothesis: String,
    normalize: Boolean = true,
    replacements: Map<String, String> = emptyMap()
): WerResult {
    val alignment = alignWer(
        reference = reference,
        hypothesis = hypothesis,
        normalize = normalize,
        replacements = replacements
    )

    val counts = countEdits(alignment)

    val refLength = alignment.count {
        it.type != AlignmentType.INSERTION
    }

    return WerResult(
        wer = calculateRate(refLength, counts),
        match = counts.match,
        substitution = counts.substitution,
        insertion = counts.insertion,
        deletion = counts.deletion
    )
}

// ------------------------------------------------------------
// Character Error Rate (CER)
// ------------------------------------------------------------

/**
 * Splits strings into Unicode code points rather than UTF-16
 * Char values, so supplementary characters are handled correctly.
 */
private fun toCodePoints(text: String): List<String> {
    val result = mutableListOf<String>()
    var index = 0

    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        result.add(String(Character.toChars(codePoint)))
        index += Character.charCount(codePoint)
    }

    return result
}

fun alignCer(
    reference: String,
    hypothesis: String,
    normalize: Boolean = true,
    replacements: Map<String, String> = emptyMap()
): List<AlignmentItem> {
    val ref = if (normalize) {
        Normalize.normalize(reference, replacements = replacements)
    } else {
        reference
    }

    val hyp = if (normalize) {
        Normalize.normalize(hypothesis, replacements = replacements)
    } else {
        hypothesis
    }

    val a = toCodePoints(ref)
    val b = toCodePoints(hyp)
    val n = a.size
    val m = b.size

    fun gapCost(char: String) = if (char.isBlank()) 0 else 1

    val dp = Array(n + 1) { IntArray(m + 1) }

    for (i in 1..n) {
        dp[i][0] = dp[i - 1][0] + gapCost(a[i - 1])
    }

    for (j in 1..m) {
        dp[0][j] = dp[0][j - 1] + gapCost(b[j - 1])
    }

    for (i in 1..n) {
        for (j in 1..m) {
            val subCost = if (a[i - 1] == b[j - 1]) 0 else 1

            dp[i][j] = minOf(
                dp[i - 1][j - 1] + subCost,
                dp[i - 1][j] + gapCost(a[i - 1]),
                dp[i][j - 1] + gapCost(b[j - 1])
            )
        }
    }

    val reversed = mutableListOf<AlignmentItem>()
    var i = n
    var j = m

    while (i > 0 || j > 0) {
        if (i > 0 && j > 0) {
            val r = a[i - 1]
            val h = b[j - 1]
            val cost = if (r == h) 0 else 1

            if (dp[i][j] == dp[i - 1][j - 1] + cost) {
                reversed.add(
                    AlignmentItem(
                        reference = r,
                        hypothesis = h,
                        type = when {
                            r.isBlank() && h.isBlank() ->
                                AlignmentType.IGNORED
                            r == h -> AlignmentType.MATCH
                            else -> AlignmentType.SUBSTITUTION
                        }
                    )
                )
                i--
                j--
                continue
            }
        }

        if (i > 0 &&
            dp[i][j] == dp[i - 1][j] + gapCost(a[i - 1])
        ) {
            val r = a[i - 1]

            reversed.add(
                AlignmentItem(
                    reference = r,
                    hypothesis = null,
                    type = if (r.isBlank()) {
                        AlignmentType.IGNORED
                    } else {
                        AlignmentType.DELETION
                    }
                )
            )
            i--
            continue
        }

        if (j > 0) {
            val h = b[j - 1]

            reversed.add(
                AlignmentItem(
                    reference = null,
                    hypothesis = h,
                    type = if (h.isBlank()) {
                        AlignmentType.IGNORED
                    } else {
                        AlignmentType.INSERTION
                    }
                )
            )
            j--
        }
    }

    return reversed.asReversed()
}

fun calculateCer(
    reference: String,
    hypothesis: String,
    normalize: Boolean = true,
    replacements: Map<String, String> = emptyMap()
): CerResult {
    val alignment = alignCer(
        reference = reference,
        hypothesis = hypothesis,
        normalize = normalize,
        replacements = replacements
    )

    val counts = countEdits(alignment)

    val refLength = alignment.count {
        it.type != AlignmentType.INSERTION &&
                it.type != AlignmentType.IGNORED
    }

    return CerResult(
        cer = calculateRate(refLength, counts),
        match = counts.match,
        substitution = counts.substitution,
        insertion = counts.insertion,
        deletion = counts.deletion
    )
}