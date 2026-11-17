package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.cupertino.renderers.util.CupertinoTheme
import com.chobantonov.jsonfroms.cupertino.renderers.util.cupertinoColors
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class ButtonColorJvmUiTest {
    private val primary = Color(0xFF2345AA)
    private val destructive = Color(0xFFBB2233)

    @Test fun materialErrorUsesThemeErrorContainer() = color(false)
    @Test fun cupertinoErrorUsesThemeDestructiveContainer() = color(true)

    private fun color(cupertino: Boolean) = runComposeUiTest {
        var color by mutableStateOf("primary")
        val schema = JsonSchema(buildJsonObject { put("type", "object") })
        val renderForm: @androidx.compose.runtime.Composable () -> Unit = {
            Box(Modifier.width(320.dp)) {
                JsonForms(
                    data = buildJsonObject {}, schema = schema,
                    uischema = UnknownUiSchemaElement(
                        type = "Button", options = mapOf("fullWidth" to JsonPrimitive(true)),
                        properties = buildJsonObject {
                            put("label", "Semantic color")
                            put("action", "save")
                            put("color", color)
                        },
                    ),
                    renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                    config = mapOf(ExtendedActionHandlerConfigKey to ExtendedActionHandler { _, _ -> }),
                )
            }
        }
        setContent {
            if (cupertino) CupertinoTheme(
                darkTheme = false,
                colors = cupertinoColors(false).copy(accent = primary, destructive = destructive),
            ) { renderForm() }
            else MaterialTheme(colorScheme = lightColorScheme(primary = primary, error = destructive)) {
                renderForm()
            }
        }

        fun containerPixel(): Color {
            val image = onNode(hasClickAction()).captureToImage().toPixelMap()
            return image[30, image.height / 2]
        }
        val primaryPixel = containerPixel()
        assertTrue(primaryPixel.blue > primaryPixel.red, "Primary button should use the blue theme accent: $primaryPixel")
        runOnIdle { color = "error" }
        val errorPixel = containerPixel()
        assertTrue(errorPixel.red > errorPixel.blue, "Error button should use the red theme token: $errorPixel")
    }
}
