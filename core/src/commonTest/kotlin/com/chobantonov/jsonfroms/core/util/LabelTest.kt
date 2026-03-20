package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.BooleanControlLabel
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.DescriptionControlLabel
import com.chobantonov.jsonfroms.core.models.LabelDescription
import com.chobantonov.jsonfroms.core.models.StringControlLabel
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonPrimitive

class LabelTest {
    @Test
    fun `control relative - no schema`() {
        assertLabel(
            control = ControlElement(scope = "/properties/foo"),
            expected = LabelDescription(text = "Foo", show = true),
        )
    }

    @Test
    fun `control without label string camel split - no schema`() {
        assertLabel(
            control = ControlElement(scope = "#/properties/fooBarBaz"),
            expected = LabelDescription(text = "Foo Bar Baz", show = true),
        )
    }

    @Test
    fun `control with label string - no schema`() {
        assertLabel(
            control = ControlElement(
                scope = "#/properties/foo",
                label = StringControlLabel("bar"),
            ),
            expected = LabelDescription(text = "bar", show = true),
        )
    }

    @Test
    fun `control with label boolean - no schema`() {
        assertLabel(
            control = ControlElement(
                scope = "#/properties/foo",
                label = BooleanControlLabel(true),
            ),
            expected = LabelDescription(text = "Foo", show = true),
        )
    }

    @Test
    fun `control with label object empty - no schema`() {
        assertLabel(
            control = ControlElement(
                scope = "#/properties/foo",
                label = DescriptionControlLabel(LabelDescription()),
            ),
            expected = LabelDescription(text = "Foo", show = true),
        )
    }

    @Test
    fun `control with label object text-only - no schema`() {
        assertLabel(
            control = ControlElement(
                scope = "#/properties/foo",
                label = DescriptionControlLabel(LabelDescription(text = "mega bar")),
            ),
            expected = LabelDescription(text = "mega bar", show = true),
        )
    }

    @Test
    fun `control with label object visible-only - no schema`() {
        assertLabel(
            control = ControlElement(
                scope = "#/properties/foo",
                label = DescriptionControlLabel(LabelDescription(show = true)),
            ),
            expected = LabelDescription(text = "Foo", show = true),
        )
    }

    @Test
    fun `control with label object full - no schema`() {
        assertLabel(
            control = ControlElement(
                scope = "#/properties/foo",
                label = DescriptionControlLabel(LabelDescription(text = "mega bar", show = false)),
            ),
            expected = LabelDescription(text = "mega bar", show = false),
        )
    }

    @Test
    fun `control relative - schema without title`() {
        assertLabel(
            control = ControlElement(scope = "/properties/foo"),
            schema = stringSchema(),
            expected = LabelDescription(text = "Foo", show = true),
        )
    }

    @Test
    fun `control without label string camel split - schema without title`() {
        assertLabel(
            control = ControlElement(scope = "#/properties/fooBarBaz"),
            schema = stringSchema(),
            expected = LabelDescription(text = "Foo Bar Baz", show = true),
        )
    }

    @Test
    fun `control with label string - schema without title`() {
        assertLabel(
            control = ControlElement(
                scope = "#/properties/foo",
                label = StringControlLabel("bar"),
            ),
            schema = stringSchema(),
            expected = LabelDescription(text = "bar", show = true),
        )
    }

    @Test
    fun `control with label boolean - schema without title`() {
        assertLabel(
            control = ControlElement(
                scope = "#/properties/foo",
                label = BooleanControlLabel(true),
            ),
            schema = stringSchema(),
            expected = LabelDescription(text = "Foo", show = true),
        )
    }

    @Test
    fun `control with label object empty - schema without title`() {
        assertLabel(
            control = ControlElement(
                scope = "#/properties/foo",
                label = DescriptionControlLabel(LabelDescription()),
            ),
            schema = stringSchema(),
            expected = LabelDescription(text = "Foo", show = true),
        )
    }

    @Test
    fun `control with label object text-only - schema without title`() {
        assertLabel(
            control = ControlElement(
                scope = "#/properties/foo",
                label = DescriptionControlLabel(LabelDescription(text = "mega bar")),
            ),
            schema = stringSchema(),
            expected = LabelDescription(text = "mega bar", show = true),
        )
    }

    @Test
    fun `control with label object visible-only - schema without title`() {
        assertLabel(
            control = ControlElement(
                scope = "#/properties/foo",
                label = DescriptionControlLabel(LabelDescription(show = true)),
            ),
            schema = stringSchema(),
            expected = LabelDescription(text = "Foo", show = true),
        )
    }

    @Test
    fun `control with label object full - schema without title`() {
        assertLabel(
            control = ControlElement(
                scope = "#/properties/foo",
                label = DescriptionControlLabel(LabelDescription(text = "mega bar", show = false)),
            ),
            schema = stringSchema(),
            expected = LabelDescription(text = "mega bar", show = false),
        )
    }

    @Test
    fun `control relative - schema with title`() {
        assertLabel(
            control = ControlElement(scope = "/properties/foo"),
            schema = titledStringSchema(),
            expected = LabelDescription(text = "Schema Title", show = true),
        )
    }

    @Test
    fun `control without label string camel split - schema with title`() {
        assertLabel(
            control = ControlElement(scope = "#/properties/fooBarBaz"),
            schema = titledStringSchema(),
            expected = LabelDescription(text = "Schema Title", show = true),
        )
    }

    @Test
    fun `control with label string - schema with title`() {
        assertLabel(
            control = ControlElement(
                scope = "#/properties/foo",
                label = StringControlLabel("bar"),
            ),
            schema = titledStringSchema(),
            expected = LabelDescription(text = "bar", show = true),
        )
    }

    @Test
    fun `control with label boolean - schema with title`() {
        assertLabel(
            control = ControlElement(
                scope = "#/properties/foo",
                label = BooleanControlLabel(true),
            ),
            schema = titledStringSchema(),
            expected = LabelDescription(text = "Schema Title", show = true),
        )
    }

    @Test
    fun `control with label object empty - schema with title`() {
        assertLabel(
            control = ControlElement(
                scope = "#/properties/foo",
                label = DescriptionControlLabel(LabelDescription()),
            ),
            schema = titledStringSchema(),
            expected = LabelDescription(text = "Schema Title", show = true),
        )
    }

    @Test
    fun `control with label object text-only - schema with title`() {
        assertLabel(
            control = ControlElement(
                scope = "#/properties/foo",
                label = DescriptionControlLabel(LabelDescription(text = "mega bar")),
            ),
            schema = titledStringSchema(),
            expected = LabelDescription(text = "mega bar", show = true),
        )
    }

    @Test
    fun `control with label object visible-only - schema with title`() {
        assertLabel(
            control = ControlElement(
                scope = "#/properties/foo",
                label = DescriptionControlLabel(LabelDescription(show = true)),
            ),
            schema = titledStringSchema(),
            expected = LabelDescription(text = "Schema Title", show = true),
        )
    }

    @Test
    fun `control with label object full - schema with title`() {
        assertLabel(
            control = ControlElement(
                scope = "#/properties/foo",
                label = DescriptionControlLabel(LabelDescription(text = "mega bar", show = false)),
            ),
            schema = titledStringSchema(),
            expected = LabelDescription(text = "mega bar", show = false),
        )
    }

    private fun assertLabel(
        control: ControlElement,
        schema: com.chobantonov.jsonfroms.core.models.JsonSchema? = null,
        expected: LabelDescription,
    ) {
        assertEquals(expected, createLabelDescriptionFrom(control, schema))
    }

    private fun stringSchema() = buildJsonSchema {
        put("type", JsonPrimitive("string"))
    }

    private fun titledStringSchema() = buildJsonSchema {
        put("type", kotlinx.serialization.json.JsonPrimitive("string"))
        put("title", kotlinx.serialization.json.JsonPrimitive("Schema Title"))
    }
}
