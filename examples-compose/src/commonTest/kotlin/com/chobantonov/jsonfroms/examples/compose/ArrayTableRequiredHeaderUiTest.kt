package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ArrayTableRequiredHeaderUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","required":["name","code"],"properties":{"name":{"type":"string"},"code":{"type":"string"},"note":{"type":"string"}}}}}}"""
    ).jsonObject)
    private val data = Json.parseToJsonElement("""{"rows":[{"name":"Ada","code":"A1","note":"hello"}]}""")
    private val ui = ControlElement("#/properties/rows", options = mapOf(
        "table" to JsonPrimitive(true),
        "hideRequiredAsterisk" to JsonPrimitive(false),
        "cells" to Json.parseToJsonElement("""{"name":{"hideRequiredAsterisk":true}}"""),
        "columnDefs" to Json.parseToJsonElement("""[
          {"field":"name","headerName":"Required Name"},
          {"field":"code","headerName":"Required Code"},
          {"field":"note","headerName":"Optional Note"},
          {"field":"summary","scope":"#","headerName":"Whole Row"}
        ]"""),
    ))

    @Test fun materialRequiredHeadersUseCellCollectionAndGlobalPrecedence() = verify(false)
    @Test fun cupertinoRequiredHeadersUseCellCollectionAndGlobalPrecedence() = verify(true)

    private fun verify(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ui,
                config = mapOf("hideRequiredAsterisk" to true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onNodeWithText("Required Name").assertExists()
        onNodeWithText("Required Name*").assertDoesNotExist()
        onNodeWithText("Required Code*").assertExists()
        onNodeWithText("Optional Note").assertExists()
        onNodeWithText("Optional Note*").assertDoesNotExist()
        onNodeWithText("Whole Row").assertExists()
        onNodeWithText("Whole Row*").assertDoesNotExist()
    }
}
