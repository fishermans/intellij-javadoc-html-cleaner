package com.github.gindex.cleaner

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.javadoc.PsiDocComment
import java.util.regex.Pattern

class TagAnnotator : Annotator {

    private val leadingAsterisksPattern = Pattern.compile("(\\R\\s+\\*)")

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val javadoc = element as? PsiDocComment ?: return
        val javadocText = javadoc.text ?: return

        Tag.tags.forEach {
            val tagStyleKey = it.mapTagToStyle() ?: return@forEach

            it.tagStartAndEndPattern.extractMatchingRanges(javadocText)
                    .flatMap { (text, start, end) ->
                        extractRangesWithSplit(text, javadoc.textOffset + start, javadoc.textOffset + end)
                    }
                    .forEach { range ->
                        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                                .range(range)
                                .textAttributes(tagStyleKey)
                                .create()
                    }
        }
    }

    private fun extractRangesWithSplit(text: String, startInParent: Int, endInParent: Int): List<TextRange> =
            if (text.contains("*")) {
                leadingAsterisksPattern.extractMatchingRanges(text)
                        .fold(Pair(startInParent, listOf<TextRange>())) { (currentStart, collector), asterisksRange ->
                            val extendedRange = collector.plus(TextRange(currentStart, startInParent + asterisksRange.start))
                            Pair(startInParent + asterisksRange.end, extendedRange)
                        }.run {
                            second.plus(TextRange(first, endInParent))
                        }
            } else {
                listOf(TextRange(startInParent, endInParent))
            }

}