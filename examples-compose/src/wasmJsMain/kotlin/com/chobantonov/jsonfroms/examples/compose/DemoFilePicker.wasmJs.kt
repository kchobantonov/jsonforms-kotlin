package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.extended.renderers.ExtendedFilePickerEvents
import com.chobantonov.jsonfroms.extended.renderers.FilePickerEvent
import com.chobantonov.jsonfroms.extended.renderers.batchFileEvent
import com.chobantonov.jsonfroms.extended.renderers.encodedFileValue
import com.chobantonov.jsonfroms.extended.renderers.fileRestrictionEnabled
import com.chobantonov.jsonfroms.extended.renderers.fileSizeViolation
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Composable
internal actual fun createDemoFilePicker(): ExtendedFilePickerEvents? = object : ExtendedFilePickerEvents {
    override fun pick(schema: JsonSchema, options: JsonOptions, onEvent: (FilePickerEvent) -> Unit) {
        pickBrowserFiles(schema, options, false, onEvent)
    }

    override fun pickMultiple(schema: JsonSchema, options: JsonOptions, onEvent: (FilePickerEvent) -> Unit) {
        pickBrowserFiles(schema, options, true, onEvent)
    }
}

private fun pickBrowserFiles(
    schema: JsonSchema,
    options: JsonOptions,
    multiple: Boolean,
    onEvent: (FilePickerEvent) -> Unit,
) {
    val accept = (options["accept"] as? JsonPrimitive)?.contentOrNull
        ?: (schema["contentMediaType"] as? JsonPrimitive)?.contentOrNull
        ?: ""
    val outcomes = mutableListOf<FilePickerEvent>()
    openBrowserFile(
        accept,
        multiple,
        sizeBound(options, "formatMinimum"),
        sizeBound(options, "formatMaximum"),
        sizeBound(options, "formatExclusiveMinimum"),
        sizeBound(options, "formatExclusiveMaximum"),
        fileRestrictionEnabled(options),
    ) { status, dataUrl, name, mime, originalBytes ->
        when (status) {
            0 -> outcomes += FilePickerEvent.Selected(
                encodedFileValue(schema, dataUrl.substringAfter(','), mime, name),
                originalBytes.toLong(),
            )
            1 -> {
                val violation = fileSizeViolation(originalBytes.toLong(), options)
                outcomes += if (violation != null) FilePickerEvent.RejectedSize(violation)
                    else FilePickerEvent.ReadFailed()
            }
            2 -> onEvent(FilePickerEvent.ReadFailed())
            3 -> onEvent(FilePickerEvent.Cancelled)
            4 -> onEvent(if (multiple) batchFileEvent(outcomes) else outcomes.firstOrNull() ?: FilePickerEvent.Cancelled)
        }
    }
}

private fun sizeBound(options: JsonOptions, name: String): Double =
    (options[name] as? JsonPrimitive)?.content?.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0.0 } ?: Double.NaN

@OptIn(ExperimentalWasmJsInterop::class)
private fun openBrowserFile(
    accept: String,
    multiple: Boolean,
    minimum: Double,
    maximum: Double,
    exclusiveMinimum: Double,
    exclusiveMaximum: Double,
    restrict: Boolean,
    callback: (Int, String, String, String, Double) -> Unit,
): Unit = js(
    """{
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = accept;
        input.multiple = multiple;
        let finished = false;
        const emit = (status, data = '', name = '', mime = '', size = -1) => {
            if (finished) return;
            if (status === 2 || status === 3 || status === 4) {
                finished = true;
                input.value = '';
            }
            callback(status, data, name, mime, size);
        };
        input.oncancel = () => emit(3);
        input.onchange = () => {
            const files = input.files;
            if (!files || files.length === 0) {
                emit(3);
                return;
            }
            const readAt = index => {
                if (finished) return;
                if (index >= files.length) {
                    emit(4);
                    return;
                }
                const file = files[index];
                const size = file.size;
                const invalid = (Number.isFinite(minimum) && size < minimum) ||
                    (Number.isFinite(maximum) && size > maximum) ||
                    (Number.isFinite(exclusiveMinimum) && size <= exclusiveMinimum) ||
                    (Number.isFinite(exclusiveMaximum) && size >= exclusiveMaximum);
                if (restrict && invalid) {
                    emit(1, '', file.name, file.type, size);
                    readAt(index + 1);
                    return;
                }
                const reader = new FileReader();
                reader.onload = () => {
                    emit(0, String(reader.result), file.name, file.type, size);
                    readAt(index + 1);
                };
                reader.onerror = () => emit(2);
                try {
                    reader.readAsDataURL(file);
                } catch (_) {
                    emit(2);
                }
            };
            readAt(0);
        };
        input.click();
    }"""
)
