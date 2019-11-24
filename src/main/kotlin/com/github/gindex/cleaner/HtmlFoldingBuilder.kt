package com.github.gindex.cleaner

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.util.PsiTreeUtil

class HtmlFoldingBuilder : FoldingBuilderEx() {
    companion object {
        private val group: FoldingGroup = FoldingGroup.newGroup("html")
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        return true
    }

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val javadocs: Collection<PsiDocComment> = PsiTreeUtil.findChildrenOfType(root, PsiDocComment::class.java)

        return javadocs.flatMap { javadoc ->
            val tagFoldingDesc: List<FoldingDescriptor> = getTagFoldingDecs(javadoc, group)
            val entityFoldingDesc: List<FoldingDescriptor> = getEntityFoldingDesc(javadoc, group)

            tagFoldingDesc.plus(entityFoldingDesc)
        }.toTypedArray()
    }

    private fun getTagFoldingDecs(javadoc: PsiDocComment, group: FoldingGroup): List<FoldingDescriptor> {
        return Tag.anyTagPattern
            .extractMatchingRanges(javadoc.text)
            .map { (text, start, end) ->
                val placeholderText = when {
                    text.contains("<li>") -> "-"
                    else -> ""
                }

                FoldingDescriptor(
                    javadoc.node,
                    TextRange(javadoc.textOffset + start, javadoc.textOffset + end),
                    group,
                    placeholderText
                )
            }
    }

    private fun getEntityFoldingDesc(javadoc: PsiDocComment, group: FoldingGroup): List<FoldingDescriptor> {
        return CharacterEntityMapping.entityPattern
            .extractMatchingRanges(javadoc.text)
            .mapNotNull { (text, start, end) ->
                CharacterEntityMapping.mapToChar(text)?.run {
                    FoldingDescriptor(
                        javadoc.node,
                        TextRange(javadoc.textOffset + start, javadoc.textOffset + end),
                        group,
                        this
                    )
                }
            }
    }

    override fun getPlaceholderText(node: ASTNode): String? {
        return null
    }

}