package com.chobantonov.jsonfroms.core.actions

import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.models.JsonData
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.UiSchemaTester
import com.chobantonov.jsonfroms.core.validation.JsonSchemaValidator
import com.chobantonov.jsonfroms.core.validation.ValidationIssue

data class InitActionOptions(
    val validator: JsonSchemaValidator? = null,
    val validationMode: ValidationMode? = null,
    val additionalErrors: List<ValidationIssue> = emptyList(),
)

sealed interface UpdateArrayContext

const val ADD = "ADD"
const val REMOVE = "REMOVE"
const val MOVE = "MOVE"

data class AddArrayContext(
    val values: List<JsonData>,
) : UpdateArrayContext {
    val type: String = ADD
}

data class RemoveArrayContext(
    val indices: List<Int>,
) : UpdateArrayContext {
    val type: String = REMOVE
}

data class MoveOperation(
    val from: Int?,
    val to: Int?,
)

data class MoveArrayContext(
    val moves: List<MoveOperation>,
) : UpdateArrayContext {
    val type: String = MOVE
}

sealed interface JsonFormsAction

data class InitAction(
    val data: JsonData,
    val schema: JsonSchema,
    val uiSchema: UiSchemaElement? = null,
    val options: InitActionOptions? = null,
    val dataProvided: Boolean = true,
) : JsonFormsAction

data class UpdateCoreAction(
    val data: JsonData,
    val schema: JsonSchema,
    val uiSchema: UiSchemaElement? = null,
    val options: InitActionOptions? = null,
    val dataProvided: Boolean = true,
) : JsonFormsAction

data class UpdateAction(
    val path: String,
    val updater: (JsonData?) -> JsonData?,
    val context: Any? = null,
    /** Explicitly distinguishes an absent root value from JSON null. Ignored for nested paths. */
    val dataProvided: Boolean? = null,
) : JsonFormsAction

data class UpdateErrorsAction(
    val errors: List<ValidationIssue>,
) : JsonFormsAction

data class SetSchemaAction(
    val schema: JsonSchema,
) : JsonFormsAction

data class SetUISchemaAction(
    val uiSchema: UiSchemaElement,
) : JsonFormsAction

data class SetValidationModeAction(
    val validationMode: ValidationMode,
) : JsonFormsAction

data class SetValidatorAction(
    val validator: JsonSchemaValidator,
) : JsonFormsAction

data class SetConfigAction(
    val config: Map<String, Any?>,
) : JsonFormsAction

data class AddRendererAction<TRenderer>(
    val tester: RankedTester,
    val renderer: TRenderer,
) : JsonFormsAction

data class RemoveRendererAction(
    val tester: RankedTester,
) : JsonFormsAction

data class AddCellRendererAction<TCell>(
    val tester: RankedTester,
    val cell: TCell,
) : JsonFormsAction

data class RemoveCellRendererAction(
    val tester: RankedTester,
) : JsonFormsAction

data class AddUISchemaAction(
    val tester: UiSchemaTester,
    val uiSchema: UiSchemaElement,
) : JsonFormsAction

data class RemoveUISchemaAction(
    val tester: UiSchemaTester,
) : JsonFormsAction

object Actions {
    fun init(
        data: JsonData,
        schema: JsonSchema = Generate.jsonSchema(data),
        uischema: Any? = null,
        options: InitActionOptions? = null,
        dataProvided: Boolean = true,
    ): InitAction = InitAction(
        data = data,
        dataProvided = dataProvided,
        schema = schema,
        uiSchema = (uischema as? UiSchemaElement) ?: Generate.uiSchema(schema),
        options = options,
    )

    fun updateCore(
        data: JsonData,
        schema: JsonSchema,
        uischema: UiSchemaElement? = null,
        options: InitActionOptions? = null,
        dataProvided: Boolean = true,
    ): UpdateCoreAction = UpdateCoreAction(
        data = data,
        dataProvided = dataProvided,
        schema = schema,
        uiSchema = uischema,
        options = options,
    )

    fun update(
        path: String,
        updater: (JsonData?) -> JsonData?,
        context: Any? = null,
        dataProvided: Boolean? = null,
    ): UpdateAction = UpdateAction(
        path = path,
        updater = updater,
        context = context,
        dataProvided = dataProvided,
    )

    fun registerUISchema(
        tester: UiSchemaTester,
        uiSchema: UiSchemaElement,
    ): AddUISchemaAction = AddUISchemaAction(
        tester = tester,
        uiSchema = uiSchema,
    )

    fun unregisterUISchema(
        tester: UiSchemaTester,
    ): RemoveUISchemaAction = RemoveUISchemaAction(
        tester = tester,
    )

    fun isUpdateArrayContext(context: Any?): Boolean =
        when (context) {
            is AddArrayContext -> context.values.isNotEmpty()
            is RemoveArrayContext -> context.indices.isNotEmpty()
                && context.indices.all { it >= 0 }
            is MoveArrayContext -> context.moves.isNotEmpty()
                && context.moves.all { move -> move.from != null && move.to != null }
            is Map<*, *> -> isUpdateArrayContextMap(context)
            else -> false
        }

    private fun isUpdateArrayContextMap(context: Map<*, *>): Boolean {
        val type = context["type"] as? String ?: return false
        return when (type) {
            ADD -> {
                val values = context["values"] as? List<*> ?: return false
                values.isNotEmpty()
            }

            REMOVE -> {
                val indices = context["indices"] as? List<*> ?: return false
                indices.isNotEmpty() && indices.all { it is Int }
            }

            MOVE -> {
                val moves = context["moves"] as? List<*> ?: return false
                moves.isNotEmpty() && moves.all { move ->
                    val entry = move as? Map<*, *> ?: return@all false
                    entry["from"] is Int && entry["to"] is Int
                }
            }

            else -> false
        }
    }
}
