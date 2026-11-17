package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.ui.text.LinkAnnotation
import com.chobantonov.jsonfroms.core.util.UrlPolicy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs

class LabelInterpolationTest {
    private val data = Json.parseToJsonElement("""{"seats":3,"customerName":"Northwind Books","displayName":"Ana **Petrova**","constructor":"ordinary","toString":"also ordinary"}""")

    @Test fun markdownLinkParametersAreComponentEncodedBeforePolicy() {
        val params = JsonObject(mapOf("query" to JsonPrimitive("{data.customerName}")))
        val record = Json.parseToJsonElement("""{"customerName":"A&B / България (one)"}""")
        val result = interpolateLabel("[search](https://example.com/?q={query}&lang={locale})",
            params, record, "bg-BG", true, true)
        assertEquals(emptyList(), result.failures)
        assertEquals("[search](https://example.com/?q=A%26B%20%2F%20%D0%91%D1%8A%D0%BB%D0%B3%D0%B0%D1%80%D0%B8%D1%8F%20%28one%29&lang=bg-BG)", result.text)
        val parsed = parseSafeMarkdown(result.text, UrlPolicy())
        assertEquals(0, parsed.refusedLinks)
        assertEquals("https://example.com/?q=A%26B%20%2F%20%D0%91%D1%8A%D0%BB%D0%B3%D0%B0%D1%80%D0%B8%D1%8F%20%28one%29&lang=bg-BG",
            assertIs<LinkAnnotation.Url>(parsed.text.getLinkAnnotations(0, parsed.text.length).single().item).url)
    }

    @Test fun markdownLinkPolicyStillRejectsUnsafeAuthoredScheme() {
        val result = interpolateLabel("[open](javascript:{query})",
            JsonObject(mapOf("query" to JsonPrimitive("{data.customerName}"))), data,
            "en", true, true)
        val parsed = parseSafeMarkdown(result.text, UrlPolicy())
        assertEquals(1, parsed.refusedLinks)
        assertEquals(emptyList(), parsed.text.getLinkAnnotations(0, parsed.text.length))
    }

    @Test fun itemSubstitutionUsesComponentEncodingAndCodeExampleStaysText() {
        val record = Json.parseToJsonElement("""{"name":"A/B?"}""")
        val item = Json.parseToJsonElement("""{"id":"one&two"}""")
        val params = JsonObject(mapOf(
            "id" to JsonPrimitive("{item.id}"),
            "name" to JsonPrimitive("{data.name}"),
        ))
        val result = interpolateLabel("``[example](/q={id})`` [real](/q={id}&name={name})",
            params, record, "en", true, true, item = item)
        assertEquals(emptyList(), result.failures)
        assertTrue(result.text.startsWith("``[example](/q=one&two)``"))
        val parsed = parseSafeMarkdown(result.text, UrlPolicy())
        assertEquals("/q=one%26two&name=A%2FB%3F",
            assertIs<LinkAnnotation.Url>(parsed.text.getLinkAnnotations(0, parsed.text.length).single().item).url)
    }

    @Test fun referenceLinkDefinitionEncodesUrlAndPreservesTitleText() {
        val params = JsonObject(mapOf(
            "query" to JsonPrimitive("{data.customerName}"),
            "title" to JsonPrimitive("title **plain**"),
        ))
        val record = Json.parseToJsonElement("""{"customerName":"A&B (one)"}""")
        val result = interpolateLabel("[open][ref]\n\n[ref]: </lookup?q={query}&lang={locale}> \"{title}\"",
            params, record, "bg", true, true)
        assertEquals(emptyList(), result.failures)
        assertTrue(result.text.contains("[ref]: </lookup?q=A%26B%20%28one%29&lang=bg>"))
        assertTrue(result.text.endsWith("\"title \\*\\*plain\\*\\*\""))
        val parsed = parseSafeMarkdown(result.text, UrlPolicy())
        assertEquals(0, parsed.refusedLinks)
        assertEquals("/lookup?q=A%26B%20%28one%29&lang=bg",
            assertIs<LinkAnnotation.Url>(parsed.text.getLinkAnnotations(0, parsed.text.length).single().item).url)
    }

    @Test fun referenceLinkDefinitionStillChecksFinalUrlPolicy() {
        val result = interpolateLabel("[open][ref]\n\n[ref]: javascript:{query}",
            JsonObject(mapOf("query" to JsonPrimitive("{data.customerName}"))),
            data, "en", true, true)
        val parsed = parseSafeMarkdown(result.text, UrlPolicy())
        assertEquals(1, parsed.refusedLinks)
        assertEquals(emptyList(), parsed.text.getLinkAnnotations(0, parsed.text.length))
    }

    @Test fun markdownProseRemainsEscapedAndUrlParenthesesCannotCloseLink() {
        val params = JsonObject(mapOf("value" to JsonPrimitive("{data.displayName}")))
        val record = Json.parseToJsonElement("""{"displayName":") [evil](javascript:alert(1)"}""")
        val result = interpolateLabel("{value} [safe](/find?q={value})", params, record,
            "en", true, true)
        assertTrue(result.text.startsWith("\\) \\[evil\\]\\(javascript:alert\\(1\\) "))
        val parsed = parseSafeMarkdown(result.text, UrlPolicy())
        assertEquals(0, parsed.refusedLinks)
        assertEquals(1, parsed.text.getLinkAnnotations(0, parsed.text.length).size)
        assertEquals("/find?q=%29%20%5Bevil%5D%28javascript%3Aalert%281%29",
            assertIs<LinkAnnotation.Url>(parsed.text.getLinkAnnotations(0, parsed.text.length).single().item).url)
    }

    @Test fun doubleBracesAndQuotedClosingBraceStayLiteral() {
        assertEquals(listOf(TextSegment.Literal("Write {seats}; "), TextSegment.Expression("data[\"}\"]")),
            splitTextTemplate("Write {{seats}}; {data[\"}\"]}"))
    }

    @Test fun escapedDynamicPathsWarnWithoutEvaluatingOrWarnOnIntentionalStaticLiteral() {
        val params = JsonObject(mapOf("escaped" to JsonPrimitive("{{data.customerName}}")))
        val result = interpolateLabel("Write {{seats}}; {{data.customerName}}; {escaped}",
            params, data, "en", dynamicAllowed = true, markdown = false)
        assertEquals("Write {seats}; {data.customerName}; {data.customerName}", result.text)
        assertEquals(emptyList(), result.failures)
        assertEquals(2, result.warnings.size)
        assertTrue(result.warnings.any { it.startsWith("text contains") })
        assertTrue(result.warnings.any { it.startsWith("textParams.escaped") })
    }

    @Test fun celQuotedStringsAndBracketKeysUseEscapesWithoutHostEvaluation() {
        val params = JsonObject(mapOf(
            "apostrophe" to JsonPrimitive("""{'it\'s'}"""),
            "escapes" to JsonPrimitive("""{'\u0041\x42\103'}"""),
            "supplementary" to JsonPrimitive("""{'\U0001F600'}"""),
            "double" to JsonPrimitive("""{"\x41"}"""),
            "slash" to JsonPrimitive("""{'a\\' + 'b'}"""),
            "dotted" to JsonPrimitive("""{data['field.with.dots']}"""),
            "bracket" to JsonPrimitive("""{data['key]part']}"""),
            "closing" to JsonPrimitive("""{data['}']}"""),
            "invalidEscape" to JsonPrimitive("""{'\q'}"""),
            "invalidScalar" to JsonPrimitive("""{'\uD800'}"""),
        ))
        val record = Json.parseToJsonElement("""{"field.with.dots":"dot","key]part":"bracket","}":"brace"}""")
        val result = interpolateLabel(
            "{apostrophe}|{escapes}|{supplementary}|{double}|{slash}|{dotted}|{bracket}|{closing}|{invalidEscape}|{invalidScalar}",
            params, record, "en", true, false,
        )
        assertEquals("it's|ABC|😀|A|a\\b|dot|bracket|brace||", result.text)
        assertEquals(2, result.failures.size)
    }

    @Test fun celRawStringsKeepBackslashesLiteralAndUseQuotedKeys() {
        val params = JsonObject(mapOf(
            "raw" to JsonPrimitive("""{r'path\to\file'}"""),
            "upper" to JsonPrimitive("""{R"\n\q"}"""),
            "quoted" to JsonPrimitive("""{r'say \'hello\''}"""),
            "key" to JsonPrimitive("""{data[r'key\part']}"""),
            "ordinary" to JsonPrimitive("""{'\n'.size()}"""),
        ))
        val values = JsonObject(mapOf("key\\part" to JsonPrimitive("found")))
        val result = interpolateLabel("{raw}|{upper}|{quoted}|{key}|{ordinary}",
            params, values, "en", true, false)
        assertEquals("path\\to\\file|\\n\\q|say \\'hello\\'|found|1", result.text)
        assertEquals(emptyList(), result.failures)

        val invalid = JsonObject(mapOf(
            "unterminated" to JsonPrimitive("""{r'abc\'}"""),
            "newline" to JsonPrimitive("{r'first\nsecond'}"),
        ))
        val errors = interpolateLabel("{unterminated}|{newline}|ready", invalid, values, "en", true, false)
        assertEquals("{r'abc\\'}||ready", errors.text)
        assertEquals(1, errors.failures.size)
    }

    @Test fun celTripleQuotedStringsHandleNewlinesDelimitersAndNestedSyntax() {
        val params = JsonObject(mapOf(
            "single" to JsonPrimitive("{'''line one\nline 'two' and } three'''}"),
            "double" to JsonPrimitive("{\"\"\"a + b, [x] and \\\"quote\\\"\"\"\"}"),
            "raw" to JsonPrimitive("{r'''path\\new\nline'''}"),
            "index" to JsonPrimitive("{[\"\"\"a]b\"\"\", 2][0]}"),
            "call" to JsonPrimitive("{size('''a,(b)''')}"),
            "invalid" to JsonPrimitive("{'''unterminated'}"),
        ))
        val result = interpolateLabel("{single}|{double}|{raw}|{index}|{call}|{invalid}",
            params, data, "en", true, false)
        assertEquals("line one\nline 'two' and } three|a + b, [x] and \"quote\"|path\\new\nline|a]b|5|{'''unterminated'}", result.text)
        assertEquals(emptyList(), result.failures)
    }

    @Test fun staticParamsDataPathsAndPluralCondition() {
        val params = Json.parseToJsonElement("""{"product":"Atlas","seats":"{data.seats}"}""") as JsonObject
        val result = interpolateLabel("{product}: {seats} {seats == 1 ? \"seat\" : \"seats\"}",
            params, data, "en", dynamicAllowed = true, markdown = false)
        assertEquals("Atlas: 3 seats", result.text)
        assertEquals(emptyList(), result.failures)
    }

    @Test fun absentDataIsQuietButUnknownRootAndClosedGateAreReported() {
        val params = Json.parseToJsonElement("""{"missing":"{data.optional.deep}","bad":"{nowhere.name}","closed":"{data.seats}"}""") as JsonObject
        val open = interpolateLabel("A{missing}B{bad}", params, data, "en", true, false)
        assertEquals("AB", open.text)
        assertEquals(1, open.failures.size)
        val closed = interpolateLabel("{closed}", params, data, "en", false, false)
        assertEquals("", closed.text)
        assertEquals(3, closed.failures.size)
    }

    @Test fun outOfRangeArrayIndexIsQuietButNonNumericIndexIsAnError() {
        val params = Json.parseToJsonElement("""{
          "absent":"{data.items[9].name}",
          "large":"{data.items[2147483648].name}",
          "max":"{data.items[9223372036854775807].name}",
          "negative":"{data.items[-9223372036854775808].name}",
          "invalid":"{data.items[\"first\"]}"
        }""") as JsonObject
        val record = Json.parseToJsonElement("""{"items":[{"name":"A"}]}""")
        val result = interpolateLabel("A{absent}B{large}C{max}D{negative}E{invalid}F", params, record, "en", true, false)
        assertEquals("ABCDEF", result.text)
        assertEquals(1, result.failures.size)
    }

    @Test fun bracketOperandsResolveThroughCelScopeAndKeepAbsentDataQuiet() {
        val params = Json.parseToJsonElement("""{
          "selected":"{data.rows[data.selected].name}",
          "nested":"{data.rows[data.indices[0]].name}",
          "object":"{data.labels[data.labelKey]}",
          "computed":"{data.rows[1 + 1].name}",
          "missing":"{data.rows[data.noIndex].name}",
          "badArray":"{data.rows[data.labelKey].name}",
          "badObject":"{data.labels[data.selected]}",
          "undeclared":"{data.labels[unknownKey]}"
        }""") as JsonObject
        val record = Json.parseToJsonElement("""{
          "rows":[{"name":"First"},{"name":"Second"},{"name":"Third"}],
          "selected":1,"indices":[2],"labelKey":"primary","labels":{"primary":"Primary"}
        }""")
        val result = interpolateLabel(
            "{selected}|{nested}|{object}|{computed}|{missing}|{badArray}|{badObject}|{undeclared}",
            params, record, "en", true, false,
        )
        assertEquals("Second|Third|Primary|Third||||", result.text)
        assertEquals(3, result.failures.size)
        assertTrue(result.failures.any { "array index must be an integer" in it })
        assertTrue(result.failures.any { "object key must be a string" in it })
        assertTrue(result.failures.any { "unknown identifier" in it })
    }

    @Test fun gatedConfigContextAndItemStayInParameterScopeOnly() {
        val params = Json.parseToJsonElement("""{
          "brand":"{config.brand}","person":"{context.external.person}","row":"{item.name}"
        }""") as JsonObject
        val open = interpolateLabel("{brand}/{person}/{row}", params, data, "en", true, false,
            config = jsonSafeConfig(mapOf<String, Any?>("brand" to "Atlas", "callback" to { "unsafe" })),
            context = Json.parseToJsonElement("""{"external":{"person":"Ada"}}"""),
            item = Json.parseToJsonElement("""{"name":"First"}"""))
        assertEquals("Atlas/Ada/First", open.text)
        assertEquals(emptyList(), open.failures)
        assertEquals(false, "callback" in jsonSafeConfig(mapOf<String, Any?>("callback" to { "unsafe" })))
        val closed = interpolateLabel("{brand}/{person}/{row}", params, data, "en", false, false)
        assertEquals("//", closed.text)
        assertEquals(3, closed.failures.size)
        val direct = interpolateLabel("{context.external.person}", params, data, "en", true, false,
            context = Json.parseToJsonElement("""{"external":{"person":"Ada"}}"""))
        assertEquals("", direct.text)
        assertEquals(1, direct.failures.size)
        val outside = interpolateLabel("{row}", Json.parseToJsonElement("""{"row":"{item.name}"}""") as JsonObject,
            data, "en", true, false)
        assertEquals("", outside.text)
        assertEquals(emptyList(), outside.failures)
    }

    @Test fun textCannotReadDataAndReservedParamCannotShadowLocale() {
        val params = Json.parseToJsonElement("""{"locale":"wrong","account":"{data.customerName}"}""") as JsonObject
        val result = interpolateLabel("{data.customerName} {locale} {account}", params, data, "bg", true, false)
        assertEquals(" bg Northwind Books", result.text)
        assertEquals(2, result.failures.size)
    }

    @Test fun jsonConstructorKeysAndMarkdownSubstitutionAreSafe() {
        val params = Json.parseToJsonElement("""{"ctor":"{data.constructor}","sibling":"{data.toString}","holder":"{data.displayName}"}""") as JsonObject
        val result = interpolateLabel("**{ctor}** {sibling}; {holder}", params, data, "en", true, true)
        assertEquals("**ordinary** also ordinary; Ana \\*\\*Petrova\\*\\*", result.text)
        assertEquals(emptyList(), result.failures)
        val rendered = parseSafeMarkdown(result.text, com.chobantonov.jsonfroms.core.util.UrlPolicy())
        assertEquals("ordinary also ordinary; Ana **Petrova**", rendered.text.text)
        assertEquals(1, rendered.text.spanStyles.size)
    }

    @Test fun substitutedMarkdownLinkRemainsText() {
        val params = Json.parseToJsonElement("""{"value":"{data.displayName}"}""") as JsonObject
        val hostile = Json.parseToJsonElement("""{"displayName":"[click](javascript:alert(1))"}""")
        val result = interpolateLabel("**Name:** {value}", params, hostile, "en", true, true)
        val rendered = parseSafeMarkdown(result.text, com.chobantonov.jsonfroms.core.util.UrlPolicy())
        assertEquals("Name: [click](javascript:alert(1))", rendered.text.text)
        assertEquals(0, rendered.text.getLinkAnnotations(0, rendered.text.length).size)
        assertEquals(0, rendered.refusedLinks)
    }

    @Test fun registeredTranslateFunctionResolvesParameterKey() {
        val params = Json.parseToJsonElement("""{"plan":"{translate(\"plan.\" + data.plan)}"}""") as JsonObject
        val record = Json.parseToJsonElement("""{"plan":"Team"}""")
        val result = interpolateLabel("Your plan is {plan}.", params, record, "bg", true, false, translate = { key, fallback ->
            if (key == "plan.Team") "Екип" else fallback
        })
        assertEquals("Your plan is Екип.", result.text)
        assertEquals(emptyList(), result.failures)
    }

    @Test fun translatePassesExplicitFallbackToHostWithoutOverridingCatalogHit() {
        val params = Json.parseToJsonElement("""{
          "known":"{translate(\"plan.Team\", \"Generic\")}",
          "missing":"{translate(\"plan.Unknown\", \"Custom\")}",
          "implicit":"{translate(\"plan.Other\")}"
        }""") as JsonObject
        val calls = mutableListOf<Pair<String, String>>()
        val result = interpolateLabel("{known}|{missing}|{implicit}", params, data, "bg", true, false,
            translate = { key, fallback ->
                calls += key to fallback
                if (key == "plan.Team") "Екип" else fallback
            })
        assertEquals("Екип|Custom|plan.Other", result.text)
        assertEquals(listOf("plan.Team" to "Generic", "plan.Unknown" to "Custom",
            "plan.Other" to "plan.Other"), calls)
        assertEquals(emptyList(), result.failures)

        val invalid = interpolateLabel("{translate(\"key\", 7)}|{translate()}|{translate(\"key\", \"a\", \"b\")}",
            null, data, "en", true, false)
        assertEquals("||", invalid.text)
        assertEquals(3, invalid.failures.size)
    }

    @Test fun collectionPredicatesAreLexicallyScopedAndShortCircuit() {
        val values = JsonObject(mapOf(
            "items" to JsonArray((0 until 5_000).map { JsonPrimitive(it) }),
            "flags" to JsonObject(mapOf("first" to JsonPrimitive(true), "second" to JsonPrimitive(false))),
        ))
        val params = Json.parseToJsonElement("""{
          "all":"{data.items.all(i, i >= 0)}",
          "exists":"{data.items.exists(i, i == 42)}",
          "one":"{data.items.exists_one(i, i == 42)}",
          "notOne":"{data.items.exists_one(i, i < 2)}",
          "keys":"{data.flags.exists(k, k == \"second\")}",
          "missing":"{data.absent.all(i, i > 0)}"
        }""") as JsonObject
        val result = interpolateLabel("{all}|{exists}|{one}|{notOne}|{keys}|{missing}",
            params, values, "en", true, false)
        assertEquals("true|true|true|false|true|", result.text)
        assertEquals(emptyList(), result.failures)

        val invalidParams = JsonObject(mapOf(
            "bad" to JsonPrimitive("{data.items.all(i, i + 1)}"),
            "host" to JsonPrimitive("{data.items.exists(i, alert(i))}"),
        ))
        val invalid = interpolateLabel("{bad}|{host}|{i}",
            invalidParams, values, "en", true, false)
        assertEquals("||", invalid.text)
        assertEquals(3, invalid.failures.size)
    }

    @Test fun collectionPredicateBudgetRejectsOversizedInputWithoutEvaluatingIt() {
        val values = JsonObject(mapOf("items" to JsonArray((0..10_000).map { JsonPrimitive(it) })))
        val params = JsonObject(mapOf("result" to JsonPrimitive("{data.items.all(i, i >= 0)}")))
        val result = interpolateLabel("Value: {result}", params, values, "en", true, false)
        assertEquals("Value: ", result.text)
        assertTrue(result.failures.single().contains("limit"))

        val nested = JsonObject(mapOf(
            "deep" to JsonPrimitive("{" + "!".repeat(130) + "true}"),
            "after" to JsonPrimitive("ready"),
        ))
        val recovered = interpolateLabel("{deep}|{after}", nested, data, "en", true, false)
        assertEquals("|ready", recovered.text)
        assertTrue(recovered.failures.single().contains("limit"))
    }

    @Test fun collectionPredicateErrorsFollowCelReductionRules() {
        val values = JsonObject(mapOf(
            "decisive" to JsonArray(listOf(JsonPrimitive(0), JsonPrimitive(-1), JsonPrimitive(1))),
            "allTrue" to JsonArray(listOf(JsonPrimitive(0), JsonPrimitive(1))),
            "twoMatchesThenError" to JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2), JsonPrimitive(0))),
        ))
        val params = JsonObject(mapOf(
            "all" to JsonPrimitive("{data.decisive.all(i, 1 / i > 0)}"),
            "exists" to JsonPrimitive("{data.decisive.exists(i, 1 / i < 0)}"),
            "unresolved" to JsonPrimitive("{data.allTrue.all(i, 1 / i > 0)}"),
            "one" to JsonPrimitive("{data.twoMatchesThenError.exists_one(i, 1 / i > 0)}"),
            "limit" to JsonPrimitive("{data.decisive.all(i, i == 0 ? ${"!".repeat(130)}true : false)}"),
        ))
        val result = interpolateLabel("{all}|{exists}|{unresolved}|{one}|{limit}",
            params, values, "en", true, false)
        assertEquals("false|true|||", result.text)
        assertEquals(3, result.failures.size)
        assertTrue(result.failures.any { it.contains("limit") })
    }

    @Test fun mapAndFilterComposeOverListsAndMapKeys() {
        val values = Json.parseToJsonElement("""{"items":[1,2,3],"flags":{"first":true,"second":false}}""")
        val params = JsonObject(mapOf(
            "count" to JsonPrimitive("{size(data.items.filter(i, i > 1))}"),
            "mapped" to JsonPrimitive("{data.items.map(i, i * 2).exists(v, v == 4)}"),
            "filteredMap" to JsonPrimitive("{data.items.map(i, i > 1, i * 10).exists(v, v == 30)}"),
            "keys" to JsonPrimitive("{data.flags.filter(k, k == \"second\").exists(v, v == \"second\")}"),
            "mappedKeys" to JsonPrimitive("{data.flags.map(k, k).exists(v, v == \"first\")}"),
        ))
        val result = interpolateLabel("{count}|{mapped}|{filteredMap}|{keys}|{mappedKeys}",
            params, values, "en", true, false)
        assertEquals("2|true|true|true|true", result.text)
        assertEquals(emptyList(), result.failures)

        val invalid = JsonObject(mapOf(
            "transform" to JsonPrimitive("{data.items.map(i, 1 / (i - 2))}"),
            "predicate" to JsonPrimitive("{data.items.filter(i, i + 1)}"),
        ))
        val errors = interpolateLabel("{transform}|{predicate}", invalid, values, "en", true, false)
        assertEquals("|", errors.text)
        assertEquals(2, errors.failures.size)
    }

    @Test fun celListLiteralsComposeWithMembershipAndMacros() {
        val params = JsonObject(mapOf(
            "member" to JsonPrimitive("{2 in [1, 2, 3]}"),
            "filtered" to JsonPrimitive("{size([1, 2, 3].filter(n, n > 1))}"),
            "mapped" to JsonPrimitive("{[1, 2, 3].map(n, n * 2).exists(n, n == 6)}"),
            "nested" to JsonPrimitive("{size([[1, 2], [3]])}"),
            "empty" to JsonPrimitive("{size([])}"),
        ))
        val result = interpolateLabel("{member}|{filtered}|{mapped}|{nested}|{empty}",
            params, data, "en", true, false)
        assertEquals("true|2|true|2|0", result.text)
        assertEquals(emptyList(), result.failures)

        val invalid = JsonObject(mapOf(
            "missing" to JsonPrimitive("{size([data.absent])}"),
            "trailing" to JsonPrimitive("{size([1,])}"),
            "bad" to JsonPrimitive("{size([1, 1 / 0])}"),
        ))
        val errors = interpolateLabel("{missing}|{trailing}|{bad}|ready", invalid, data, "en", true, false)
        assertEquals("|||ready", errors.text)
        assertEquals(3, errors.failures.size)
    }

    @Test fun celStringKeyMapLiteralsComposeAndReportInvalidEntries() {
        val params = JsonObject(mapOf(
            "size" to JsonPrimitive("{size({\"a\": 1, \"b\": 2})}"),
            "member" to JsonPrimitive("{\"a\" in {\"a\": null}}"),
            "keys" to JsonPrimitive("{ {\"a\": 1, \"b\": 2}.filter(k, k == \"b\").exists(k, k == \"b\") }"),
            "nested" to JsonPrimitive("{size({\"outer\": {\"inner\": [1, 2]}})}"),
            "quoted" to JsonPrimitive("{\"}\" in {\"}\": true}}"),
            "empty" to JsonPrimitive("{size({})}"),
        ))
        val result = interpolateLabel("{size}|{member}|{keys}|{nested}|{quoted}|{empty}",
            params, data, "en", true, false)
        assertEquals("2|true|true|1|true|0", result.text)
        assertEquals(emptyList(), result.failures)

        val invalid = JsonObject(mapOf(
            "duplicate" to JsonPrimitive("{size({\"a\": 1, \"a\": 2})}"),
            "nonString" to JsonPrimitive("{size({1: \"one\"})}"),
            "missing" to JsonPrimitive("{size({\"a\": data.absent})}"),
            "bad" to JsonPrimitive("{size({\"a\": 1 / 0})}"),
        ))
        val errors = interpolateLabel("{duplicate}|{nonString}|{missing}|{bad}|ready",
            invalid, data, "en", true, false)
        assertEquals("||||ready", errors.text)
        assertEquals(4, errors.failures.size)
    }

    @Test fun postfixIndexesSelectFromLiteralsAndMacroResults() {
        val params = JsonObject(mapOf(
            "list" to JsonPrimitive("{[10, 20][1]}"),
            "nested" to JsonPrimitive("{[[1], [2]][1][0]}"),
            "map" to JsonPrimitive("{ {\"a\": [4, 5]}[\"a\"][1] }"),
            "filtered" to JsonPrimitive("{[1, 2, 3].filter(i, i > 1)[0]}"),
            "mapped" to JsonPrimitive("{[1, 2].map(i, i * 10)[1]}"),
            "absent" to JsonPrimitive("{[1, 2][9]}"),
        ))
        val result = interpolateLabel("{list}|{nested}|{map}|{filtered}|{mapped}|{absent}",
            params, data, "en", true, false)
        assertEquals("20|2|5|2|20|", result.text)
        assertEquals(emptyList(), result.failures)

        val invalid = JsonObject(mapOf(
            "list" to JsonPrimitive("{[1, 2][\"wrong\"]}"),
            "map" to JsonPrimitive("{ {\"a\": 1}[0] }"),
        ))
        val errors = interpolateLabel("{list}|{map}|ready", invalid, data, "en", true, false)
        assertEquals("||ready", errors.text)
        assertEquals(2, errors.failures.size)
    }

    @Test fun celReceiverSizeAndStringFunctionsCompose() {
        val params = JsonObject(mapOf(
            "list" to JsonPrimitive("{[1, 2, 3].size()}"),
            "map" to JsonPrimitive("{ {\"a\": 1, \"b\": 2}.size() }"),
            "string" to JsonPrimitive("{\"A😀B\".size()}"),
            "filtered" to JsonPrimitive("{[1, 2, 3].filter(i, i > 1).size()}"),
            "contains" to JsonPrimitive("{\"Bulgarian\".contains(\"gar\")}"),
            "starts" to JsonPrimitive("{\"Bulgarian\".startsWith(\"Bul\")}"),
            "ends" to JsonPrimitive("{\"Bulgarian\".endsWith(\"ian\")}"),
            "negative" to JsonPrimitive("{\"Bulgarian\".contains(\"GER\")}"),
        ))
        val result = interpolateLabel("{list}|{map}|{string}|{filtered}|{contains}|{starts}|{ends}|{negative}",
            params, data, "en", true, false)
        assertEquals("3|2|3|2|true|true|true|false", result.text)
        assertEquals(emptyList(), result.failures)

        val invalid = JsonObject(mapOf(
            "sizeArity" to JsonPrimitive("{[1].size(2)}"),
            "wrongReceiver" to JsonPrimitive("{[1].contains(\"1\")}"),
            "wrongArgument" to JsonPrimitive("{\"one\".startsWith(1)}"),
            "unregistered" to JsonPrimitive("{\"one\".unknown()}"),
        ))
        val errors = interpolateLabel("{sizeArity}|{wrongReceiver}|{wrongArgument}|{unregistered}|ready",
            invalid, data, "en", true, false)
        assertEquals("||||ready", errors.text)
        assertEquals(4, errors.failures.size)
    }

    @Test fun celListConcatenationComposesAndStaysBounded() {
        val params = JsonObject(mapOf(
            "equal" to JsonPrimitive("{[1] + [2, 3] == [1, 2, 3]}"),
            "size" to JsonPrimitive("{size([1] + [] + [2, 3])}"),
            "mapped" to JsonPrimitive("{([1] + [2, 3]).map(i, i * 2)[2]}"),
            "mixed" to JsonPrimitive("{[1] + data.values == [1, 4]}"),
        ))
        val values = Json.parseToJsonElement("""{"values":[4]}""")
        val result = interpolateLabel("{equal}|{size}|{mapped}|{mixed}", params, values, "en", true, false)
        assertEquals("true|3|6|true", result.text)
        assertEquals(emptyList(), result.failures)

        val invalid = JsonObject(mapOf(
            "mixed" to JsonPrimitive("{[1] + 2}"),
            "subtraction" to JsonPrimitive("{[1] - [2]}"),
            "tooLarge" to JsonPrimitive("{size(data.large + [0])}"),
        ))
        val large = JsonObject(mapOf("large" to JsonArray((0 until 10_000).map(::JsonPrimitive))))
        val errors = interpolateLabel("{mixed}|{subtraction}|{tooLarge}|ready",
            invalid, large, "en", true, false)
        assertEquals("|||ready", errors.text)
        assertEquals(3, errors.failures.size)
        assertTrue(errors.failures.any { it.contains("limit") })
    }

    @Test fun celScalarConversionsCheckTypesAndIntegerRange() {
        val params = JsonObject(mapOf(
            "boolString" to JsonPrimitive("{bool(\"FALSE\")}"),
            "boolValue" to JsonPrimitive("{bool(true)}"),
            "intDouble" to JsonPrimitive("{int(3.9)}"),
            "negative" to JsonPrimitive("{int(-3.9)}"),
            "intString" to JsonPrimitive("{int(\"42\")}"),
            "doubleInt" to JsonPrimitive("{double(2) + 0.5}"),
            "doubleString" to JsonPrimitive("{double(\"3.25\")}"),
            "nested" to JsonPrimitive("{int(double(\"3.9\"))}"),
        ))
        val result = interpolateLabel(
            "{boolString}|{boolValue}|{intDouble}|{negative}|{intString}|{doubleInt}|{doubleString}|{nested}",
            params, data, "en", true, false)
        assertEquals("false|true|3|-3|42|2.5|3.25|3", result.text)
        assertEquals(emptyList(), result.failures)

        val invalid = JsonObject(mapOf(
            "bool" to JsonPrimitive("{bool(\"yes\")}"),
            "intString" to JsonPrimitive("{int(\"3.5\")}"),
            "intRange" to JsonPrimitive("{int(9223372036854775807.0)}"),
            "double" to JsonPrimitive("{double(\"NaN\")}"),
            "structure" to JsonPrimitive("{int([1])}"),
        ))
        val errors = interpolateLabel("{bool}|{intString}|{intRange}|{double}|{structure}|ready",
            invalid, data, "en", true, false)
        assertEquals("|||||ready", errors.text)
        assertEquals(5, errors.failures.size)
    }

    @Test fun celStringConversionAndDynIdentityComposeWithoutCoercion() {
        val params = JsonObject(mapOf(
            "bool" to JsonPrimitive("{string(true)}"),
            "integer" to JsonPrimitive("{string(42)}"),
            "negative" to JsonPrimitive("{string(-42)}"),
            "identity" to JsonPrimitive("{string(\"plain\")}"),
            "dynList" to JsonPrimitive("{dyn([1, 2]).size()}"),
            "dynScalar" to JsonPrimitive("{dyn(data.seats) == 3}"),
        ))
        val result = interpolateLabel("{bool}|{integer}|{negative}|{identity}|{dynList}|{dynScalar}",
            params, data, "en", true, false)
        assertEquals("true|42|-42|plain|2|true", result.text)
        assertEquals(emptyList(), result.failures)

        val invalid = JsonObject(mapOf(
            "structure" to JsonPrimitive("{string([1])}"),
            "null" to JsonPrimitive("{string(null)}"),
            "arity" to JsonPrimitive("{dyn()}"),
        ))
        val errors = interpolateLabel("{structure}|{null}|{arity}|ready",
            invalid, data, "en", true, false)
        assertEquals("|||ready", errors.text)
        assertEquals(3, errors.failures.size)
    }

    @Test fun celDoubleStringConversionUsesShortestLocaleIndependentDecimal() {
        val params = JsonObject(mapOf(
            "fraction" to JsonPrimitive("{string(3.14)}"),
            "whole" to JsonPrimitive("{string(double(3))}"),
            "negativeZero" to JsonPrimitive("{string(-0.0)}"),
            "fixedSmall" to JsonPrimitive("{string(0.0001)}"),
            "scientificSmall" to JsonPrimitive("{string(0.00001)}"),
            "scientificLarge" to JsonPrimitive("{string(1000000.0)}"),
            "composed" to JsonPrimitive("{string(double('2.5') * 2.0)}"),
        ))
        val result = interpolateLabel("{fraction}|{whole}|{negativeZero}|{fixedSmall}|{scientificSmall}|{scientificLarge}|{composed}",
            params, data, "bg", true, false)
        assertEquals("3.14|3|-0|0.0001|1e-05|1e+06|5", result.text)
        assertEquals(emptyList(), result.failures)
    }

    @Test fun celDoubleStringConversionShortensSubnormalBoundary() {
        val params = JsonObject(mapOf(
            "minimum" to JsonPrimitive("{string(5e-324)}"),
            "negative" to JsonPrimitive("{string(-5e-324)}"),
            "normal" to JsonPrimitive("{string(2.2250738585072014e-308)}"),
            "largest" to JsonPrimitive("{string(1.7976931348623157e308)}"),
            "integerBoundary" to JsonPrimitive("{string(9007199254740992.0)}"),
        ))
        val result = interpolateLabel("{minimum}|{negative}|{normal}|{largest}|{integerBoundary}",
            params, data, "en", true, false)
        assertEquals("5e-324|-5e-324|2.2250738585072014e-308|1.7976931348623157e+308|9.007199254740992e+15", result.text)
        assertEquals(emptyList(), result.failures)
    }

    @Test fun unknownFunctionAndStructureValueAreAuthorErrors() {
        val params = Json.parseToJsonElement("""{"account":"{data.customerName}","record":"{data}"}""") as JsonObject
        val result = interpolateLabel("A{shout(account)}B{record}C", params, data, "en", true, false)
        assertEquals("ABC", result.text)
        assertEquals(2, result.failures.size)
    }

    @Test fun registeredNumberCurrencyAndCalendarDateUseLocale() {
        val params = Json.parseToJsonElement("""{
          "amount":"{currency(data.amountDue, \"EUR\")}",
          "price":"{number(data.seatPrice, 2)}",
          "renewal":"{date(data.renewalDate)}"
        }""") as JsonObject
        val record = Json.parseToJsonElement("""{"amountDue":148.5,"seatPrice":49.5,"renewalDate":"2026-10-01"}""")
        val en = interpolateLabel("{amount}|{price}|{renewal}", params, record, "en", true, false)
        assertEquals(emptyList(), en.failures)
        val enParts = en.text.split('|')
        assertEquals("€148.50", enParts[0])
        assertEquals("49.50", enParts[1])
        assertTrue(enParts[2].contains("Oct") && enParts[2].contains("2026"))
        val bg = interpolateLabel("{amount}|{price}|{renewal}", params, record, "bg", true, false)
        assertEquals(emptyList(), bg.failures)
        val bgParts = bg.text.split('|')
        assertTrue(bgParts[0].contains("148,50") && bgParts[0].contains("€"))
        assertEquals("49,50", bgParts[1])
        assertTrue(bgParts[2].contains("1.10.2026"))
    }

    @Test fun missingFormattedDataIsQuietAndInvalidArgumentsReport() {
        val params = Json.parseToJsonElement("""{"missing":"{date(data.noDate)}","invalid":"{number(\"abc\")}","badDate":"{date(\"2026-02-30\")}"}""") as JsonObject
        val result = interpolateLabel("A{missing}B{invalid}C{badDate}D", params, data, "en", true, false)
        assertEquals("ABCD", result.text)
        assertEquals(2, result.failures.size)
    }

    @Test fun registeredPercentTimeAndDateTimeUseLocale() {
        val params = Json.parseToJsonElement("""{
          "ratio":"{percent(data.ratio)}", "clock":"{time(data.clock)}",
          "appointment":"{dateTime(data.appointment)}", "day":"{date(data.appointment)}",
          "local":"{dateTime(data.localAppointment)}"
        }""") as JsonObject
        val record = Json.parseToJsonElement("""{"ratio":0.25,"clock":"14:30:00","appointment":"2026-10-01T14:30:00+02:00","localAppointment":"2026-10-01T14:30"}""")
        val result = interpolateLabel("{ratio}|{clock}|{appointment}|{day}|{local}", params, record, "en", true, false)
        assertEquals(emptyList(), result.failures)
        val text = result.text.replace('\u202f', ' ').replace('\u00a0', ' ')
        assertTrue(text.startsWith("25%|2:30 PM|"), result.text)
        assertTrue(text.contains("Oct 1, 2026"), result.text)
        assertTrue(text.endsWith("2:30 PM"), result.text)
    }

    @Test fun temporalValuesValidateCalendarClockAndOffset() {
        assertEquals(ExpressionInstant(0, true), parseExpressionInstant("1970-01-01"))
        assertEquals(ExpressionInstant(52_200_000, true), parseExpressionInstant("14:30:00"))
        assertEquals(parseExpressionInstant("2026-10-01T12:30:00Z").epochMillis,
            parseExpressionInstant("2026-10-01T14:30:00+02:00").epochMillis)
        assertEquals("2026-10-01T14:30:00.000", parseExpressionInstant("2026-10-01T14:30").localDateTime)
        assertEquals(null, parseExpressionInstant("2026-10-01T14:30+02:00").localDateTime)
        for (invalid in listOf("2026-02-30", "25:00:00", "14:30.5", "2026-02-30T14:30", "2026-10-01T14:30.5", "2026-10-01T12:30:00+24:00", "tomorrow")) {
            assertTrue(runCatching { parseExpressionInstant(invalid) }.isFailure, invalid)
        }
    }

    @Test fun localDateTimeChoosesEarlierOverlapAndDefersSpringGap() {
        val overlap = parseExpressionInstant("2026-11-01T01:30").epochMillis
        val fallTransition = parseExpressionInstant("2026-11-01T06:00Z").epochMillis
        val fallOffset: (Long) -> Int = { if (it < fallTransition) -4 * 3_600_000 else -5 * 3_600_000 }
        assertEquals(parseExpressionInstant("2026-11-01T01:30-04:00").epochMillis,
            resolveLocalExpressionInstant(overlap, fallOffset))

        val gap = parseExpressionInstant("2026-03-08T02:30").epochMillis
        val springTransition = parseExpressionInstant("2026-03-08T07:00Z").epochMillis
        val springOffset: (Long) -> Int = { if (it < springTransition) -5 * 3_600_000 else -4 * 3_600_000 }
        assertEquals(null, resolveLocalExpressionInstant(gap, springOffset))
    }

    @Test fun booleanOrderingGroupingAndSizeFollowExpressionPrecedence() {
        val params = Json.parseToJsonElement("""{
          "choice":"{(size(data.names) > 1 && data.seats * 2 >= 6) ? \"many\" : \"few\"}",
          "nested":"{data.seats == 3 ? data.seats > 2 ? \"yes\" : \"no\" : \"other\"}",
          "length":"{size(data.customerName)}",
          "arithmetic":"{(data.seats + 5) * 2 - 4 / 2}",
          "nameAdd":"{data.customerName + \"!\"}",
          "exponent":"{1e+2 + 1.0}"
        }""") as JsonObject
        val record = Json.parseToJsonElement("""{"names":["A","B"],"seats":3,"customerName":"Ada"}""")
        val result = interpolateLabel("{choice}|{nested}|{length}|{arithmetic}|{nameAdd}|{exponent}", params, record, "en", true, false)
        assertEquals("many|yes|3|14|Ada!|101", result.text)
        assertEquals(emptyList(), result.failures)
    }

    @Test fun stringSizeCountsUnicodeCodePointsAcrossSurrogatePairs() {
        val params = Json.parseToJsonElement("""{
          "emoji":"{size(data.emoji)}",
          "combining":"{size(data.combining)}",
          "sequence":"{size(data.sequence)}",
          "empty":"{size(data.empty)}"
        }""") as JsonObject
        val record = JsonObject(mapOf(
            "emoji" to JsonPrimitive("😀"),
            "combining" to JsonPrimitive("e\u0301"),
            "sequence" to JsonPrimitive("👩‍💻"),
            "empty" to JsonPrimitive(""),
        ))
        val result = interpolateLabel("{emoji}|{combining}|{sequence}|{empty}",
            params, record, "en", true, false)
        assertEquals("1|2|3|0", result.text)
        assertEquals(emptyList(), result.failures)
    }

    @Test fun stringOrderingUsesUnicodeCodePointsRatherThanUtf16Units() {
        val params = Json.parseToJsonElement("""{
          "ascending":"{data.privateUse < data.emoji}",
          "descending":"{data.emoji > data.privateUse}",
          "prefix":"{data.emoji < data.emojiLong}",
          "equal":"{data.emoji <= data.emoji}"
        }""") as JsonObject
        val record = JsonObject(mapOf(
            "privateUse" to JsonPrimitive("\uE000"),
            "emoji" to JsonPrimitive("😀"),
            "emojiLong" to JsonPrimitive("😀a"),
        ))
        val result = interpolateLabel("{ascending}|{descending}|{prefix}|{equal}",
            params, record, "en", true, false)
        assertEquals("true|true|true|true", result.text)
        assertEquals(emptyList(), result.failures)
    }

    @Test fun logicalBranchesShortCircuitAndDecisiveValuesAbsorbErrors() {
        val params = Json.parseToJsonElement("""{
          "safeAnd":"{false && missing.value}",
          "safeOr":"{true || missing.value}",
          "negated":"{!(data.seats < 2)}",
          "precedence":"{true || false && false}",
          "equality":"{1 == 2 != true}",
          "absorbed":"{data.seats && false}",
          "invalid":"{data.seats && 1 / 0 > 0}"
        }""") as JsonObject
        val result = interpolateLabel("{safeAnd}|{safeOr}|{negated}|{precedence}|{equality}|{absorbed}|{invalid}", params, data, "en", true, false)
        assertEquals("false|true|true|true|true|false|", result.text)
        assertEquals(1, result.failures.size)
    }

    @Test fun logicalOperatorsReduceErrorsFromEitherSideButKeepLimitsImmediate() {
        val params = JsonObject(mapOf(
            "orRight" to JsonPrimitive("{1 / 0 > 0 || true}"),
            "orLeft" to JsonPrimitive("{true || 1 / 0 > 0}"),
            "andRight" to JsonPrimitive("{1 / 0 > 0 && false}"),
            "andLeft" to JsonPrimitive("{false && 1 / 0 > 0}"),
            "nested" to JsonPrimitive("{(1 / 0 > 0 || false) && false}"),
            "unresolvedOr" to JsonPrimitive("{1 / 0 > 0 || false}"),
            "unresolvedAnd" to JsonPrimitive("{1 / 0 > 0 && true}"),
            "limit" to JsonPrimitive("{${"!".repeat(130)}true || true}"),
        ))
        val result = interpolateLabel(
            "{orRight}|{orLeft}|{andRight}|{andLeft}|{nested}|{unresolvedOr}|{unresolvedAnd}|{limit}|ready",
            params, data, "en", true, false)
        assertEquals("true|true|false|false|false||||ready", result.text)
        assertEquals(3, result.failures.size)
        assertTrue(result.failures.any { it.contains("limit") })
    }

    @Test fun integerArithmeticRejectsOverflowWithoutWrapping() {
        val params = Json.parseToJsonElement("""{
          "sum":"{data.max + 1}", "difference":"{data.min - 1}",
          "product":"{data.max * 2}", "reverseProduct":"{-1 * data.min}",
          "quotient":"{data.min / -1}",
          "negation":"{-data.min}", "max":"{data.max + 0}",
          "remainder":"{data.min % -1}", "next":"{data.min + 1}",
          "literal":"{-9223372036854775808}"
        }""") as JsonObject
        val record = Json.parseToJsonElement("""{"max":9223372036854775807,"min":-9223372036854775808}""")
        val result = interpolateLabel(
            "A{sum}B{difference}C{product}D{reverseProduct}E{quotient}F{negation}G{max}|{remainder}|{next}|{literal}",
            params, record, "en", true, false,
        )
        assertEquals("ABCDEFG9223372036854775807|0|-9223372036854775807|-9223372036854775808", result.text)
        assertEquals(6, result.failures.size)
        assertTrue(result.failures.all { "integer overflow" in it }, result.failures.toString())
    }

    @Test fun arithmeticKeepsDoubleTypeAndRejectsMixedAndFloatingRemainder() {
        val params = Json.parseToJsonElement("""{
          "mixed":"{1 + 2.0}", "mixedAfter":"{(1.5 + 0.5) + 1}",
          "mixedData":"{data.floating + data.integer}", "floatRemainder":"{5.0 % 2.0}",
          "intRemainder":"{5 % 2}", "doubleSum":"{1.0 + 2.0}",
          "wholeDouble":"{1.5 + 0.5}", "unaryDouble":"{-1.0}"
        }""") as JsonObject
        val record = Json.parseToJsonElement("""{"floating":1.0,"integer":2}""")
        val result = interpolateLabel(
            "A{mixed}B{mixedAfter}C{mixedData}D{floatRemainder}E{intRemainder}|{doubleSum}|{wholeDouble}|{unaryDouble}",
            params, record, "en", true, false,
        )
        assertEquals("ABCDE1|3|2|-1", result.text)
        assertEquals(4, result.failures.size)
        assertTrue(result.failures.any { "remainder requires integers" in it })
        assertEquals(3, result.failures.count { "arithmetic requires matching numeric types" in it })
    }

    @Test fun numericEqualityOrderingAndMembershipKeepLargeIntegersExact() {
        val params = Json.parseToJsonElement("""{
          "equal":"{1 == 1.0}", "unequal":"{1 != 1.0}",
          "largeEqual":"{data.large == data.rounded}", "largeOrder":"{data.large > data.rounded}",
          "boundaryEqual":"{data.boundary == data.rounded}", "maxOrder":"{data.max < data.aboveMax}",
          "fraction":"{1 < 1.5}", "negative":"{-1 > -1.5}",
          "member":"{1 in data.decimals}", "list":"{data.listA == data.listB}",
          "object":"{data.objectA == data.objectB}"
        }""") as JsonObject
        val record = Json.parseToJsonElement("""{
          "large":9007199254740993,"boundary":9007199254740992,"rounded":9007199254740992.0,
          "max":9223372036854775807,"aboveMax":9223372036854775808.0,
          "decimals":[1.0],"listA":[1,{"x":2}],"listB":[1.0,{"x":2.0}],
          "objectA":{"value":[1]},"objectB":{"value":[1.0]}
        }""")
        val result = interpolateLabel(
            "{equal}|{unequal}|{largeEqual}|{largeOrder}|{boundaryEqual}|{maxOrder}|" +
                "{fraction}|{negative}|{member}|{list}|{object}",
            params, record, "en", true, false,
        )
        assertEquals("true|false|false|true|true|true|true|true|true|true|true", result.text)
        assertEquals(emptyList(), result.failures)
    }

    @Test fun booleanOrderingUsesFalseBeforeTrue() {
        val params = Json.parseToJsonElement("""{
          "less":"{false < true}", "greater":"{true > false}",
          "equal":"{false <= false}", "reverse":"{true <= false}",
          "wrong":"{true < 1}"
        }""") as JsonObject
        val result = interpolateLabel("{less}|{greater}|{equal}|{reverse}|{wrong}", params, data, "en", true, false)
        assertEquals("true|true|true|false|", result.text)
        assertEquals(1, result.failures.size)
    }

    @Test fun presenceAndMembershipDistinguishNullFromMissing() {
        val params = Json.parseToJsonElement("""{
          "present":"{has(data.present)}", "missing":"{has(data.missing)}",
          "member":"{\"Ada\" in data.names}", "key":"{\"present\" in data}",
          "absentKey":"{\"missing\" in data}", "namedIn":"{data.in}"
        }""") as JsonObject
        val record = Json.parseToJsonElement("""{"present":null,"names":["Ada","Bea"],"in":"ordinary"}""")
        val result = interpolateLabel("{present}|{missing}|{member}|{key}|{absentKey}|{namedIn}", params, record, "en", true, false)
        assertEquals("true|false|true|true|false|ordinary", result.text)
        assertEquals(emptyList(), result.failures)
    }

    @Test fun hasMacroRequiresFieldSelectionAndChecksNestedMapKeyPresence() {
        val params = Json.parseToJsonElement("""{
          "nullField":"{has(data.outer.value)}",
          "nestedMissing":"{has(data.outer.missing)}",
          "missingParent":"{has(data.absent.deep)}",
          "bracketParent":"{has(data[\"outer\"].value)}",
          "indexSyntax":"{has(data[\"outer\"])}",
          "arithmetic":"{has(1 + 1)}",
          "listField":"{has(data.names.length)}"
        }""") as JsonObject
        val record = Json.parseToJsonElement("""{"outer":{"value":null},"names":["Ada"]}""")
        val result = interpolateLabel(
            "{nullField}|{nestedMissing}|{missingParent}|{bracketParent}|{indexSyntax}|{arithmetic}|{listField}",
            params, record, "en", true, false,
        )
        assertEquals("true|false|false|true|||", result.text)
        assertEquals(3, result.failures.size)
        assertEquals(2, result.failures.count { "has requires field selection" in it })
        assertTrue(result.failures.any { "has requires an object field" in it })
    }
}
