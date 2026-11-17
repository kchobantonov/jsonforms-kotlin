package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.ui.text.LinkAnnotation
import com.chobantonov.jsonfroms.core.util.UrlPolicy
import com.chobantonov.jsonfroms.core.models.LabelElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SafeMarkdownTest {
    @Test fun extendedProfileBuildsSafeStructuralBlocks() {
        val source = "# Heading **bold**\n\nSetext *heading*\n---\n\n> quoted *text*\n> - nested\n\n```kotlin\nx < y\n```\n\n| A | B |\n| --- | --- |\n| one | [link](/help) |"
        val basic = parseSafeMarkdown(source, UrlPolicy())
        assertEquals(source, basic.text.text)
        val extended = parseSafeMarkdown(source, UrlPolicy(), MarkdownProfile.Extended)
        assertEquals("Heading bold", assertIs<SafeMarkdownBlock.Heading>(extended.blocks[0]).content.text)
        assertEquals(1, assertIs<SafeMarkdownBlock.Heading>(extended.blocks[0]).level)
        assertEquals("Setext heading", assertIs<SafeMarkdownBlock.Heading>(extended.blocks[1]).content.text)
        assertEquals(2, assertIs<SafeMarkdownBlock.Heading>(extended.blocks[1]).level)
        val quote = assertIs<SafeMarkdownBlock.Quote>(extended.blocks[2])
        assertEquals("quoted text", assertIs<SafeMarkdownBlock.Text>(quote.blocks[0]).content.text)
        assertEquals("nested", assertIs<SafeMarkdownBlock.Text>(
            assertIs<SafeMarkdownBlock.ListBlock>(quote.blocks[1]).items.single().blocks.single()).content.text)
        assertEquals("x < y\n", assertIs<SafeMarkdownBlock.Code>(extended.blocks[3]).content.text)
        val table = assertIs<SafeMarkdownBlock.Table>(extended.blocks[4])
        assertEquals(listOf("A", "B"), table.rows[0].map { it.text })
        assertEquals(listOf("one", "link"), table.rows[1].map { it.text })
        assertEquals("/help", assertIs<LinkAnnotation.Url>(table.rows[1][1]
            .getLinkAnnotations(0, table.rows[1][1].length).single().item).url)
    }

    @Test fun extendedProfileKeepsHtmlAndImagesLiteralAndRefusesUnsafeCellLinks() {
        val source = "<script>bad()</script>\n\n![alt](https://example.com/a.png)\n\n| A |\n| --- |\n| [bad](javascript:alert(1)) |"
        val document = parseSafeMarkdown(source, UrlPolicy(), MarkdownProfile.Extended)
        assertEquals(1, document.refusedLinks)
        assertEquals("<script>bad()</script>", assertIs<SafeMarkdownBlock.Text>(document.blocks[0]).content.text)
        assertEquals("![alt](https://example.com/a.png)", assertIs<SafeMarkdownBlock.Text>(document.blocks[1]).content.text)
        val cell = assertIs<SafeMarkdownBlock.Table>(document.blocks[2]).rows[1][0]
        assertEquals("bad", cell.text)
        assertEquals(emptyList(), cell.getLinkAnnotations(0, cell.length))
        val fenced = parseSafeMarkdown("```html\n<script>safe text</script>\n```", UrlPolicy(), MarkdownProfile.Extended)
        assertEquals("<script>safe text</script>\n", assertIs<SafeMarkdownBlock.Code>(fenced.blocks.single()).content.text)
    }

    @Test fun extendedProfileKeepsEmptyCodeAndPolicyCheckedNestedLinks() {
        val source = "# [heading](custom:part)\n\n> [blocked](javascript:alert(1))\n\n```\n```\n\n| Link |\n| --- |\n| [cell](custom:part) |"
        val document = parseSafeMarkdown(source, UrlPolicy(allowedSchemes = setOf("custom")), MarkdownProfile.Extended)
        val heading = assertIs<SafeMarkdownBlock.Heading>(document.blocks[0]).content
        assertEquals("heading", heading.text)
        assertEquals("custom:part", assertIs<LinkAnnotation.Url>(
            heading.getLinkAnnotations(0, heading.length).single().item).url)
        val quote = assertIs<SafeMarkdownBlock.Quote>(document.blocks[1])
        val blocked = assertIs<SafeMarkdownBlock.Text>(quote.blocks.single()).content
        assertEquals("blocked", blocked.text)
        assertEquals(emptyList(), blocked.getLinkAnnotations(0, blocked.length))
        assertEquals("", assertIs<SafeMarkdownBlock.Code>(document.blocks[2]).content.text)
        val cell = assertIs<SafeMarkdownBlock.Table>(document.blocks[3]).rows[1][0]
        assertEquals("custom:part", assertIs<LinkAnnotation.Url>(
            cell.getLinkAnnotations(0, cell.length).single().item).url)
        assertEquals(1, document.refusedLinks)
    }
    @Test fun listsExposeNestedItemsAndKeepPolicyCheckedLinks() {
        val document = parseSafeMarkdown(
            "Before\n\n- first [link][ref]\n- second\n  - nested **bold**\n\n3. one\n1. two\n\n[ref]: /help",
            UrlPolicy(),
        )
        assertEquals(0, document.refusedLinks)
        val unordered = assertIs<SafeMarkdownBlock.ListBlock>(document.blocks[1])
        assertEquals(false, unordered.ordered)
        assertEquals(listOf("•", "•"), unordered.items.map { it.marker })
        val first = assertIs<SafeMarkdownBlock.Text>(unordered.items[0].blocks.single()).content
        assertEquals("first link", first.text)
        assertEquals("/help", assertIs<LinkAnnotation.Url>(
            first.getLinkAnnotations(0, first.length).single().item).url)
        val nested = assertIs<SafeMarkdownBlock.ListBlock>(unordered.items[1].blocks[1])
        assertEquals("nested bold", assertIs<SafeMarkdownBlock.Text>(nested.items.single().blocks.single()).content.text)
        val ordered = assertIs<SafeMarkdownBlock.ListBlock>(document.blocks[2])
        assertEquals(true, ordered.ordered)
        assertEquals(listOf("3.", "4."), ordered.items.map { it.marker })
        assertEquals("one", assertIs<SafeMarkdownBlock.Text>(ordered.items[0].blocks.single()).content.text)
    }

    @Test fun refusedListLinkKeepsTextWithoutAnnotationOrDuplicateDiagnosticCount() {
        val document = parseSafeMarkdown("- [blocked](javascript:alert(1))\n- visible", UrlPolicy())
        assertEquals(1, document.refusedLinks)
        val list = assertIs<SafeMarkdownBlock.ListBlock>(document.blocks.single())
        val first = assertIs<SafeMarkdownBlock.Text>(list.items.first().blocks.single()).content
        assertEquals("blocked", first.text)
        assertEquals(emptyList(), first.getLinkAnnotations(0, first.length))
        assertEquals("visible", assertIs<SafeMarkdownBlock.Text>(list.items.last().blocks.single()).content.text)
    }

    @Test fun parenthesizedOrderedListKeepsStartAndDelimiter() {
        val list = assertIs<SafeMarkdownBlock.ListBlock>(parseSafeMarkdown(
            "4) first\n1) second", UrlPolicy()).blocks.single())
        assertEquals(listOf("4)", "5)"), list.items.map { it.marker })
    }

    @Test
    fun basicProfileKeepsTextAndStylesWithoutHtml() {
        val document = parseSafeMarkdown("**Bold** *italic* ~~old~~ `code`\n\n- first\n- second", UrlPolicy())
        assertEquals("Bold italic old code\n\n- first\n- second", document.text.text)
        assertEquals(4, document.text.spanStyles.size)
        assertEquals(0, document.refusedLinks)
    }

    @Test
    fun orderedListAndLineBreakStayReadable() {
        val source = "1. first\n2. second\n\nnext  \nline"
        val document = parseSafeMarkdown(source, UrlPolicy())
        assertEquals("1. first\n2. second\n\nnext\nline", document.text.text)
        assertEquals("first second", parseSafeMarkdown("first\nsecond", UrlPolicy()).text.text)
        assertEquals("first\nsecond", parseSafeMarkdown("first\\\nsecond", UrlPolicy()).text.text)
        assertEquals("strong soft", parseSafeMarkdown("**strong\nsoft**", UrlPolicy()).text.text)
    }

    @Test
    fun excludedConstructsRemainLiteral() {
        val source = "# Heading\n> quote\n<script>alert(1)</script>\n![alt](https://example.com/a.png)" +
            "\n\n| first | second |\n| --- | --- |\n| a | b |\n\n```html\n<a href=\"javascript:bad\">hi</a>\n```"
        assertEquals(source, parseSafeMarkdown(source, UrlPolicy()).text.text)
    }

    @Test
    fun urlPolicyAloneControlsLinks() {
        val source = "[safe](/help) [blocked](javascript:alert(1)) [custom](custom:part)"
        val document = parseSafeMarkdown(source, UrlPolicy(allowedSchemes = setOf("custom")))
        assertEquals("safe blocked custom", document.text.text)
        assertEquals(1, document.refusedLinks)
        val links = document.text.getLinkAnnotations(0, document.text.length)
        assertEquals(2, links.size)
        assertEquals("/help", assertIs<LinkAnnotation.Url>(links[0].item).url)
        assertEquals("custom:part", assertIs<LinkAnnotation.Url>(links[1].item).url)
    }

    @Test
    fun referenceAndAngleLinksUseTheSamePolicy() {
        val source = "[manual][docs] <https://example.com> <help@example.com>\n\n[docs]: /guide"
        val document = parseSafeMarkdown(source, UrlPolicy())
        assertEquals("manual https://example.com help@example.com\n\n", document.text.text)
        val links = document.text.getLinkAnnotations(0, document.text.length)
        assertEquals(listOf("/guide", "https://example.com", "mailto:help@example.com"),
            links.map { assertIs<LinkAnnotation.Url>(it.item).url })
    }

    @Test
    fun typographyAndMarkdownConfigUseLocalThenHostDefaults() {
        val host = mapOf<String, Any?>("jsonformsExtended" to mapOf(
            "markup" to mapOf("typography" to false, "markdown" to mapOf("enabled" to false)),
        ))
        assertEquals(false, markdownEnabled(host))
        assertEquals(false, markupTypographyEnabled(LabelElement("text"), host))
        assertEquals(true, markupTypographyEnabled(LabelElement("text", options = mapOf(
            "typography" to JsonPrimitive(true),
        )), host))
        assertEquals(true, markdownEnabled(emptyMap()))
        assertEquals(true, markupTypographyEnabled(LabelElement("text"), emptyMap()))
        assertEquals(MarkdownProfile.Basic, markdownProfile(emptyMap()))
        assertEquals(MarkdownProfile.Basic, markdownProfile(mapOf("jsonformsExtended" to mapOf(
            "markup" to mapOf("markdown" to mapOf("profile" to "unknown"))))))
        assertEquals(MarkdownProfile.Extended, markdownProfile(mapOf("jsonformsExtended" to mapOf(
            "markup" to mapOf("markdown" to mapOf("profile" to "extended"))))))
    }

    @Test
    fun escapedMarkdownPunctuationDisplaysLiterally() {
        val document = parseSafeMarkdown("Ana \\*\\*Petrova\\*\\*", UrlPolicy())
        assertEquals("Ana **Petrova**", document.text.text)
        assertEquals(0, document.text.spanStyles.size)
    }
}
