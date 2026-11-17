package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.chobantonov.jsonfroms.core.util.UrlPolicy
import com.chobantonov.jsonfroms.core.util.isAllowedUrl
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser

data class SafeMarkdownDocument(
    val text: AnnotatedString,
    val refusedLinks: Int,
    val blocks: List<SafeMarkdownBlock> = emptyList(),
)

sealed interface SafeMarkdownBlock {
    data class Text(val content: AnnotatedString) : SafeMarkdownBlock
    data class ListBlock(val ordered: Boolean, val items: List<SafeMarkdownListItem>) : SafeMarkdownBlock
    data class Heading(val level: Int, val content: AnnotatedString) : SafeMarkdownBlock
    data class Quote(val blocks: List<SafeMarkdownBlock>) : SafeMarkdownBlock
    data class Code(val content: AnnotatedString) : SafeMarkdownBlock
    data class Table(val rows: List<List<AnnotatedString>>) : SafeMarkdownBlock
}

data class SafeMarkdownListItem(val marker: String, val blocks: List<SafeMarkdownBlock>)
enum class MarkdownProfile { Basic, Extended }

/** Builds Compose text from an allowlist of Markdown AST nodes; no HTML is generated. */
fun parseSafeMarkdown(
    source: String, policy: UrlPolicy, profile: MarkdownProfile = MarkdownProfile.Basic,
): SafeMarkdownDocument {
    val root = MarkdownParser(GFMFlavourDescriptor(), cancellationToken = CancellationToken { })
        .buildMarkdownTreeFromString(source as CharSequence)
    val output = AnnotatedString.Builder()
    var refusedLinks = 0
    val renderedRanges = mutableMapOf<ASTNode, IntRange>()

    fun slice(node: ASTNode): String = source.substring(node.startOffset, node.endOffset)
    fun unescapeText(value: String): String = buildString {
        var index = 0
        while (index < value.length) {
            val next = value.getOrNull(index + 1)
            val punctuation = next != null && (next in '!'..'/' || next in ':'..'@' ||
                next in '['..'`' || next in '{'..'~')
            if (value[index] == '\\' && punctuation) index++
            append(value[index])
            index++
        }
    }
    fun normalizeLabel(value: String): String = value.removeSurrounding("[", "]")
        .trim().split(Regex("\\s+")).joinToString(" ").lowercase()
    val references = root.children.filter { it.type == MarkdownElementTypes.LINK_DEFINITION }
        .mapNotNull { node ->
            val label = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_LABEL }
            val destination = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }
            if (label == null || destination == null) null
            else normalizeLabel(slice(label)) to slice(destination).removeSurrounding("<", ">")
        }.toMap()

    fun appendLink(target: String, content: () -> Unit) {
        val start = output.length
        content()
        if (target.isNotEmpty() && isAllowedUrl(target, policy)) {
            output.addLink(LinkAnnotation.Url(target), start, output.length)
            output.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, output.length)
        } else if (target.isNotEmpty()) {
            refusedLinks++
        }
    }

    fun visit(node: ASTNode, inline: Boolean = false) {
        val renderedStart = output.length
        when (node.type) {
            MarkdownElementTypes.STRONG, MarkdownElementTypes.EMPH, GFMElementTypes.STRIKETHROUGH -> {
                val start = output.length
                node.children.forEach { child ->
                    if (child.type != MarkdownTokenTypes.EMPH && child.type != GFMTokenTypes.TILDE) visit(child, true)
                }
                val style = when (node.type) {
                    MarkdownElementTypes.STRONG -> SpanStyle(fontWeight = FontWeight.Bold)
                    MarkdownElementTypes.EMPH -> SpanStyle(fontStyle = FontStyle.Italic)
                    else -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                }
                output.addStyle(style, start, output.length)
            }
            MarkdownElementTypes.CODE_SPAN -> {
                val start = output.length
                node.children.forEach { child ->
                    if (child.type != MarkdownTokenTypes.BACKTICK) output.append(slice(child))
                }
                output.addStyle(SpanStyle(fontFamily = FontFamily.Monospace), start, output.length)
            }
            MarkdownElementTypes.INLINE_LINK,
            MarkdownElementTypes.FULL_REFERENCE_LINK,
            MarkdownElementTypes.SHORT_REFERENCE_LINK -> {
                val label = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }
                val destination = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }
                val target = destination?.let { slice(it).removeSurrounding("<", ">") }
                    ?: node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_LABEL }
                        ?.let { references[normalizeLabel(slice(it))] }
                    ?: label?.let { references[normalizeLabel(slice(it))] }
                if (label == null || target == null) {
                    output.append(slice(node))
                } else {
                    appendLink(target) {
                        label.children.forEach { child ->
                            if (child.type != MarkdownTokenTypes.LBRACKET && child.type != MarkdownTokenTypes.RBRACKET) {
                                visit(child, true)
                            }
                        }
                    }
                }
            }
            MarkdownElementTypes.AUTOLINK -> {
                val target = slice(node).removeSurrounding("<", ">")
                appendLink(target) { output.append(target) }
            }
            MarkdownTokenTypes.EMAIL_AUTOLINK -> {
                val address = slice(node)
                appendLink("mailto:$address") { output.append(address) }
            }
            MarkdownTokenTypes.HARD_LINE_BREAK -> output.append("\n")
            MarkdownTokenTypes.EOL -> if (inline) output.append(" ") else output.append(slice(node))
            MarkdownTokenTypes.TEXT -> output.append(unescapeText(slice(node)))
            MarkdownElementTypes.IMAGE, MarkdownElementTypes.HTML_BLOCK -> output.append(slice(node))
            MarkdownElementTypes.BLOCK_QUOTE -> if (profile == MarkdownProfile.Basic) output.append(slice(node))
                else node.children.forEach { child ->
                    if (!(child.type.toString() == "Markdown:BLOCK_QUOTE" && child.children.isEmpty()) &&
                        !(child.type.toString() == "WHITE_SPACE" && slice(child).trimStart().startsWith(">"))) visit(child)
                }
            MarkdownElementTypes.CODE_FENCE, MarkdownElementTypes.CODE_BLOCK ->
                if (profile == MarkdownProfile.Basic) output.append(slice(node))
                else if (node.type == MarkdownElementTypes.CODE_BLOCK) output.append(slice(node))
                else node.children.dropWhile { !it.type.toString().contains("CODE_FENCE_CONTENT") }
                    .takeWhile { !it.type.toString().contains("CODE_FENCE_END") }
                    .forEach { output.append(slice(it)) }
            MarkdownElementTypes.ATX_1, MarkdownElementTypes.ATX_2, MarkdownElementTypes.ATX_3,
            MarkdownElementTypes.ATX_4, MarkdownElementTypes.ATX_5, MarkdownElementTypes.ATX_6,
            MarkdownElementTypes.SETEXT_1, MarkdownElementTypes.SETEXT_2 ->
                if (profile == MarkdownProfile.Basic) output.append(slice(node))
                else node.children.filter { it.type.toString().contains("_CONTENT") }
                    .forEach { content -> content.children.dropWhile { slice(it).isBlank() }
                        .forEach { visit(it, true) } }
            GFMElementTypes.TABLE -> if (profile == MarkdownProfile.Basic) output.append(slice(node))
                else node.children.filter { it.type == GFMElementTypes.HEADER || it.type == GFMElementTypes.ROW }
                    .forEachIndexed { rowIndex, row ->
                        if (rowIndex > 0) output.append("\n")
                        row.children.filter { it.type.toString() == "Markdown:CELL" }.forEachIndexed { index, cell ->
                            if (index > 0) output.append(" | ")
                            visit(cell)
                        }
                    }
            GFMTokenTypes.CELL -> node.children.forEach { visit(it, true) }
            MarkdownElementTypes.MARKDOWN_FILE,
            MarkdownElementTypes.UNORDERED_LIST,
            MarkdownElementTypes.ORDERED_LIST,
            MarkdownElementTypes.LIST_ITEM -> node.children.forEach { visit(it, inline) }
            MarkdownElementTypes.LINK_TEXT -> node.children.forEach { visit(it, true) }
            MarkdownElementTypes.PARAGRAPH -> node.children.forEachIndexed { index, child ->
                val prev = node.children.getOrNull(index - 1)?.type
                val next = node.children.getOrNull(index + 1)?.type
                if (child.type == MarkdownTokenTypes.EOL) {
                    if (prev != MarkdownTokenTypes.HARD_LINE_BREAK) output.append(" ")
                }
                else if ((child.type != MarkdownTokenTypes.LT || next != MarkdownTokenTypes.EMAIL_AUTOLINK) &&
                    (child.type != MarkdownTokenTypes.GT || prev != MarkdownTokenTypes.EMAIL_AUTOLINK)) visit(child, true)
            }
            MarkdownElementTypes.LINK_DEFINITION -> Unit
            else -> output.append(slice(node))
        }
        renderedRanges[node] = renderedStart until output.length
    }
    visit(root)
    val text = output.toAnnotatedString()
    fun rendered(node: ASTNode): AnnotatedString? {
        val range = renderedRanges[node] ?: return null
        return if (range.isEmpty()) null else text.subSequence(range.first, range.last + 1)
    }
    fun trimmed(content: AnnotatedString): AnnotatedString {
        val start = content.text.indexOfFirst { !it.isWhitespace() }
        if (start < 0) return AnnotatedString("")
        val end = content.text.indexOfLast { !it.isWhitespace() } + 1
        return content.subSequence(start, end)
    }
    fun blocks(nodes: List<ASTNode>): List<SafeMarkdownBlock> = buildList {
        for (node in nodes) {
            when (node.type) {
                MarkdownElementTypes.ORDERED_LIST, MarkdownElementTypes.UNORDERED_LIST -> {
                    val ordered = node.type == MarkdownElementTypes.ORDERED_LIST
                    val items = node.children.filter { it.type == MarkdownElementTypes.LIST_ITEM }
                    val firstMarker = items.firstOrNull()?.children?.firstOrNull()?.let(::slice).orEmpty()
                    val firstNumber = firstMarker.takeWhile(Char::isDigit).toIntOrNull() ?: 1
                    val delimiter = firstMarker.firstOrNull { it == '.' || it == ')' } ?: '.'
                    add(SafeMarkdownBlock.ListBlock(ordered, items.mapIndexed { index, item ->
                        val marker = if (ordered) "${firstNumber + index}$delimiter" else "•"
                        SafeMarkdownListItem(marker, blocks(item.children))
                    }))
                }
                MarkdownElementTypes.LIST_ITEM, MarkdownElementTypes.LINK_DEFINITION,
                MarkdownTokenTypes.EOL, MarkdownTokenTypes.LIST_BULLET,
                MarkdownTokenTypes.LIST_NUMBER -> Unit
                MarkdownElementTypes.BLOCK_QUOTE -> if (profile == MarkdownProfile.Extended) {
                    add(SafeMarkdownBlock.Quote(blocks(node.children.filter {
                        !(it.type.toString() == "Markdown:BLOCK_QUOTE" && it.children.isEmpty()) &&
                            !(it.type.toString() == "WHITE_SPACE" && slice(it).trimStart().startsWith(">"))
                    })))
                } else rendered(node)?.let { add(SafeMarkdownBlock.Text(it)) }
                MarkdownElementTypes.ATX_1, MarkdownElementTypes.ATX_2, MarkdownElementTypes.ATX_3,
                MarkdownElementTypes.ATX_4, MarkdownElementTypes.ATX_5, MarkdownElementTypes.ATX_6,
                MarkdownElementTypes.SETEXT_1, MarkdownElementTypes.SETEXT_2 -> {
                    val content = rendered(node)
                    if (content != null) {
                        if (profile == MarkdownProfile.Extended) {
                            val level = node.type.toString().substringAfterLast('_').toIntOrNull() ?: 1
                            add(SafeMarkdownBlock.Heading(level, trimmed(content)))
                        } else add(SafeMarkdownBlock.Text(content))
                    }
                }
                MarkdownElementTypes.CODE_FENCE, MarkdownElementTypes.CODE_BLOCK -> {
                    if (profile == MarkdownProfile.Extended) {
                        add(SafeMarkdownBlock.Code(rendered(node) ?: AnnotatedString("")))
                    } else rendered(node)?.let { add(SafeMarkdownBlock.Text(it)) }
                }
                GFMElementTypes.TABLE -> if (profile == MarkdownProfile.Extended) {
                    val rows = node.children.filter { it.type == GFMElementTypes.HEADER || it.type == GFMElementTypes.ROW }
                        .map { row -> row.children.filter { it.type == GFMTokenTypes.CELL }
                            .map { cell -> trimmed(rendered(cell) ?: AnnotatedString("")) } }
                    add(SafeMarkdownBlock.Table(rows))
                } else rendered(node)?.let { add(SafeMarkdownBlock.Text(it)) }
                else -> if (slice(node).isNotBlank()) rendered(node)?.let { add(SafeMarkdownBlock.Text(it)) }
            }
        }
    }
    return SafeMarkdownDocument(text, refusedLinks, blocks(root.children))
}
