package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.store.Translator
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put

class I18nUtilTest {
    private val translator = Translator { key, defaultMessage, _ ->
        when (key) {
            "categoryLabelKey" -> "Basic"
            "address.label" -> "Address"
            "group.label" -> "Translated group"
            "intro.text" -> "Translated introduction"
            "server.email" -> "Translated server error"
            else -> defaultMessage
        }
    }

    @Test
    fun `deriveLabelForUISchemaElement translates a category label as its key`() {
        val category = CategoryElement(label = "categoryLabelKey")

        assertEquals("Basic", deriveLabelForUISchemaElement(category, translator))
    }

    @Test
    fun `deriveLabelForUISchemaElement uses the i18n label key for a category`() {
        val category = CategoryElement(label = "", i18n = "address")

        assertEquals("Address", deriveLabelForUISchemaElement(category, translator))
    }

    @Test
    fun `deriveLabelForUISchemaElement uses the i18n label key for a group`() {
        val group = GroupElement(label = "Fallback", i18n = "group")

        assertEquals("Translated group", deriveLabelForUISchemaElement(group, translator))
    }

    @Test
    fun `label uses i18n text key and falls back to authored text`() {
        val label = LabelElement(text = "Original", i18n = "intro")
        assertEquals("Translated introduction", deriveLabelForUISchemaElement(label, translator))
        assertEquals("Original", deriveLabelForUISchemaElement(label, defaultTranslator))
    }

    @Test
    fun `additional error with explicit i18n key follows locale translation`() {
        val error = ErrorObject(instancePath = "/email", message = "Server error", params = mapOf("i18nKey" to "server.email"))
        assertEquals("Translated server error", defaultErrorTranslator.translate(error, translator, null))
        assertEquals("Server error", defaultErrorTranslator.translate(error, defaultTranslator, null))
    }

    @Test
    fun scopedTextUsesPrefixValuesAndFallback() {
        var observedValues: Map<String, Any?> = emptyMap()
        val scoped = Translator { id, _, values ->
            observedValues = values
            if (id == "root.invalidDate") "Невалидна дата" else null
        }
        val values = mapOf<String, Any?>("format" to "YYYY-MM-DD")

        assertEquals("Невалидна дата", translateScopedText(scoped, "root", "invalidDate", "Invalid date", values))
        assertEquals(values, observedValues)
        assertEquals("Fallback", translateScopedText(scoped, "root", "unknown", "Fallback"))
        assertEquals("Fallback", translateScopedText(null, null, "unknown", "Fallback"))
    }

    @Test
    fun `transformPathToI18nPrefix returns root when empty`() {
        assertEquals("root", transformPathToI18nPrefix(""))
    }

    @Test
    fun `transformPathToI18nPrefix does not modify non-array paths`() {
        assertEquals("foo", transformPathToI18nPrefix("foo"))
        assertEquals("foo.bar", transformPathToI18nPrefix("foo.bar"))
        assertEquals("bar3.foo2", transformPathToI18nPrefix("bar3.foo2"))
    }

    @Test
    fun `transformPathToI18nPrefix removes array indices`() {
        assertEquals("foo.bar", transformPathToI18nPrefix("foo.2.bar"))
        assertEquals("foo.bar", transformPathToI18nPrefix("foo.234324234.bar"))
        assertEquals("foo.bar", transformPathToI18nPrefix("foo.0.bar"))
        assertEquals("foo.bar.foobar", transformPathToI18nPrefix("foo.0.bar.1.foobar"))
        assertEquals("foobar", transformPathToI18nPrefix("3.foobar"))
        assertEquals("foobar", transformPathToI18nPrefix("foobar.3"))
        assertEquals("foo1.b2ar3.foo", transformPathToI18nPrefix("foo1.23.b2ar3.1.5.foo"))
        assertEquals("root", transformPathToI18nPrefix("3"))
    }

    @Test
    fun `getI18nKeyPrefixBySchema gets key from uischema over schema`() {
        val control = ControlElement(
            scope = "#/properties/foo",
            i18n = "controlFoo",
        )
        val schema = buildJsonSchema {
            put("type", "string")
            put("i18n", JsonPrimitive("schemaFoo"))
        }

        assertEquals("controlFoo", getI18nKeyPrefixBySchema(schema, control))
    }

    @Test
    fun `getI18nKeyPrefixBySchema gets schema key for missing uischema key`() {
        val control = ControlElement(
            scope = "#/properties/foo",
        )
        val schema = buildJsonSchema {
            put("type", "string")
            put("i18n", JsonPrimitive("schemaFoo"))
        }

        assertEquals("schemaFoo", getI18nKeyPrefixBySchema(schema, control))
    }

    @Test
    fun `getI18nKeyPrefixBySchema returns undefined for missing uischema and schema keys`() {
        val control = ControlElement(
            scope = "#/properties/foo",
        )
        val schema = buildJsonSchema {
            put("type", "string")
        }

        assertNull(getI18nKeyPrefixBySchema(schema, control))
    }

    @Test
    fun `getI18nKeyPrefixBySchema returns undefined for undefined parameters`() {
        assertNull(getI18nKeyPrefixBySchema(null, null))
    }
}
