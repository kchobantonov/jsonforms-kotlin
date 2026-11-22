package com.chobantonov.jsonfroms.material.renderers.complex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
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
import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.addFiniteStringChoice
import com.chobantonov.jsonfroms.core.mappers.addChipValue
import com.chobantonov.jsonfroms.core.mappers.choiceAutocomplete
import com.chobantonov.jsonfroms.core.mappers.finiteChipsChoices
import com.chobantonov.jsonfroms.core.mappers.finiteStringArrayChoices
import com.chobantonov.jsonfroms.core.mappers.mapStateToMultiEnumControlProps
import com.chobantonov.jsonfroms.core.mappers.removeArrayChoiceOccurrence
import com.chobantonov.jsonfroms.core.mappers.ContainsRemovalGuard
import com.chobantonov.jsonfroms.core.mappers.replaceArrayChoiceOccurrence
import com.chobantonov.jsonfroms.core.mappers.supportsMultiSelectArray
import com.chobantonov.jsonfroms.core.mappers.supportsChipsArray
import com.chobantonov.jsonfroms.core.mappers.StringArrayChoice
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.optionIs
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.schemaMatches
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.util.acceptsRestrictedCountChange
import com.chobantonov.jsonfroms.core.util.resolveRestrict
import com.chobantonov.jsonfroms.core.util.resolveArrayActionPolicy
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

val materialMultiSelectTester: RankedTester = rankWith(6, and(
    uiTypeIs("Control"), optionIs("variant", "multi-select"),
    schemaMatches { schema, root -> supportsMultiSelectArray(schema, root) },
))
val materialChipsTester: RankedTester = rankWith(6, and(
    uiTypeIs("Control"), optionIs("variant", "chips"),
    schemaMatches { schema, root -> supportsChipsArray(schema, root) },
))

val MaterialMultiSelectRenderer = ComposeJsonFormsRenderer { props -> MaterialMultiSelectRenderer(props) }
val MaterialChipsRenderer = ComposeJsonFormsRenderer { props -> MaterialArrayChoiceRenderer(props, chips = true) }

@Composable
fun MaterialMultiSelectRenderer(props: ComposeJsonFormsRendererProps) = MaterialArrayChoiceRenderer(props, chips = false)

@Composable
private fun MaterialArrayChoiceRenderer(props: ComposeJsonFormsRendererProps, chips: Boolean) {
    val control = props.uischema as? ControlElement ?: return
    val state = mapStateToMultiEnumControlProps(props.state, OwnPropsOfControl(
        controlUiSchema = control,
        own = OwnPropsOfRenderer(uischema = control, schema = props.schema,
            enabled = props.enabled, path = props.path, renderers = props.renderers, cells = props.cells),
    ))
    if (!state.visible) return
    com.chobantonov.jsonfroms.compose.ReportUnsupportedInputAdornments(props,
        if (chips) "chips" else "multi-select")
    val choices = if (chips) finiteChipsChoices(state.schema, state.rootSchema)
        else finiteStringArrayChoices(state.schema, state.rootSchema) ?: return
    val selected = (state.data as? JsonArray)?.toList().orEmpty()
    val restrict = resolveRestrict(control.options["restrict"], state.config)
    val removalGuard = remember(state.schema, props.state.jsonforms.core.validator,
        props.state.jsonforms.core.data, state.path, selected) {
        ContainsRemovalGuard(state.schema, props.state.jsonforms.core.validator,
            props.state.jsonforms.core.data, state.path, JsonArray(selected))
    }
    val interactive = state.enabled && !state.readonly
    val actionPolicy = resolveArrayActionPolicy(control.options, state.config)
    val canAdd = interactive && !actionPolicy.disableAdd && (choices == null || choices.any { state.schema.uniqueItems != true || it.value !in selected }) &&
        acceptsRestrictedCountChange(selected.size, selected.size + 1, null, state.schema.maxItems, restrict)
    val i18n = props.state.jsonforms.i18n
    val hideRequiredAsterisk = strictControlBooleanOption(
        state.config + control.options, "hideRequiredAsterisk") ?: false
    val requiredDescription = if (state.required) translateControlRequired(i18n) else null
    val showUnfocusedDescription = strictControlBooleanOption(
        state.config + control.options, "showUnfocusedDescription") == true
    fun label(key: String, fallback: String): String = translateRendererText(
        i18n.translate, i18n.locale, key, fallback, arrayChoiceLocaleBundles,
        i18nKeyPrefix = state.i18nKeyPrefix)
    val addLabel = label("arrayChoice.add", "Add choice")
    val removeLabel = label("arrayChoice.remove", "Remove choice")
    val replaceLabel = label("arrayChoice.replace", "Replace choice")
    val currentLabel = label("arrayChoice.current", "Current value")
    val outsideChoicesLabel = label("arrayChoice.outsideChoices", "Current value outside choices")
    val searchLabel = label("arrayChoice.search", "Search choices")
    val noResultsLabel = label("arrayChoice.noResults", "No matches")
    val fieldName = state.label.ifBlank { state.path }
    val searchable = choiceAutocomplete(control.options["autocomplete"], state.config)
    val focusRequester = rememberInitialControlFocusRequester(state.path,
        state.config + control.options, if (chips && choices == null) interactive else canAdd)
    var expanded by remember(state.path) { mutableStateOf(false) }
    var replacingIndex by remember(state.path) { mutableStateOf<Int?>(null) }
    LaunchedEffect(selected) { replacingIndex = null }
    LaunchedEffect(interactive) {
        if (!interactive) {
            expanded = false
            replacingIndex = null
        }
    }
    var query by remember(state.path) { mutableStateOf("") }
    var draft by remember(state.path) { mutableStateOf("") }
    var touched by remember(state.path) { mutableStateOf(false) }
    var focused by remember(state.path) { mutableStateOf(false) }
    val visibleError = if (touched) state.errors else state.errorsBeforeTouch
    val visibleDescription = state.description?.takeIf {
        it.isNotBlank() && (focused || showUnfocusedDescription)
    }
    val invalidStored = state.data.takeIf { state.dataProvided && it !is JsonArray }
    val invalidStatus = invalidStored?.let { "$currentLabel: $it" }
    val accessibleState = listOfNotNull(requiredDescription, invalidStatus, visibleDescription).joinToString(". ")
    val accessibleError = listOfNotNull(invalidStatus, visibleError.takeIf(String::isNotBlank))
        .distinct().joinToString(". ")
    fun choiceLabel(choice: StringArrayChoice): String {
        val fallback = state.options.firstOrNull { it.value == choice.value }?.label ?: choice.label
        return i18n.translate?.translate("${state.i18nKeyPrefix}.${choice.value.content}", fallback, emptyMap())
            ?: fallback
    }
    fun shown(value: JsonElement): String = choices?.firstOrNull { it.value == value }?.let(::choiceLabel)
        ?: (value as? JsonPrimitive)?.takeIf { it.isString }?.content ?: value.toString()

    Column(Modifier.fillMaxWidth().onFocusChanged { focused = it.hasFocus },
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (state.label.isNotBlank()) Text(computeLabel(state.label, state.required, hideRequiredAsterisk))
        if (invalidStatus != null) {
            Text(invalidStatus, color = MaterialTheme.colorScheme.error)
        }
        selected.forEachIndexed { index, value ->
            val valueLabel = shown(value)
            val canRemove = interactive && !actionPolicy.disableRemove && acceptsRestrictedCountChange(
                selected.size, selected.size - 1, state.schema.minItems, null, restrict) &&
                (!restrict || removalGuard.canRemove(index))
            val replacements = choices.orEmpty().filter { choice ->
                choice.value != value &&
                    (state.schema.uniqueItems != true || choice.value !in selected)
            }
            val canReplace = interactive && !actionPolicy.disableAdd && !actionPolicy.disableRemove &&
                restrict && replacements.isNotEmpty()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(valueLabel)
                    if (choices != null && choices.none { it.value == value }) {
                        Text("$outsideChoicesLabel: $value", color = MaterialTheme.colorScheme.error)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    OutlinedButton(
                        enabled = canRemove,
                        modifier = Modifier.semantics {
                            contentDescription = "$removeLabel: $valueLabel, ${index + 1}"
                        },
                        onClick = {
                            props.dispatch(UpdateAction(state.path, updater = update@{ current ->
                                if (!canRemove || !state.enabled || state.readonly) return@update current
                                if (current != JsonArray(selected) || (restrict && !removalGuard.canRemove(index)))
                                    return@update current
                                removeArrayChoiceOccurrence(current, index, value, state.schema.minItems, restrict)
                            }))
                            touched = true
                        },
                    ) { Text("$removeLabel: $valueLabel") }
                    if (canReplace) Box {
                        OutlinedButton(onClick = { replacingIndex = index },
                            modifier = Modifier.semantics {
                                contentDescription = "$replaceLabel: $valueLabel, ${index + 1}"
                            }) { Text("$replaceLabel: $valueLabel") }
                        DropdownMenu(expanded = replacingIndex == index,
                            onDismissRequest = { replacingIndex = null }) {
                            replacements.forEach { choice ->
                                val choiceText = choiceLabel(choice)
                                DropdownMenuItem(text = { Text(choiceText) }, onClick = {
                                    props.dispatch(UpdateAction(state.path, updater = update@{ current ->
                                        if (!canReplace || !state.enabled || state.readonly) return@update current
                                        replaceArrayChoiceOccurrence(current, index, value, choice.value,
                                            choices.orEmpty(), state.schema.uniqueItems == true)
                                    }))
                                    replacingIndex = null
                                    touched = true
                                })
                            }
                        }
                    }
                }
            }
        }
        if (chips && choices == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = draft, onValueChange = { draft = it },
                    label = { Text(label("arrayChoice.token", "New token")) },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester).semantics {
                        if (accessibleState.isNotBlank()) stateDescription = accessibleState
                        if (accessibleError.isNotBlank()) error(accessibleError)
                    },
                    enabled = interactive, singleLine = true)
                OutlinedButton(modifier = Modifier.semantics {
                    contentDescription = "$addLabel: $fieldName"
                }, onClick = {
                    val token = draft
                    props.dispatch(UpdateAction(state.path, updater = update@{ current ->
                        if (!canAdd || !state.enabled || state.readonly) return@update current
                        addChipValue(current, token, null, state.schema.uniqueItems == true,
                            state.schema.maxItems, restrict)
                    }))
                    draft = ""
                    touched = true
                }, enabled = canAdd && draft.isNotEmpty()) { Text(addLabel) }
            }
        } else androidx.compose.foundation.layout.Box {
            OutlinedButton(onClick = { expanded = true }, enabled = canAdd,
                modifier = Modifier.focusRequester(focusRequester).semantics {
                    contentDescription = "$addLabel: $fieldName"
                    if (accessibleState.isNotBlank()) stateDescription = accessibleState
                    if (accessibleError.isNotBlank()) error(accessibleError)
                }) {
                Text(addLabel)
            }
            DropdownMenu(expanded = expanded && canAdd, onDismissRequest = { expanded = false }) {
                if (searchable) OutlinedTextField(value = query, onValueChange = { query = it },
                    label = { Text(searchLabel) }, singleLine = true)
                val available = choices.orEmpty().filter { state.schema.uniqueItems != true || it.value !in selected }.filter { choice ->
                    !searchable || choiceLabel(choice).contains(query, ignoreCase = true)
                }
                if (available.isEmpty()) Text(noResultsLabel)
                available.forEach { choice ->
                    val choiceLabel = choiceLabel(choice)
                    val accessibleChoice = if (choices.orEmpty().count { choiceLabel(it) == choiceLabel } > 1)
                        "$choiceLabel (${choice.value.content})" else choiceLabel
                    DropdownMenuItem(text = { Text(choiceLabel) },
                        modifier = Modifier.semantics { contentDescription = accessibleChoice }, onClick = {
                        props.dispatch(UpdateAction(state.path, updater = update@{ current ->
                            if (!canAdd || !state.enabled || state.readonly) return@update current
                            if (chips) addChipValue(current, choice.value.content, choices,
                                state.schema.uniqueItems == true, state.schema.maxItems, restrict)
                            else addFiniteStringChoice(current, choice.value, choices.orEmpty(), state.schema.maxItems, restrict)
                        }))
                        expanded = false
                        query = ""
                        touched = true
                    })
                }
            }
        }
        visibleDescription?.let { Text(it) }
        visibleError.takeIf(String::isNotBlank)?.let { Text(it) }
    }
}
