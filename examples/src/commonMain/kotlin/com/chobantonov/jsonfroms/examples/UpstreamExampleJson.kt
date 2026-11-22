package com.chobantonov.jsonfroms.examples

import com.chobantonov.jsonfroms.core.models.AndCondition
import com.chobantonov.jsonfroms.core.models.BooleanControlLabel
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.Condition
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.DescriptionControlLabel
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.ImageViewElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue
import com.chobantonov.jsonfroms.core.models.LabelDescription
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.LeafCondition
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.models.LinkElement
import com.chobantonov.jsonfroms.core.models.OrCondition
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.SeparatorElement
import com.chobantonov.jsonfroms.core.models.SpacerElement
import com.chobantonov.jsonfroms.core.models.StringControlLabel
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.store.Translator
import com.chobantonov.jsonfroms.core.testers.NOT_APPLICABLE
import com.chobantonov.jsonfroms.core.testers.UiSchemaTester
import com.chobantonov.jsonfroms.core.util.JsonPointer
import com.chobantonov.jsonfroms.examples.upstream.behavior.i18nExample
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Builds one checked-in Kotlin example from its readable JSON fields. */
internal fun upstreamExample(
    name: String,
    label: String,
    dataJson: String? = null,
    schemaJson: String? = null,
    uischemaJson: String? = null,
    configJson: String? = null,
): ExampleDescription = parseUpstreamExample(buildJsonObject {
    put("name", name)
    put("label", label)
    dataJson?.let { put("data", Json.parseToJsonElement(it)) }
    schemaJson?.let { put("schema", Json.parseToJsonElement(it)) }
    uischemaJson?.let { put("uischema", Json.parseToJsonElement(it)) }
    configJson?.let { put("config", Json.parseToJsonElement(it)) }
})

private fun parseUpstreamExample(example: JsonObject): ExampleDescription =
    ExampleDescription(
        name = example.requiredString("name"),
        label = example.requiredString("label"),
        sourceUrl = upstreamExampleSourceUrl(example.requiredString("name")),
        data = example["data"] ?: JsonNull,
        dataProvided = "data" in example,
        schema = example.optionalObject("schema")?.let(::JsonSchema),
        uischema = example.optionalObject("uischema")?.toUiSchemaElement(),
        config = example.optionalObject("config")?.toKotlinMap().orEmpty(),
        readonly = example.optionalBoolean("readonly") ?: false,
        translations = example.optionalObject("translations")
            ?.mapValues { (_, value) -> (value as? JsonPrimitive)?.content.orEmpty() }
            .orEmpty(),
    ).withPortableUpstreamBehavior(
        example.optionalObject("config")?.optionalObject("withSort")?.toUiSchemaElement(),
    )

internal fun parseSpecExamples(snapshot: String): List<ExampleDescription> =
    Json.parseToJsonElement(snapshot).requiredArray("examples").mapIndexed { index, element ->
        val example = element.requiredObject("examples[$index]")
        val name = example.requiredString("name")
        val localizedTranslations = example.optionalObject("translations")?.mapValues { (locale, entries) ->
            flattenSpecTranslations(entries as? JsonObject
                ?: error("translations.$locale must be an object"))
        }.orEmpty()
        ExampleDescription(
            name = name,
            label = example.requiredString("label"),
            source = ExampleSource.Spec,
            sourceUrl = "https://github.com/kchobantonov/jsonforms-extended-spec/tree/" +
                "$specExamplesCommit/examples/${name.removePrefix("spec-")}",
            capabilityNotice = example.optionalObject("capabilityNotice")?.mapValues { (_, value) ->
                (value as? JsonPrimitive)?.content ?: error("Capability notice must be a string")
            }.orEmpty(),
            data = example["data"] ?: JsonNull,
            invalidData = example["invalid-data"],
            dataProvided = "data" in example,
            schema = example.optionalObject("schema")?.let(::JsonSchema),
            uischema = example.optionalObject("uischema")?.toUiSchemaElement(),
            uischemas = example["uischemas"]?.requiredArray("uischemas")?.mapIndexed { entryIndex, entry ->
                val registryEntry = entry.requiredObject("uischemas[$entryIndex]")
                val ui = registryEntry["uischema"].requiredObject("uischema").toUiSchemaElement()
                val tester = registryEntry.optionalObject("match")?.toSpecUiSchemaTester()
                    ?: { _, _, _ -> NOT_APPLICABLE }
                JsonFormsUISchemaRegistryEntry(tester, ui)
            }.orEmpty(),
            config = example.optionalObject("config")?.toKotlinMap().orEmpty(),
            translations = localizedTranslations["en"].orEmpty(),
            localizedTranslations = localizedTranslations,
        )
    }

private fun flattenSpecTranslations(entries: JsonObject): Map<String, String> {
    val values = mutableMapOf<String, Pair<Int, String>>()
    fun visit(node: JsonObject, prefix: String, depth: Int) {
        node.forEach { (key, value) ->
            val path = if (prefix.isEmpty()) key else "$prefix.$key"
            when (value) {
                is JsonObject -> visit(value, path, depth + 1)
                is JsonPrimitive -> {
                    require(value.isString) { "Translation $path must be a string" }
                    val old = values[path]
                    if (old == null || depth < old.first) values[path] = depth to value.content
                }
                else -> error("Translation $path must be a string or object")
            }
        }
    }
    visit(entries, "", 0)
    val flattened = values.mapValues { it.value.second }.toMutableMap()
    // The pinned catalogs also publish schema-pointer keys. Ordinary Controls
    // look up data-path keys, so expose aliases without overriding authored keys.
    values.forEach { (key, value) ->
        schemaTranslationAlias(key)?.let { alias ->
            if (alias !in flattened) flattened[alias] = value.second
        }
    }
    return flattened
}

private fun schemaTranslationAlias(key: String): String? {
    val marker = ".schema.properties."
    val start = key.indexOf(marker)
    if (start <= 0) return null
    val parts = ("properties." + key.substring(start + marker.length)).split('.')
    val suffix = parts.lastOrNull()?.takeIf { it == "label" || it == "description" } ?: return null
    val names = mutableListOf<String>()
    var index = 0
    val end = parts.lastIndex
    while (index < end) {
        when (parts[index]) {
            "properties" -> {
                val name = parts.getOrNull(index + 1)?.takeIf(String::isNotEmpty) ?: return null
                names += name
                index += 2
            }
            "items" -> index++
            "prefixItems" -> {
                if (parts.getOrNull(index + 1)?.toIntOrNull() == null) return null
                index += 2
            }
            else -> return null
        }
    }
    return names.takeIf { it.isNotEmpty() }?.joinToString(".")?.plus(".$suffix")
}

private fun JsonObject.toSpecUiSchemaTester(): UiSchemaTester {
    val supported = setOf("rank", "schemaPath", "notSchemaPath", "schemaPointer", "exists", "equals")
    require(keys.all { it in supported }) { "Unknown UI-schema match fields: ${keys - supported}" }
    val rank = (this["rank"] as? JsonPrimitive)?.intOrNull
        ?: error("UI-schema match.rank must be an integer")
    val requiredSchemaPath = optionalString("schemaPath")
    val excludedSchemaPath = optionalString("notSchemaPath")
    val pointer = optionalString("schemaPointer")
    val exists = optionalBoolean("exists")
    val expectedValue = this["equals"]
    require(pointer != null || (exists == null && expectedValue == null)) {
        "UI-schema match.exists and equals require schemaPointer"
    }
    return { schema, schemaPath, _ ->
        val resolved = pointer?.let { path -> schema?.toJsonObject()?.let { JsonPointer.resolve(it, path) } }
        when {
            requiredSchemaPath != null && requiredSchemaPath != schemaPath -> NOT_APPLICABLE
            excludedSchemaPath != null && excludedSchemaPath == schemaPath -> NOT_APPLICABLE
            exists != null && (resolved != null) != exists -> NOT_APPLICABLE
            expectedValue != null && expectedValue != resolved -> NOT_APPLICABLE
            else -> rank
        }
    }
}

private fun ExampleDescription.withPortableUpstreamBehavior(sortUiSchema: UiSchemaElement?): ExampleDescription = when (name) {
    "array", "array-with-sorting" -> copy(actions = listOf(
        ExampleAction("Enable Sorting") { state -> state.copy(uischema = sortUiSchema ?: state.uischema) },
        ExampleAction("Disable Sorting") { state -> state.copy(uischema = uischema) },
    ) + if (name == "array") listOf(ExampleAction("Toggle readonly") { it.copy(readonly = !it.readonly) }) else emptyList())
    "array-with-detail" -> copy(actions = listOf(
        ExampleAction("Toggle readonly") { it.copy(readonly = !it.readonly) },
    ))
    "i18n" -> copy(
        i18n = i18nExample.i18n,
        translations = i18nExample.translations,
        localizedTranslations = i18nExample.localizedTranslations,
    )
    // Keep the upstream JSON Editor's absent initial value distinct from an explicit JSON null.
    "json-editor" -> copy(dataProvided = false)
    "additional-properties" -> withLocalizedTranslations(
        english = additionalPropertiesTranslations(
            title = "Additional Properties", propertyName = "Property name", newPropertyName = "New property name",
            add = "Add", rename = "Rename", remove = "Delete", save = "Save", cancel = "Cancel",
            required = "Property name is required", invalid = "Property name is invalid",
            defined = "Property already exists",
        ),
        bulgarian = additionalPropertiesTranslations(
            title = "Допълнителни свойства", propertyName = "Име на свойство",
            newPropertyName = "Ново име на свойство", add = "Добавяне", rename = "Преименуване",
            remove = "Изтриване", save = "Запазване", cancel = "Отказ",
            required = "Името на свойството е задължително", invalid = "Името на свойството е невалидно",
            defined = "Свойството вече съществува",
        ),
    )
    "dynamic" -> copy(
        actions = listOf(ExampleAction("Change data") { state ->
            state.copy(data = JsonObject(mapOf("id" to JsonPrimitive("aaa"))))
        }),
    )
    "jsonschema" -> copy(
        actions = listOf(
            ExampleAction("Register UISchema") { it.copy(uischemas = jsonSchemaUiSchemas()) },
            ExampleAction("Unregister UISchema") { it.copy(uischemas = emptyList()) },
        ),
    )
    "nestedArray" -> {
        val nestedUiSchema = VerticalLayout(
            elements = listOf(
                ControlElement(scope = "#/properties/name"),
                ControlElement(scope = "#/properties/choices"),
            ),
        )
        val registry = listOf(
            JsonFormsUISchemaRegistryEntry(
                tester = { _, schemaPath, _ ->
                    if (schemaPath == "#/properties/exampleArray") 2 else NOT_APPLICABLE
                },
                uischema = nestedUiSchema,
            ),
        )
        copy(
            actions = listOf(
                ExampleAction("Register NestedArray UISchema") { it.copy(uischemas = registry) },
                ExampleAction("Unregister NestedArray UISchema") { it.copy(uischemas = emptyList()) },
            ),
        )
    }
    "enum-i18n" -> withLocalizedTranslations(
        english = mapOf(
            "country.DE" to "Germany", "country.IT" to "Italy", "country.JP" to "Japan",
            "country.US" to "United States", "country.RU" to "Russia", "country.Other" to "Other Country",
            "countryNoAutocomplete.DE" to "Germany", "countryNoAutocomplete.IT" to "Italy",
            "countryNoAutocomplete.JP" to "Japan", "countryNoAutocomplete.US" to "United States",
            "countryNoAutocomplete.RU" to "Russia", "countryNoAutocomplete.Other" to "Other Country",
            "status.pending" to "Awaiting Review", "status.approved" to "Approved", "status.rejected" to "Declined",
        ),
        bulgarian = mapOf(
            "country.DE" to "Германия", "country.IT" to "Италия", "country.JP" to "Япония",
            "country.US" to "Съединени щати", "country.RU" to "Русия", "country.Other" to "Друга държава",
            "countryNoAutocomplete.DE" to "Германия", "countryNoAutocomplete.IT" to "Италия",
            "countryNoAutocomplete.JP" to "Япония", "countryNoAutocomplete.US" to "Съединени щати",
            "countryNoAutocomplete.RU" to "Русия", "countryNoAutocomplete.Other" to "Друга държава",
            "status.pending" to "Очаква преглед", "status.approved" to "Одобрено", "status.rejected" to "Отхвърлено",
        ),
    )
    "array-i18n" -> withLocalizedTranslations(
        english = mapOf(
            "comments.noDataMessage" to "Be the first to write a comment",
            "comments.addTooltip" to "Add a Comment",
            "comments.addAriaLabel" to "Add a Comment",
            "comments.deleteDialogAccept" to "Delete!",
            "comments.deleteDialogDecline" to "Cancel!",
            "comments.deleteDialogMessage" to "Are you sure you want to delete this comment?",
        ),
        bulgarian = mapOf(
            "comments.noDataMessage" to "Напишете първия коментар",
            "comments.addTooltip" to "Добавяне на коментар",
            "comments.addAriaLabel" to "Добавяне на коментар",
            "comments.deleteDialogAccept" to "Изтриване!",
            "comments.deleteDialogDecline" to "Отказ!",
            "comments.deleteDialogMessage" to "Сигурни ли сте, че искате да изтриете този коментар?",
        ),
    )
    "array-with-translated-custom-element-label" -> copy(
        i18n = JsonFormsI18nState(
            locale = "en",
            translate = Translator { key, _, _ -> "translator.$key" },
        ),
    )
    else -> this
}

private const val JSON_SCHEMA_DRAFT_7_ID = "http://json-schema.org/draft-07/schema#"

private fun jsonSchemaUiSchemas(): List<JsonFormsUISchemaRegistryEntry> = listOf(
    JsonFormsUISchemaRegistryEntry(
        tester = { schema, schemaPath, _ ->
            if (schemaPath == "#/properties/minItems" && schema.hasSingleType(JsonSchemaType.Number)) {
                2
            } else {
                NOT_APPLICABLE
            }
        },
        uischema = ControlElement(scope = "#", label = StringControlLabel("Min Items")),
    ),
    JsonFormsUISchemaRegistryEntry(
        tester = { _, schemaPath, _ -> if (schemaPath == "#/properties/maxItems") 2 else NOT_APPLICABLE },
        uischema = ControlElement(scope = "#", label = StringControlLabel("Max Items")),
    ),
    JsonFormsUISchemaRegistryEntry(
        tester = { schema, schemaPath, path ->
            if (
                path == "type" && schemaPath == "#/properties/type" &&
                schema.hasSingleType(JsonSchemaType.Array)
            ) {
                2
            } else {
                NOT_APPLICABLE
            }
        },
        uischema = ControlElement(scope = "#"),
    ),
    JsonFormsUISchemaRegistryEntry(
        tester = { schema, _, _ ->
            if (schema?.draftId == JSON_SCHEMA_DRAFT_7_ID && schema.hasSingleType(JsonSchemaType.Object)) {
                2
            } else {
                NOT_APPLICABLE
            }
        },
        uischema = jsonSchemaObjectUiSchema,
    ),
    JsonFormsUISchemaRegistryEntry(
        tester = { schema, _, _ ->
            if (schema?.draftId == JSON_SCHEMA_DRAFT_7_ID && schema.hasSingleType(JsonSchemaType.Boolean)) {
                2
            } else {
                NOT_APPLICABLE
            }
        },
        uischema = VerticalLayout(elements = listOf(ControlElement(scope = "#/"))),
    ),
)

private fun JsonSchema?.hasSingleType(type: JsonSchemaType): Boolean =
    (this?.typeValue as? JsonSchemaTypeValue.Single)?.value == type

private val jsonSchemaObjectUiSchema: UiSchemaElement by lazy {
    Json.parseToJsonElement(
        """{"type":"Categorization","elements":[{"type":"Category","label":"Basic","elements":[{"type":"VerticalLayout","elements":[{"type":"Control","scope":"#/properties/__DOLLAR__id"},{"type":"Control","scope":"#/properties/__DOLLAR__schema"},{"type":"Control","scope":"#/properties/title"},{"type":"Control","scope":"#/properties/description"}]}]},{"type":"Category","label":"Structure","elements":[{"type":"VerticalLayout","elements":[{"type":"Control","scope":"#/properties/type","options":{"label-0":"Single Type","label-1":"Multiple Types"}},{"type":"Categorization","rule":{"effect":"SHOW","condition":{"scope":"#/properties/type","schema":{"anyOf":[{"type":"string","enum":["object"]},{"type":"array","items":{"type":"string"},"contains":{"type":"string","enum":["object"]}}]}}},"elements":[{"type":"Category","label":"Properties","elements":[{"type":"Control","scope":"#/properties/properties"}]},{"type":"Category","label":"Required","elements":[{"type":"Control","scope":"#/properties/required"}]}]}]}]},{"type":"Category","label":"Constraints","rule":{"effect":"SHOW","condition":{"scope":"#/properties/type","schema":{"oneOf":[{"type":"string","enum":["string","number","integer","array","object","boolean","null"]},{"type":"array","items":{"type":"string","enum":["string","number","integer","array","object","boolean","null"]},"minItems":1,"uniqueItems":true}]}}},"elements":[{"type":"Categorization","elements":[{"type":"Category","label":"Number","rule":{"effect":"SHOW","condition":{"scope":"#/properties/type","schema":{"anyOf":[{"type":"string","enum":["number","integer"]},{"type":"array","items":{"type":"string"},"contains":{"type":"string","enum":["number","integer"]}}]}}},"elements":[{"type":"VerticalLayout","elements":[{"type":"Control","scope":"#/properties/multipleOf"},{"type":"Control","scope":"#/properties/maximum"},{"type":"Control","scope":"#/properties/exclusiveMaximum"},{"type":"Control","scope":"#/properties/minimum"},{"type":"Control","scope":"#/properties/exclusiveMinimum"}]}]},{"type":"Category","label":"String","rule":{"effect":"SHOW","condition":{"scope":"#/properties/type","schema":{"anyOf":[{"type":"string","enum":["string"]},{"type":"array","items":{"type":"string"},"contains":{"type":"string","enum":["string"]}}]}}},"elements":[{"type":"Control","scope":"#/properties/maxLength"},{"type":"Control","scope":"#/properties/minLength"},{"type":"Control","scope":"#/properties/pattern"}]},{"type":"Category","label":"Array","rule":{"effect":"SHOW","condition":{"scope":"#/properties/type","schema":{"anyOf":[{"type":"string","enum":["array"]},{"type":"array","items":{"type":"string"},"contains":{"type":"string","enum":["array"]}}]}}},"elements":[{"type":"Control","scope":"#/properties/minItems"},{"type":"Control","scope":"#/properties/maxItems"},{"type":"Control","scope":"#/properties/uniqueItems"}]},{"type":"Category","label":"Object","rule":{"effect":"SHOW","condition":{"scope":"#/properties/type","schema":{"anyOf":[{"type":"string","enum":["object"]},{"type":"array","items":{"type":"string"},"contains":{"type":"string","enum":["object"]}}]}}},"elements":[{"type":"Control","scope":"#/properties/minProperties"},{"type":"Control","scope":"#/properties/maxProperties"}]}]}]}],"options":{"variant":"stepper","showNavButtons":true}}""".replace("__DOLLAR__", "$"),
    ).requiredObject("jsonschema registered UI schema").toUiSchemaElement()
}

private fun ExampleDescription.withLocalizedTranslations(
    english: Map<String, String>,
    bulgarian: Map<String, String>,
): ExampleDescription = copy(
    translations = english,
    localizedTranslations = mapOf("en" to english, "bg" to bulgarian),
    i18n = JsonFormsI18nState(
        locale = "en",
        translate = Translator { key, defaultMessage, _ -> english[key] ?: defaultMessage },
    ),
)

private fun additionalPropertiesTranslations(
    title: String,
    propertyName: String,
    newPropertyName: String,
    add: String,
    rename: String,
    remove: String,
    save: String,
    cancel: String,
    required: String,
    invalid: String,
    defined: String,
): Map<String, String> {
    val prefix = "root.additionalProperties"
    return mapOf(
        "$prefix.title" to title,
        "$prefix.propertyNameLabel" to propertyName,
        "$prefix.renamePropertyNameLabel" to newPropertyName,
        "$prefix.addTooltip" to add,
        "$prefix.addAriaLabel" to add,
        "$prefix.renameTooltip" to rename,
        "$prefix.renameAriaLabel" to rename,
        "$prefix.removeTooltip" to remove,
        "$prefix.removeAriaLabel" to remove,
        "$prefix.saveTooltip" to save,
        "$prefix.saveAriaLabel" to save,
        "$prefix.cancelTooltip" to cancel,
        "$prefix.cancelAriaLabel" to cancel,
        "$prefix.propertyNameRequired" to required,
        "$prefix.propertyNameInvalid" to invalid,
        "$prefix.propertyAlreadyDefined" to defined,
    )
}

internal fun JsonObject.toUiSchemaElement(): UiSchemaElement {
    val type = requiredString("type")
    val rule = optionalObject("rule")?.toRule()
    val options = optionalObject("options").orEmpty()
    val elements = optionalArray("elements")?.mapIndexed { index, child ->
        child.requiredObject("elements[$index]").toUiSchemaElement()
    }.orEmpty()

    return when (type) {
        "VerticalLayout" -> VerticalLayout(rule, options, elements, optionalString("name"))
        "HorizontalLayout" -> HorizontalLayout(rule, options, elements, optionalString("name"))
        "Group" -> GroupElement(
            label = optionalString("label"), i18n = optionalString("i18n"), rule = rule,
            options = options, elements = elements, name = optionalString("name"),
            cover = this["cover"]?.requiredObject("cover")?.toUiSchemaElement(),
            header = optionalArray("header")?.mapIndexed { index, child ->
                child.requiredObject("header[$index]").toUiSchemaElement()
            }.orEmpty(),
            footer = optionalArray("footer")?.mapIndexed { index, child ->
                child.requiredObject("footer[$index]").toUiSchemaElement()
            }.orEmpty(),
        )
        "Category" -> CategoryElement(optionalString("label").orEmpty(), optionalString("i18n"), rule, options, elements, optionalString("name"))
        "Categorization" -> CategorizationElement(optionalString("label").orEmpty(), optionalString("i18n"), rule, options, elements, optionalString("name"))
        "Label" -> LabelElement(requiredString("text"), optionalString("i18n"), rule, options, optionalString("name"))
        "Spacer" -> SpacerElement(this["size"], rule, options, optionalString("name"))
        "Separator" -> SeparatorElement(rule, options, optionalString("name"))
        "Link" -> LinkElement(requiredString("href"), optionalString("label"), optionalString("target"),
            optionalString("rel"), optionalString("i18n"), rule, options, optionalString("name"))
        "ImageView" -> ImageViewElement(optionalString("src"), optionalString("scope"), optionalString("alt"),
            optionalString("i18n"), rule, options, optionalString("name"))
        "Control" -> ControlElement(requiredString("scope"), controlLabel(), optionalString("i18n"), rule, options, optionalString("name"))
        "ListWithDetail" -> ListWithDetailElement(requiredString("scope"), optionalString("i18n"), rule, options, optionalString("name"))
        else -> UnknownUiSchemaElement(
            type = type,
            rule = rule,
            options = options,
            properties = JsonObject(filterKeys { it !in setOf("type", "rule", "options", "elements") }),
            elements = elements,
        )
    }
}

private fun JsonObject.controlLabel() = when (val label = this["label"]) {
    null, JsonNull -> null
    is JsonPrimitive -> if (label.isString) StringControlLabel(label.content) else {
        label.booleanOrNull?.let(::BooleanControlLabel)
    }
    is JsonObject -> DescriptionControlLabel(
        LabelDescription(label.optionalString("text"), label.optionalBoolean("show")),
    )
    else -> null
}

private fun JsonObject.toRule(): Rule {
    val effect = RuleEffect.entries.firstOrNull { it.name == requiredString("effect").uppercase() }
        ?: error("Unknown rule effect '${requiredString("effect")}'")
    return Rule(effect, this["condition"].requiredObject("condition").toCondition())
}

private fun JsonObject.toCondition(): Condition {
    val type = optionalString("type")
    return when {
        type == "AND" -> AndCondition(requiredConditions())
        type == "OR" -> OrCondition(requiredConditions())
        this["schema"] != null -> SchemaBasedCondition(
            scope = requiredString("scope"),
            schema = JsonSchema(this["schema"].requiredObject("schema")),
            failWhenUndefined = optionalBoolean("failWhenUndefined") ?: false,
            type = type,
        )
        else -> LeafCondition(optionalString("scope"), this["expectedValue"], type ?: "LEAF")
    }
}

private fun JsonObject.requiredConditions(): List<Condition> =
    requiredArray("conditions").mapIndexed { index, condition ->
        condition.requiredObject("conditions[$index]").toCondition()
    }

private fun JsonObject.toKotlinMap(): Map<String, Any?> = mapValues { (_, value) -> value.toKotlinValue() }

private fun JsonElement.toKotlinValue(): Any? = when (this) {
    JsonNull -> null
    is JsonObject -> toKotlinMap()
    is JsonArray -> map(JsonElement::toKotlinValue)
    is JsonPrimitive -> when {
        isString -> content
        booleanOrNull != null -> booleanOrNull
        else -> content.toLongOrNull() ?: content.toDoubleOrNull() ?: content
    }
}

private fun JsonElement?.requiredObject(name: String): JsonObject =
    this as? JsonObject ?: error("$name must be an object")

private fun JsonElement.requiredArray(name: String): JsonArray =
    this as? JsonArray ?: error("$name must be an array")

private fun JsonObject.optionalObject(name: String): JsonObject? = this[name] as? JsonObject

private fun JsonObject.optionalArray(name: String): JsonArray? = this[name] as? JsonArray

private fun JsonObject.requiredArray(name: String): JsonArray =
    this[name] as? JsonArray ?: error("$name must be an array")

private fun JsonObject.requiredString(name: String): String =
    optionalString(name) ?: error("$name must be a string")

private fun JsonObject.optionalString(name: String): String? =
    (this[name] as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content

private fun JsonObject.optionalBoolean(name: String): Boolean? =
    (this[name] as? JsonPrimitive)?.booleanOrNull
