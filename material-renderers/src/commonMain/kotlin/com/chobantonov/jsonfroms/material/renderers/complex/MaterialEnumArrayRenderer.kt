package com.chobantonov.jsonfroms.material.renderers.complex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.rememberInitialControlFocusRequester
import com.chobantonov.jsonfroms.compose.strictControlBooleanOption
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.i18n.arrayChoiceLocaleBundles
import com.chobantonov.jsonfroms.core.i18n.translateControlRequired
import com.chobantonov.jsonfroms.core.i18n.translateChoiceGroupOrientation
import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.ContainsRemovalGuard
import com.chobantonov.jsonfroms.core.mappers.mapStateToMultiEnumControlProps
import com.chobantonov.jsonfroms.core.mappers.removeArrayChoiceOccurrence
import com.chobantonov.jsonfroms.core.mappers.checkboxChoiceVertical
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.schemaMatches
import com.chobantonov.jsonfroms.core.testers.schemaSubPathMatches
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.util.hasType
import com.chobantonov.jsonfroms.core.util.jsonSchemaChoiceEquals
import com.chobantonov.jsonfroms.core.util.acceptsRestrictedCountChange
import com.chobantonov.jsonfroms.core.util.resolveRestrict
import com.chobantonov.jsonfroms.core.util.resolveArrayActionPolicy
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val MaterialEnumArrayRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialEnumArrayRenderer(props)
}

@Composable
fun MaterialEnumArrayRenderer(
    props: ComposeJsonFormsRendererProps,
) {
    val control = props.uischema as? ControlElement
        ?: error("MaterialEnumArrayRenderer can only render Control elements")
    val controlProps = mapStateToMultiEnumControlProps(
        state = props.state,
        ownProps = OwnPropsOfControl(
            controlUiSchema = control,
            own = OwnPropsOfRenderer(
                uischema = props.uischema,
                schema = props.schema,
                enabled = props.enabled,
                path = props.path,
                renderers = props.renderers,
                cells = props.cells,
            ),
        ),
    )

    if (!controlProps.visible) {
        return
    }

    val selectedValues = (controlProps.data as? JsonArray).orEmpty()
    val i18n = props.state.jsonforms.i18n
    val hideRequiredAsterisk = strictControlBooleanOption(
        controlProps.config + control.options, "hideRequiredAsterisk") ?: false
    val requiredDescription = if (controlProps.required) translateControlRequired(i18n) else null
    val showUnfocusedDescription = strictControlBooleanOption(
        controlProps.config + control.options, "showUnfocusedDescription") == true
    val vertical = checkboxChoiceVertical(control.options)
    fun label(key: String, fallback: String): String = translateRendererText(
        i18n.translate, i18n.locale, key, fallback, arrayChoiceLocaleBundles,
        i18nKeyPrefix = controlProps.i18nKeyPrefix)
    val outsideChoicesLabel = label("arrayChoice.outsideChoices", "Current value outside choices")
    val removeLabel = label("arrayChoice.remove", "Remove choice")
    val restrict = resolveRestrict(control.options["restrict"], controlProps.config)
    val actionPolicy = resolveArrayActionPolicy(control.options, controlProps.config)
    val removalGuard = remember(controlProps.schema, props.state.jsonforms.core.validator,
        props.state.jsonforms.core.data, controlProps.path, selectedValues) {
        ContainsRemovalGuard(controlProps.schema, props.state.jsonforms.core.validator,
            props.state.jsonforms.core.data, controlProps.path, JsonArray(selectedValues))
    }
    fun canRemoveAt(index: Int) = !restrict || removalGuard.canRemove(index)
    fun selectedIndex(value: JsonElement) = selectedValues.indexOfFirst { jsonSchemaChoiceEquals(it, value) }
    fun canToggleChoice(index: Int): Boolean {
        val option = controlProps.options[index]
        val selected = selectedIndex(option.value)
        val checked = selected >= 0
        val nextCount = if (checked) selectedValues.size - 1 else selectedValues.size + 1
        return !(if (checked) actionPolicy.disableRemove else actionPolicy.disableAdd) &&
            acceptsRestrictedCountChange(selectedValues.size, nextCount,
                controlProps.schema.minItems, controlProps.schema.maxItems, restrict) &&
            (!checked || canRemoveAt(selected))
    }
    val initialFocusIndex = remember(controlProps.path) {
        if (!controlProps.enabled || controlProps.readonly) -1 else
            controlProps.options.indices.firstOrNull {
                selectedIndex(controlProps.options[it].value) >= 0 && canToggleChoice(it)
            } ?: controlProps.options.indices.firstOrNull(::canToggleChoice) ?: -1
    }
    val focusRequester = rememberInitialControlFocusRequester(controlProps.path,
        controlProps.config + control.options, initialFocusIndex >= 0)
    var touched by remember(controlProps.path) { mutableStateOf(false) }
    var hasReceivedFocus by remember(controlProps.path) { mutableStateOf(false) }
    var focused by remember(controlProps.path) { mutableStateOf(false) }
    val focusGeneration = remember(controlProps.path) { intArrayOf(0) }
    val focusScope = rememberCoroutineScope()
    val visibleError = if (touched) controlProps.errors else controlProps.errorsBeforeTouch
    val visibleDescription = controlProps.description?.takeIf {
        it.isNotBlank() && (focused || showUnfocusedDescription)
    }
    val invalidStatus = controlProps.data.takeIf { controlProps.dataProvided && it !is JsonArray }
        ?.let { "$outsideChoicesLabel: $it" }
    val accessibleState = listOfNotNull(requiredDescription, invalidStatus, visibleDescription).joinToString(". ")
    val accessibleError = listOfNotNull(invalidStatus, visibleError.takeIf(String::isNotBlank))
        .distinct().joinToString(". ")
    val choiceRows: @Composable () -> Unit = {
        controlProps.options.forEachIndexed { index, option ->
            val checked = selectedIndex(option.value) >= 0
            val canToggle = canToggleChoice(index)
            Row(
                modifier = if (vertical) Modifier.fillMaxWidth() else Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(
                    modifier = (if (index == initialFocusIndex) Modifier.focusRequester(focusRequester)
                        else Modifier).semantics {
                        if (accessibleState.isNotBlank()) stateDescription = accessibleState
                        if (accessibleError.isNotBlank()) error(accessibleError)
                    },
                    checked = checked,
                    enabled = controlProps.enabled && !controlProps.readonly && canToggle,
                    onCheckedChange = { newValue ->
                        props.dispatch(
                            UpdateAction(
                                path = controlProps.path,
                                updater = update@{ current ->
                                    if (!canToggle || !controlProps.enabled || controlProps.readonly) return@update current
                                    if (current is JsonArray && current != JsonArray(selectedValues)) return@update current
                                    val items = (current as? JsonArray)?.toMutableList() ?: mutableListOf()
                                    if (newValue) {
                                        if (items.none { jsonSchemaChoiceEquals(it, option.value) }) items += option.value
                                    } else {
                                        val index = items.indexOfFirst { jsonSchemaChoiceEquals(it, option.value) }
                                        if (index >= 0 && canRemoveAt(index)) items.removeAt(index)
                                    }
                                    if (acceptsRestrictedCountChange(
                                            (current as? JsonArray)?.size ?: 0, items.size,
                                            controlProps.schema.minItems, controlProps.schema.maxItems, restrict,
                                        )) JsonArray(items) else current
                                },
                            ),
                        )
                    },
                )
                Text(option.label)
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth().onFocusChanged { state ->
            val generation = ++focusGeneration[0]
            if (state.hasFocus) {
                focused = true
                hasReceivedFocus = true
            } else if (hasReceivedFocus) {
                touched = true
                focusScope.launch {
                    // Keep the choice row stable through the pointer gesture that moves focus away.
                    delay(150)
                    if (generation == focusGeneration[0]) focused = false
                }
            }
        }.focusGroup().selectableGroup().semantics {
            stateDescription = translateChoiceGroupOrientation(i18n, vertical)
        },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = computeLabel(controlProps.label, controlProps.required, hideRequiredAsterisk),
            style = MaterialTheme.typography.titleMedium,
        )
        if (vertical) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { choiceRows() }
        else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) { choiceRows() }
        if (invalidStatus != null) {
            Text(invalidStatus,
                color = MaterialTheme.colorScheme.error)
        }
        selectedValues.forEachIndexed { index, value ->
            if (controlProps.options.none { jsonSchemaChoiceEquals(it.value, value) }) {
                val canRemove = !actionPolicy.disableRemove && acceptsRestrictedCountChange(
                    selectedValues.size, selectedValues.size - 1,
                    controlProps.schema.minItems, null, restrict) && canRemoveAt(index)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$outsideChoicesLabel: $value", Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.error)
                    OutlinedButton(
                        enabled = controlProps.enabled && !controlProps.readonly && canRemove,
                        modifier = Modifier.semantics {
                            contentDescription = "$removeLabel: $value, ${index + 1}"
                        },
                        onClick = {
                            props.dispatch(UpdateAction(controlProps.path, updater = update@{ current ->
                                if (!canRemove || !controlProps.enabled || controlProps.readonly) return@update current
                                if (current != JsonArray(selectedValues) || !canRemoveAt(index)) return@update current
                                removeArrayChoiceOccurrence(current, index, value,
                                    controlProps.schema.minItems, restrict)
                            }))
                            touched = true
                        },
                    ) { Text(removeLabel) }
                }
            }
        }
        visibleDescription?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
        visibleError.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun hasOneOfItems(schema: com.chobantonov.jsonfroms.core.models.JsonSchema): Boolean =
    schema.oneOf?.isNotEmpty() == true && schema.oneOf.orEmpty().all { it["const"] != null }

private fun hasEnumItems(schema: com.chobantonov.jsonfroms.core.models.JsonSchema): Boolean =
    hasType(schema, "string") && schema["enum"] is JsonArray

val materialEnumArrayRendererTester: RankedTester = rankWith(
    // Automatic checkbox selection yields to an explicit table or data-grid fallback.
    4,
    and(
        uiTypeIs("Control"),
        schemaMatches { schema ->
            hasType(schema, "array") &&
                schema["uniqueItems"] == JsonPrimitive(true) &&
                schema["items"] !is JsonArray
        },
        schemaSubPathMatches("items") { schema, rootSchema ->
            val resolved = (schema["\$ref"] as? JsonPrimitive)?.content?.let {
                com.chobantonov.jsonfroms.core.util.resolveSchema(rootSchema, it, rootSchema)
            } ?: schema
            hasOneOfItems(resolved) || hasEnumItems(resolved)
        },
    ),
)
