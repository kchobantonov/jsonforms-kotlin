package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.ImageViewElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageViewSourceTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"photo":{"type":"string"},"count":{"type":"integer"},"nested":{"type":"array","items":{"type":"object","properties":{"photo":{"type":"string"}}}}}}"""
    ) as JsonObject)
    private val data = Json.parseToJsonElement(
        """{"photo":"/root.png","count":7,"nested":[{"photo":"/first.png"},{"photo":"javascript:alert(1)"}]}"""
    )
    private val policy = UrlPolicy()

    @Test
    fun definedSourceWinsIncludingEmptyAndRefusedValues() {
        val scoped = "#/properties/photo"
        assertEquals(ImageSourceResolution("/direct.png"), resolveImageSource(
            ImageViewElement("/direct.png", scoped, "Portrait"), schema, schema, data, "", policy,
        ))
        assertEquals(ImageSourceResolution(), resolveImageSource(
            ImageViewElement("", scoped, "Portrait"), schema, schema, data, "", policy,
        ))
        assertEquals(ImageSourceResolution(diagnostic = ImageSourceDiagnostic.UrlRefused), resolveImageSource(
            ImageViewElement("javascript:alert(1)", scoped, "Portrait"), schema, schema, data, "", policy,
        ))
    }

    @Test
    fun scopedSourceUsesCurrentArrayItemAndRejectsWrongValues() {
        val itemSchema = resolveSchema(schema, "#/properties/nested/items", schema)!!
        val image = ImageViewElement(scope = "#/properties/photo", alt = "Portrait")
        assertEquals(ImageSourceResolution("/first.png"), resolveImageSource(
            image, itemSchema, schema, data, "nested.0", policy,
        ))
        assertEquals(ImageSourceResolution(diagnostic = ImageSourceDiagnostic.UrlRefused), resolveImageSource(
            image, itemSchema, schema, data, "nested.1", policy,
        ))
        assertEquals(ImageSourceResolution(diagnostic = ImageSourceDiagnostic.ScopeNotString), resolveImageSource(
            ImageViewElement(scope = "#/properties/count", alt = "Count"), schema, schema, data, "", policy,
        ))
        assertEquals(ImageSourceResolution(diagnostic = ImageSourceDiagnostic.ScopeNotString), resolveImageSource(
            ImageViewElement(scope = "#/properties/absent", alt = "Absent"), schema, schema, data, "", policy,
        ))
        assertEquals(ImageSourceResolution(diagnostic = ImageSourceDiagnostic.NoSource), resolveImageSource(
            ImageViewElement(alt = "None"), schema, schema, data, "", policy,
        ))
        val badData = Json.parseToJsonElement("""{"photo":42}""")
        assertEquals(ImageSourceResolution(diagnostic = ImageSourceDiagnostic.NonStringSource), resolveImageSource(
            image, schema, schema, badData, "", policy,
        ))
        assertEquals(ImageSourceResolution(), resolveImageSource(
            image, schema, schema, Json.parseToJsonElement("""{"photo":""}"""), "", policy,
        ))
    }

    @Test
    fun imageDataRequiresOptInAndImageMimeType() {
        val image = ImageViewElement(src = "data:image/png;base64,AAAA", alt = "")
        assertEquals(ImageSourceDiagnostic.UrlRefused, resolveImageSource(
            image, schema, schema, data, "", policy,
        ).diagnostic)
        assertEquals(image.src, resolveImageSource(
            image, schema, schema, data, "", policy.copy(allowImageDataUrls = true),
        ).url)
        assertEquals(ImageSourceDiagnostic.UrlRefused, resolveImageSource(
            image.copy(src = "data:text/html,<h1>x</h1>"), schema, schema, data, "",
            policy.copy(allowImageDataUrls = true),
        ).diagnostic)
    }
}
