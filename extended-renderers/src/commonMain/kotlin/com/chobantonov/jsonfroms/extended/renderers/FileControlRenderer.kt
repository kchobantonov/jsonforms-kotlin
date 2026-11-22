package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.compose.rememberInitialControlFocusRequester
import com.chobantonov.jsonfroms.compose.strictControlBooleanOption
import com.chobantonov.jsonfroms.compose.useJsonForms
import com.chobantonov.jsonfroms.core.actions.Actions
import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.i18n.translateControlRequired
import com.chobantonov.jsonfroms.core.i18n.translateControlInvalidValue
import com.chobantonov.jsonfroms.core.i18n.defaultErrorTranslator
import com.chobantonov.jsonfroms.core.i18n.defaultTranslator
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.ContainsRemovalGuard
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.mapStateToControlProps
import com.chobantonov.jsonfroms.core.mappers.invalidStringStoredValue
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.schemaMatches
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.util.JsonPath
import com.chobantonov.jsonfroms.core.util.JsonPointer
import com.chobantonov.jsonfroms.core.util.Id
import com.chobantonov.jsonfroms.core.util.resolveSchema
import com.chobantonov.jsonfroms.core.util.resolveRestrict
import com.chobantonov.jsonfroms.core.util.resolveArrayActionPolicy
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.coroutines.launch

const val ExtendedFilePickerConfigKey = "extendedFilePicker"

private enum class FileOperationWarning { Duplicate, CountLimit, FileSize, ReadFailed }
private data class ActiveFileRequest(val id: Long, val snapshot: JsonElement?)

private val isFileSchema: (JsonSchema) -> Boolean = { schema ->
    schema.typeValue == JsonSchemaTypeValue.Single(JsonSchemaType.String) && (
        (schema["contentEncoding"] as? JsonPrimitive)?.contentOrNull == "base64" ||
            schema.format == "binary" || schema.format == "byte"
        )
}

private val isFileArraySchema: (JsonSchema, JsonSchema) -> Boolean = { schema, root ->
    schema.typeValue == JsonSchemaTypeValue.Single(JsonSchemaType.Array) &&
        (schema["items"] as? JsonObject)?.let { item ->
            resolveSchema(JsonSchema(item), null, root)?.let(isFileSchema)
        } == true
}

private val scalarFileTester = rankWith(2, and(uiTypeIs("Control"), schemaMatches(isFileSchema)))
private val arrayFileTester = rankWith(7, and(uiTypeIs("Control"), schemaMatches(isFileArraySchema)))

val fileControlTester: RankedTester = { ui, schema, context ->
    maxOf(scalarFileTester(ui, schema, context), arrayFileTester(ui, schema, context))
}

fun interface ExtendedFilePicker {
    fun pick(schema: JsonSchema, options: JsonOptions, onSelected: (JsonElement) -> Unit)
}

private fun fileBooleanOption(control: ControlElement, config: Map<String, Any?>, name: String, default: Boolean): Boolean =
    (control.options[name] as? JsonPrimitive)?.booleanOrNull
        ?: when (val value = config[name]) {
            is Boolean -> value
            is JsonPrimitive -> value.booleanOrNull
            else -> null
        }
        ?: default

/** File-size publication is opt-in; an authored element option overrides form-wide config. */
internal fun filePropagateErrors(control: ControlElement, config: Map<String, Any?>): Boolean {
    control.options["propagateErrors"]?.let {
        return it is JsonPrimitive && !it.isString && it.booleanOrNull == true
    }
    val extended = config["jsonformsExtended"]
    val configured = when (extended) {
        is JsonObject -> extended["propagateErrors"]
        is Map<*, *> -> extended["propagateErrors"]
        else -> null
    }
    return when (configured) {
        is Boolean -> configured
        is JsonPrimitive -> !configured.isString && configured.booleanOrNull == true
        else -> false
    }
}

@Composable
internal fun FileControl(
    props: ComposeJsonFormsRendererProps,
    textRenderer: ComposeJsonFormsRenderer,
    cupertino: Boolean = false,
    compact: Boolean = false,
) {
    val control = props.uischema as? ControlElement ?: return
    val stateProps = mapStateToControlProps(
        props.state,
        OwnPropsOfControl(
            id = props.id,
            controlUiSchema = control,
            own = OwnPropsOfRenderer(
                uischema = control,
                schema = props.schema,
                enabled = props.enabled,
                path = props.path,
                renderers = props.renderers,
                cells = props.cells,
            ),
        ),
    )
    if (!stateProps.visible) return
    val picker = props.config[ExtendedFilePickerConfigKey] as? ExtendedFilePicker
    val eventPicker = props.config[ExtendedFilePickerEventsConfigKey] as? ExtendedFilePickerEvents
    val hasPicker = picker != null || eventPicker != null
    val editable = stateProps.enabled && !stateProps.readonly
    val schema = stateProps.schema
    val path = stateProps.path
    val multiple = schema.typeValue == com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue.Single(
        com.chobantonov.jsonfroms.core.models.JsonSchemaType.Array,
    )
    if (multiple || compact || hasPicker) {
        com.chobantonov.jsonfroms.compose.ReportUnsupportedInputAdornments(props, "file")
    }
    val pickerSchema = if (multiple) {
        (schema["items"] as? JsonObject)?.let { resolveSchema(JsonSchema(it), null, props.rootSchema) } ?: schema
    } else schema
    val current = JsonPath.get(props.state.jsonforms.core.data, path)
    val attachments = (current as? JsonArray).orEmpty()
    val invalidStored = if (multiple) stateProps.data.takeIf {
        stateProps.dataProvided && it !is JsonArray
    } else invalidStringStoredValue(stateProps.data, stateProps.dataProvided)
    val invalidStatus = invalidStored?.let {
        "${translateControlInvalidValue(props.state.jsonforms.i18n)}: $it"
    }
    val restrict = resolveRestrict(control.options["restrict"], stateProps.config)
    val removalGuard = remember(schema, props.state.jsonforms.core.validator,
        props.state.jsonforms.core.data, path, attachments) {
        ContainsRemovalGuard(schema, props.state.jsonforms.core.validator,
            props.state.jsonforms.core.data, path, JsonArray(attachments))
    }
    fun canRemoveAt(index: Int) = !restrict || removalGuard.canRemove(index)
    fun canClearAll() = !restrict || removalGuard.canReplaceWith(JsonArray(emptyList()))
    val actionPolicy = resolveArrayActionPolicy(control.options, stateProps.config)
    val clearable = fileBooleanOption(control, stateProps.config, "clearable", true)
    val pickerOptions = props.config.filterValues { it is JsonElement }.mapValues { it.value as JsonElement } +
        control.options + ("restrict" to JsonPrimitive(restrict))
    val propagateErrors = filePropagateErrors(control, stateProps.config)
    val canAdd = editable && (!multiple || !actionPolicy.disableAdd) &&
        (!multiple || !restrict || schema.maxItems?.let { attachments.size < it } != false)
    val canRemove = editable && clearable && (!multiple || !actionPolicy.disableRemove) &&
        (!restrict || schema.minItems?.let { attachments.size > it } != false)
    val canClear = editable && clearable && (attachments.isNotEmpty() || (multiple && invalidStored != null)) &&
        (!multiple || !actionPolicy.disableRemove) &&
        (!restrict || schema.minItems?.let { it == 0 } != false) && canClearAll()
    val noncompactArray = multiple && !compact
    val ordinaryPickerScalar = !multiple && !compact && hasPicker
    val managedPresentation = noncompactArray || ordinaryPickerScalar
    val appliedOptions = stateProps.config + control.options
    val showUnfocusedDescription = strictControlBooleanOption(
        appliedOptions, "showUnfocusedDescription") == true
    val hideRequiredAsterisk = strictControlBooleanOption(
        appliedOptions, "hideRequiredAsterisk") == true
    val latestCanAdd = rememberUpdatedState(canAdd)
    val translator = props.state.jsonforms.i18n.translate
    fun label(key: String, fallback: String, bundleKey: String = "file.$key"): String = translateRendererText(
        translator, props.state.jsonforms.i18n.locale, "file.$key", fallback,
        fileControlLocaleBundles, i18nKeyPrefix = stateProps.i18nKeyPrefix, bundleKey = bundleKey,
    )
    val formContext = useJsonForms()
    val controller = formContext.controller
    val hostSubjectVersion = controller.hostSubjectVersion
    val ownerId = remember(props.id, path, pickerOptions) { Id.createId("file-size:${props.id}:$path") }
    val pointer = if (path.isBlank()) "" else path.split('.').joinToString("/", prefix = "/") { JsonPointer.encode(it) }
    var knownSizeViolations by remember(control.scope, pickerOptions, hostSubjectVersion) {
        mutableStateOf<Map<JsonElement, FileSizeViolation>>(emptyMap())
    }
    var activeRequest by remember(path, hostSubjectVersion) { mutableStateOf<ActiveFileRequest?>(null) }
    var nextRequestId by remember(path) { mutableStateOf(0L) }
    var mounted by remember(path) { mutableStateOf(true) }
    fun publishKnownSizeErrors() {
        val stored = JsonPath.get(controller.state.jsonforms.core.data, path)
        val values = if (multiple) (stored as? JsonArray).orEmpty() else listOfNotNull(stored)
        val violations = values.mapNotNull { knownSizeViolations[it] }
        val first = violations.firstOrNull()
        val errors = if (first == null || !propagateErrors) emptyList() else listOf(ErrorObject(
            instancePath = pointer,
            keyword = "fileSize",
            message = translateRendererText(
                translator, props.state.jsonforms.i18n.locale, "file.sizeInvalid",
                if (violations.size == 1) "Selected file violates the size limit"
                else "${violations.size} selected files violate size limits",
                if (violations.size == 1) fileControlLocaleBundles
                else fileCountLocaleBundles(violations.size),
                mapOf("count" to violations.size, "actualBytes" to first.actualBytes,
                    "limitBytes" to first.limitBytes, "option" to first.option),
                i18nKeyPrefix = stateProps.i18nKeyPrefix,
            ),
            params = mapOf("count" to violations.size.toString(), "actualBytes" to first.actualBytes.toString(),
                "limitBytes" to first.limitBytes.toString(), "option" to first.option),
        ))
        controller.publishAdditionalErrors(ownerId, pointer, errors)
    }
    DisposableEffect(controller, ownerId) {
        mounted = true
        onDispose {
            mounted = false
            activeRequest = null
            controller.clearAdditionalErrors(ownerId)
        }
    }
    DisposableEffect(ownerId) {
        onDispose { Id.removeId(ownerId) }
    }
    LaunchedEffect(ownerId, current, knownSizeViolations, propagateErrors,
        props.state.jsonforms.i18n.locale, translator, activeRequest) {
        if (activeRequest == null) publishKnownSizeErrors()
    }
    LaunchedEffect(current) {
        if (activeRequest?.snapshot != current && activeRequest != null) {
            activeRequest = null
            publishKnownSizeErrors()
        }
    }
    LaunchedEffect(canAdd) {
        if (!canAdd && activeRequest != null) {
            activeRequest = null
            publishKnownSizeErrors()
        }
    }
    var manualValue by remember(path) { mutableStateOf("") }
    var operationWarning by remember(path) { mutableStateOf<FileOperationWarning?>(null) }
    var preserveWarningFor by remember(path) { mutableStateOf<JsonElement?>(null) }
    LaunchedEffect(current) {
        if (current != preserveWarningFor) operationWarning = null
        preserveWarningFor = null
    }
    var hasReceivedFocus by remember(path) { mutableStateOf(false) }
    var touched by remember(path) { mutableStateOf(false) }
    var focused by remember(path) { mutableStateOf(false) }
    val focusModifier = Modifier.onFocusChanged { focus ->
        focused = focus.isFocused
        if (focus.isFocused) hasReceivedFocus = true
        else if (hasReceivedFocus) touched = true
    }
    val error = if (touched) stateProps.errors else stateProps.errorsBeforeTouch
    val visibleDescription = stateProps.description?.takeIf {
        managedPresentation && it.isNotBlank() && (focused || showUnfocusedDescription)
    }
    val requiredState = translateControlRequired(props.state.jsonforms.i18n)
        .takeIf { managedPresentation && stateProps.required }
    val accessibleState = listOfNotNull(requiredState, invalidStatus, visibleDescription).joinToString(". ")
    val accessibleError = listOfNotNull(invalidStatus, error.takeIf { it.isNotBlank() })
        .distinct().joinToString(". ")
    val primaryFocusRequester = rememberInitialControlFocusRequester(
        path, appliedOptions, managedPresentation && canAdd && activeRequest == null)
    val primaryModifier = if (managedPresentation) Modifier.focusRequester(primaryFocusRequester)
        .then(focusModifier).semantics {
            if (accessibleState.isNotBlank()) stateDescription = accessibleState
            if (accessibleError.isNotBlank()) error(accessibleError)
        } else focusModifier
    val compactErrors = if (compact) {
        formContext.containerErrorIndex.errorsAtOrBelow(path).distinct()
    } else emptyList()
    val errorTranslator = props.state.jsonforms.i18n.translateError ?: defaultErrorTranslator
    val resolvedTranslator = translator ?: defaultTranslator
    val compactMessages = compactErrors.map { errorTranslator.translate(it, resolvedTranslator, control) }
        .ifEmpty { error.takeIf(String::isNotBlank)?.let(::listOf).orEmpty() }
    val compactSummary = compactMessages.size.takeIf { it > 1 }?.let { count ->
        translateRendererText(translator, props.state.jsonforms.i18n.locale, "file.errorCount",
            "$count errors", fileCountLocaleBundles(count), mapOf("count" to count),
            i18nKeyPrefix = stateProps.i18nKeyPrefix)
    }
    val compactError = (listOfNotNull(invalidStatus, compactSummary) + compactMessages)
        .distinct().joinToString("\n")
    val compactErrorLabel = invalidStatus ?: compactSummary ?: compactMessages.singleOrNull().orEmpty()
    fun compactInputModifier(base: Modifier, name: String): Modifier =
        if (compactError.isBlank()) base else base.semantics {
            contentDescription = "$name. $compactError"
            error(compactError)
        }
    val errorColor = if (cupertino) LocalCupertinoColors.current.destructive else MaterialTheme.colorScheme.error
    val warningColor = if (cupertino) LocalCupertinoColors.current.warning else MaterialTheme.colorScheme.tertiary
    val warningText = when (operationWarning) {
        FileOperationWarning.Duplicate -> label("duplicate", "This file is already attached")
        FileOperationWarning.CountLimit -> label("countLimit", "The attachment limit has been reached")
        FileOperationWarning.FileSize -> label("sizeRejected", "The selected file does not meet the size limit")
        FileOperationWarning.ReadFailed -> label("readFailed", "The selected file could not be read")
        null -> null
    }
    fun commitSelected(value: JsonElement, snapshot: JsonElement?): Boolean {
        if (!latestCanAdd.value) return false
        operationWarning = when {
            multiple && schema.uniqueItems == true && value in attachments -> FileOperationWarning.Duplicate
            multiple && restrict && schema.maxItems?.let { attachments.size >= it } == true -> FileOperationWarning.CountLimit
            else -> null
        }
        if (operationWarning != null) return false
        val expected = if (multiple) JsonArray((snapshot as? JsonArray).orEmpty() + value) else value
        props.dispatch(Actions.update(path = path, updater = { old ->
            if (old != snapshot) old
            else if (!multiple) value
            else {
                val values = (old as? JsonArray).orEmpty()
                if ((restrict && schema.maxItems?.let { values.size >= it } == true) ||
                    (schema.uniqueItems == true && value in values)
                ) old else JsonArray(values + value)
            }
        }))
        return JsonPath.get(controller.state.jsonforms.core.data, path) == expected
    }
    fun commitSelectedBatch(event: FilePickerEvent.SelectedBatch, snapshot: JsonElement?) {
        if (!latestCanAdd.value) return
        val initial = (snapshot as? JsonArray).orEmpty()
        val accepted = mutableListOf<FilePickerEvent.Selected>()
        val values = initial.toMutableList()
        var warning = if (event.rejectedSize != null) FileOperationWarning.FileSize else null
        for (file in event.files) {
            val violation = fileSizeViolation(file.originalBytes, pickerOptions)
            when {
                restrict && violation != null -> warning = FileOperationWarning.FileSize
                schema.uniqueItems == true && file.value in values -> warning = warning ?: FileOperationWarning.Duplicate
                restrict && schema.maxItems?.let { values.size >= it } == true ->
                    warning = warning ?: FileOperationWarning.CountLimit
                else -> {
                    values += file.value
                    accepted += file
                }
            }
        }
        operationWarning = warning
        if (accepted.isEmpty()) return
        val expected = JsonArray(values)
        if (warning != null) preserveWarningFor = expected
        props.dispatch(Actions.update(path = path, updater = { old ->
            if (old != snapshot) old else expected
        }))
        if (JsonPath.get(controller.state.jsonforms.core.data, path) == expected) {
            knownSizeViolations = knownSizeViolations + accepted.mapNotNull { file ->
                fileSizeViolation(file.originalBytes, pickerOptions)?.let { file.value to it }
            }
            if (!propagateErrors && accepted.any { fileSizeViolation(it.originalBytes, pickerOptions) != null }) {
                operationWarning = FileOperationWarning.FileSize
                preserveWarningFor = expected
            }
        }
    }
    fun attachmentName(value: JsonElement?, index: Int): String {
        return fileNameFromStoredValue(value) ?: "${label("attachment", "Attachment")} ${index + 1}"
    }
    fun removeAttachment(index: Int) {
        if (!canRemove || !canRemoveAt(index)) return
        operationWarning = null
        val snapshot = attachments
        props.dispatch(Actions.update(path = path, updater = { old ->
            val values = (old as? JsonArray).orEmpty()
            if (values != snapshot || index !in values.indices || !canRemove || !canRemoveAt(index) ||
                (restrict && schema.minItems?.let { values.size <= it } == true)
            ) old else JsonArray(values.filterIndexed { itemIndex, _ -> itemIndex != index })
        }))
        publishKnownSizeErrors()
    }
    fun clearAttachments() {
        if (!canClear || !canClearAll()) return
        operationWarning = null
        val snapshot = current
        props.dispatch(Actions.update(path = path, updater = { old ->
            if (old != snapshot || !canClear || !canClearAll() ||
                (restrict && schema.minItems?.let { it > 0 } == true)) old
            else JsonArray(emptyList())
        }))
        knownSizeViolations = emptyMap()
        publishKnownSizeErrors()
    }
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 8.dp)) {
        if (managedPresentation && stateProps.label.isNotBlank()) {
            Text(computeLabel(stateProps.label, stateProps.required, hideRequiredAsterisk))
        }
        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 240.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (multiple) {
                        if (attachments.isEmpty() && invalidStatus == null)
                            Text(label("empty", "No file"), maxLines = 1)
                        attachments.forEachIndexed { index, attachment ->
                            Row(
                                modifier = Modifier.border(1.dp, if (cupertino) LocalCupertinoColors.current.controlBorder
                                    else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                    .padding(start = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                FileNameText(attachmentName(attachment, index))
                                IconButton(
                                    enabled = canRemove && canRemoveAt(index),
                                    onClick = { removeAttachment(index) },
                                    modifier = Modifier.size(30.dp),
                                ) {
                                    Icon(JsonFormsActionIcons.Clear, contentDescription = label("remove", "Remove"),
                                        modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    } else {
                        if (current == null) Text(label("empty", "No file"), maxLines = 1)
                        else FileNameText(attachmentName(current, 0))
                    }
                }
                if (compactError.isNotBlank()) CompactFeedbackIcon(
                    compactError, errorColor, cupertino, compactErrorLabel)
            }
        } else if (multiple) {
            attachments.forEachIndexed { index, attachment ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(attachmentName(attachment, index), Modifier.weight(1f))
                    FileActionButton(cupertino = cupertino, enabled = canRemove && canRemoveAt(index),
                        onClick = { removeAttachment(index) }, text = label("remove", "Remove"))
                }
            }
            FileActionButton(cupertino = cupertino, enabled = canClear,
                onClick = ::clearAttachments, text = label("clearAll", "Clear all"))
        } else if (ordinaryPickerScalar) {
            if (current == null) Text(label("empty", "No file"))
            else FileNameText(attachmentName(current, 0))
        } else if (!compact) {
            textRenderer.Render(props)
        }
        if (hasPicker) {
            val compactPickerActionScroll = rememberScrollState()
            LaunchedEffect(warningText, compact) {
                if (compact && warningText != null) compactPickerActionScroll.scrollTo(compactPickerActionScroll.maxValue)
            }
            Row(
                modifier = if (compact) Modifier.widthIn(max = 240.dp).horizontalScroll(compactPickerActionScroll)
                    else Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
            val pickerAction = if (compact) {
                if (current == null || (multiple && attachments.isEmpty())) label("select", "Select")
                else if (multiple) label("add", "Add") else label("replace", "Replace")
            } else label("choose", "Choose file")
            FileActionButton(
                cupertino = cupertino,
                modifier = if (compact) compactInputModifier(primaryModifier, pickerAction) else primaryModifier,
                enabled = canAdd && activeRequest == null,
                onClick = {
                    if (!canAdd) return@FileActionButton
                    val snapshot = current
                    if (eventPicker != null) {
                        val request = ActiveFileRequest(++nextRequestId, snapshot)
                        val requestSubjectVersion = hostSubjectVersion
                        activeRequest = request
                        controller.beginAdditionalErrors(ownerId, pointer)
                        val receive: (FilePickerEvent) -> Unit = receive@{ event ->
                            if (!mounted || controller.hostSubjectVersion != requestSubjectVersion ||
                                activeRequest != request) return@receive
                            activeRequest = null
                            if (!latestCanAdd.value || JsonPath.get(controller.state.jsonforms.core.data, path) != snapshot) {
                                publishKnownSizeErrors()
                            } else {
                                when (event) {
                                    is FilePickerEvent.Selected -> {
                                        val violation = fileSizeViolation(event.originalBytes, pickerOptions)
                                        if (restrict && violation != null) {
                                            operationWarning = FileOperationWarning.FileSize
                                        } else if (commitSelected(event.value, snapshot)) {
                                            knownSizeViolations = if (multiple) {
                                                if (violation == null) knownSizeViolations
                                                else knownSizeViolations + (event.value to violation)
                                            } else {
                                                if (violation == null) emptyMap() else mapOf(event.value to violation)
                                            }
                                            if (violation != null && !propagateErrors) {
                                                operationWarning = FileOperationWarning.FileSize
                                                preserveWarningFor = event.value
                                            }
                                        }
                                    }
                                    is FilePickerEvent.SelectedBatch -> {
                                        if (multiple) commitSelectedBatch(event, snapshot)
                                        else event.files.firstOrNull()?.let { commitSelected(it.value, snapshot) }
                                    }
                                    is FilePickerEvent.RejectedSize -> operationWarning = FileOperationWarning.FileSize
                                    is FilePickerEvent.ReadFailed -> operationWarning = FileOperationWarning.ReadFailed
                                    FilePickerEvent.Cancelled -> {
                                        operationWarning = null
                                        if (editable) props.dispatch(Actions.update(path = path, updater = { old ->
                                            if (old != snapshot) old else if (multiple) JsonArray(emptyList()) else null
                                        }))
                                        knownSizeViolations = emptyMap()
                                    }
                                }
                                publishKnownSizeErrors()
                            }
                        }
                        if (multiple) eventPicker.pickMultiple(pickerSchema, pickerOptions, receive)
                        else eventPicker.pick(pickerSchema, pickerOptions, receive)
                    } else {
                        var completed = false
                        val requestSubjectVersion = hostSubjectVersion
                        picker?.pick(pickerSchema, pickerOptions) { value ->
                            if (!mounted || completed ||
                                controller.hostSubjectVersion != requestSubjectVersion) return@pick
                            completed = true
                            if (!latestCanAdd.value) return@pick
                            commitSelected(value, snapshot)
                            publishKnownSizeErrors()
                        }
                    }
                },
                text = pickerAction,
            )
            if (compact || ordinaryPickerScalar) {
                if (multiple) {
                    FileActionButton(cupertino = cupertino, enabled = canClear,
                        onClick = ::clearAttachments, text = label("clearAll", "Clear all"))
                } else {
                    FileActionButton(cupertino = cupertino, enabled = editable && clearable && current != null,
                        onClick = {
                            val snapshot = current
                            props.dispatch(Actions.update(path = path, updater = { old ->
                                if (old != snapshot) old else null
                            }))
                            knownSizeViolations = emptyMap()
                            publishKnownSizeErrors()
                        }, text = label("clear", "Clear"))
                }
                if (compact && warningText != null) CompactFeedbackIcon(
                    warningText, if (operationWarning == FileOperationWarning.ReadFailed) errorColor else warningColor,
                    cupertino,
                )
            }
            }
        } else if (multiple) {
            if (cupertino) {
                val colors = LocalCupertinoColors.current
                BasicTextField(
                    value = manualValue,
                    onValueChange = { manualValue = it },
                    enabled = canAdd,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.label),
                    modifier = compactInputModifier(
                        primaryModifier.fillMaxWidth().then(if (compact) Modifier.widthIn(max = 240.dp) else Modifier),
                        label("manualValue", "File data or URL"),
                    ).semantics {
                        if (compactError.isBlank()) contentDescription = label("manualValue", "File data or URL")
                    }
                        .border(1.dp, colors.controlBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    decorationBox = { inner ->
                        Box {
                            if (manualValue.isEmpty()) CupertinoText(label("manualValue", "File data or URL"))
                            inner()
                        }
                    },
                )
            } else {
                OutlinedTextField(
                    modifier = compactInputModifier(
                        primaryModifier.then(if (compact) Modifier.widthIn(max = 240.dp) else Modifier),
                        label("manualValue", "File data or URL"),
                    ),
                    value = manualValue,
                    onValueChange = { manualValue = it },
                    label = { Text(label("manualValue", "File data or URL")) },
                    enabled = canAdd,
                )
            }
            val compactActionScroll = rememberScrollState()
            LaunchedEffect(warningText, compact) {
                if (compact && warningText != null) compactActionScroll.scrollTo(compactActionScroll.maxValue)
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = if (compact) Modifier.widthIn(max = 240.dp).horizontalScroll(compactActionScroll)
                    else Modifier) {
            FileActionButton(cupertino = cupertino, enabled = canAdd && manualValue.isNotBlank(), onClick = {
                if (!canAdd) return@FileActionButton
                val value = JsonPrimitive(manualValue)
                operationWarning = when {
                    schema.uniqueItems == true && value in attachments -> FileOperationWarning.Duplicate
                    restrict && schema.maxItems?.let { attachments.size >= it } == true -> FileOperationWarning.CountLimit
                    else -> null
                }
                if (operationWarning != null) return@FileActionButton
                props.dispatch(Actions.update(path = path, updater = { old ->
                    val values = (old as? JsonArray).orEmpty()
                    if (!latestCanAdd.value || (restrict && schema.maxItems?.let { values.size >= it } == true) ||
                        (schema.uniqueItems == true && value in values)
                    ) old else JsonArray(values + value)
                }))
                manualValue = ""
                publishKnownSizeErrors()
            }, text = label("add", "Add attachment", bundleKey = "file.addAttachment"))
            if (compact) FileActionButton(cupertino = cupertino, enabled = canClear,
                onClick = ::clearAttachments, text = label("clearAll", "Clear all"))
            if (compact && warningText != null) CompactFeedbackIcon(
                warningText, if (operationWarning == FileOperationWarning.ReadFailed) errorColor else warningColor,
                cupertino,
            )
            }
        } else if (compact) {
            val value = (current as? JsonPrimitive)?.contentOrNull.orEmpty()
            val onManualChange: (String) -> Unit = { updated ->
                operationWarning = null
                knownSizeViolations = emptyMap()
                props.dispatch(Actions.update(path = path, updater = { JsonPrimitive(updated) }))
            }
            if (cupertino) {
                val colors = LocalCupertinoColors.current
                BasicTextField(
                    value = value,
                    onValueChange = onManualChange,
                    enabled = editable,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.label),
                    modifier = compactInputModifier(focusModifier.widthIn(max = 240.dp),
                        label("manualValue", "File data or URL")).semantics {
                        if (compactError.isBlank()) contentDescription = label("manualValue", "File data or URL")
                    }
                        .border(1.dp, colors.controlBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) CupertinoText(label("manualValue", "File data or URL"))
                            inner()
                        }
                    },
                )
            } else {
                OutlinedTextField(
                    modifier = compactInputModifier(focusModifier.widthIn(max = 240.dp),
                        label("manualValue", "File data or URL")),
                    value = value,
                    onValueChange = onManualChange,
                    label = { Text(label("manualValue", "File data or URL")) },
                    enabled = editable,
                    singleLine = true,
                )
            }
            FileActionButton(cupertino = cupertino, enabled = editable && clearable && current != null,
                onClick = {
                    val snapshot = current
                    props.dispatch(Actions.update(path = path, updater = { old -> if (old == snapshot) null else old }))
                    knownSizeViolations = emptyMap()
                    publishKnownSizeErrors()
                }, text = label("clear", "Clear"))
        }
        if (invalidStatus != null && (managedPresentation || compact))
            Text(invalidStatus, color = errorColor, style = MaterialTheme.typography.bodySmall)
        if (visibleDescription != null) Text(visibleDescription)
        if (managedPresentation && error.isNotBlank()) {
            Text(error, color = errorColor, style = MaterialTheme.typography.bodySmall)
        }
        if (!compact && warningText != null) Text(
            warningText,
            color = if (operationWarning == FileOperationWarning.ReadFailed) errorColor else warningColor,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileNameText(name: String) {
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(name) } },
        state = tooltipState,
    ) {
        Text(name, Modifier.widthIn(max = 108.dp)
            .onFocusChanged { if (it.isFocused) scope.launch { tooltipState.show() } }
            .focusable().semantics { contentDescription = name },
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCupertinoApi::class)
@Composable
internal fun CompactFeedbackIcon(
    message: String,
    color: Color,
    cupertino: Boolean,
    description: String = message,
) {
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(message) } },
        state = tooltipState,
    ) {
        val modifier = Modifier.onFocusChanged { if (it.isFocused) scope.launch { tooltipState.show() } }
            .size(32.dp).semantics { contentDescription = description }
        if (cupertino) {
            CupertinoButton(
                modifier = modifier,
                onClick = { scope.launch { tooltipState.show() } },
                colors = CupertinoButtonDefaults.plainButtonColors(),
            ) { Text("!", color = color) }
        } else {
            IconButton(onClick = { scope.launch { tooltipState.show() } }, modifier = modifier) {
                Text("!", color = color)
            }
        }
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun FileActionButton(
    cupertino: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    if (cupertino) {
        CupertinoButton(
            modifier = modifier,
            enabled = enabled,
            onClick = onClick,
            colors = CupertinoButtonDefaults.plainButtonColors(),
        ) { CupertinoText(text) }
    } else {
        OutlinedButton(modifier = modifier, enabled = enabled, onClick = onClick) { Text(text) }
    }
}
