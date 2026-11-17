package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.mappers.createCombinatorRenderInfos
import com.chobantonov.jsonfroms.core.mappers.CombinatorKeyword
import com.chobantonov.jsonfroms.core.mappers.findFittingSchemaIndex
import com.chobantonov.jsonfroms.core.util.resolveSchema
import com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class TreeMaxDepthFixtureTest {
    private fun fixture() = getBuiltInExamples().first { it.name == "spec-tree-max-depth" }

    private fun withSixthLevel(original: JsonElement): JsonObject {
        fun appendSixth(node: JsonObject, level: Int): JsonObject {
            if (level == 5) return JsonObject(node + ("children" to JsonArray(listOf(
                JsonObject(mapOf("kind" to JsonPrimitive("file"), "name" to JsonPrimitive("Level 6"))),
            ))))
            val children = node.getValue("children").jsonArray
            return JsonObject(node + ("children" to JsonArray(listOf(
                appendSixth(children.single().jsonObject, level + 1),
            ))))
        }
        return JsonObject(original.jsonObject + ("tree" to appendSixth(
            original.jsonObject.getValue("tree").jsonObject, 1,
        )))
    }

    @Test fun importedFiveLevelTreeIsValidAndSixthLevelIsRejected() {
        val example = fixture()
        val validator = OptimumCodeJsonSchemaValidator()
        val schema = requireNotNull(example.schema)
        assertTrue(validator.validate(schema, example.data).isEmpty())

        val invalid = withSixthLevel(example.data)
        val errors = validator.validate(schema, invalid)
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.instancePath.contains("children") })
    }

    @Test fun materialImportedRootMountsWithoutWriting() = mounts(false)
    @Test fun cupertinoImportedRootMountsWithoutWriting() = mounts(true)
    @Test fun materialSixthLevelCanBeRemoved() = repair(false)
    @Test fun cupertinoSixthLevelCanBeRemoved() = repair(true)

    @Test fun importedFoldersSelectFolderBranchAtEveryLevel() {
        val example = fixture()
        val schema = requireNotNull(example.schema)
        val control = (example.uischema as VerticalLayout).elements.filterIsInstance<ControlElement>().single()
        val validator = OptimumCodeJsonSchemaValidator()
        var node = example.data.jsonObject.getValue("tree")
        var invalidNode = withSixthLevel(example.data).getValue("tree")
        for (level in 1..5) {
            val nodeSchema = requireNotNull(resolveSchema(schema, "#/definitions/node$level", schema))
            val infos = createCombinatorRenderInfos(nodeSchema.oneOf.orEmpty(), schema,
                CombinatorKeyword.oneOf, control, "tree")
            assertEquals(1, findFittingSchemaIndex(infos.map { it.schema }, node,
                validator, schema), "folder branch at level $level")
            assertEquals(1, findFittingSchemaIndex(infos.map { it.schema }, invalidNode,
                validator, schema), "invalid descendant retains folder branch at level $level")
            if (level < 5) {
                node = node.jsonObject.getValue("children").jsonArray.single()
                invalidNode = invalidNode.jsonObject.getValue("children").jsonArray.single()
            }
        }
    }

    private fun mounts(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        val control = (example.uischema as VerticalLayout).elements.filterIsInstance<ControlElement>().single()
        var changes = 0
        setContent {
            JsonForms(data = example.data, schema = example.schema, uischema = control,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ })
        }
        onNodeWithText("Level 1").assertExists()
        onAllNodesWithText("Folder").assertCountEquals(5)
        onNodeWithText("Level 5").assertExists()
        onNodeWithText("No applicable renderer found.").assertDoesNotExist()
        val addActions = onAllNodes(hasContentDescription("Add item"))
        addActions.assertCountEquals(5)
        addActions.onFirst().assertIsEnabled()
        addActions.onLast().assertIsNotEnabled()
        assertEquals(0, changes)
    }

    private fun repair(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        val control = (example.uischema as VerticalLayout).elements.filterIsInstance<ControlElement>().single()
        var data by mutableStateOf<JsonElement>(withSixthLevel(example.data))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = control,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        assertEquals(0, changes)
        val deleteActions = onAllNodes(hasContentDescription("Delete item"))
        deleteActions.assertCountEquals(5)
        deleteActions.onLast().performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Delete this value?").assertExists()
        onNodeWithText("Yes").performClick()
        runOnIdle {
            assertEquals(example.data, data)
            assertEquals(1, changes)
        }
    }

}
