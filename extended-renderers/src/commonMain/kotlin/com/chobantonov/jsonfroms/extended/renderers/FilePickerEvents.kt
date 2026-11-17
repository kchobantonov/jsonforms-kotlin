package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlin.io.encoding.Base64

const val ExtendedFilePickerEventsConfigKey = "extendedFilePickerEvents"

fun interface ExtendedFilePickerEvents {
    fun pick(schema: JsonSchema, options: JsonOptions, onEvent: (FilePickerEvent) -> Unit)

    fun pickMultiple(schema: JsonSchema, options: JsonOptions, onEvent: (FilePickerEvent) -> Unit) =
        pick(schema, options) { event ->
            onEvent(if (event is FilePickerEvent.Selected) FilePickerEvent.SelectedBatch(listOf(event)) else event)
        }
}

sealed interface FilePickerEvent {
    data class Selected(val value: JsonElement, val originalBytes: Long) : FilePickerEvent
    data class SelectedBatch(
        val files: List<Selected>,
        val rejectedSize: FileSizeViolation? = null,
    ) : FilePickerEvent
    data class RejectedSize(val violation: FileSizeViolation) : FilePickerEvent
    data class ReadFailed(val message: String? = null) : FilePickerEvent
    data object Cancelled : FilePickerEvent
}

data class FileSizeViolation(val option: String, val limitBytes: Double, val actualBytes: Long)

/** A failed read aborts the whole batch; restricted size rejections leave accepted files in chooser order. */
fun batchFileEvent(results: List<FilePickerEvent>): FilePickerEvent {
    results.filterIsInstance<FilePickerEvent.ReadFailed>().firstOrNull()?.let { return it }
    val selected = results.filterIsInstance<FilePickerEvent.Selected>()
    val rejected = results.filterIsInstance<FilePickerEvent.RejectedSize>().firstOrNull()?.violation
    return when {
        selected.isNotEmpty() -> FilePickerEvent.SelectedBatch(selected, rejected)
        rejected != null -> FilePickerEvent.RejectedSize(rejected)
        else -> FilePickerEvent.Cancelled
    }
}

/** Uses the bytes actually read when picker size metadata is missing or has changed. */
fun selectedFileEvent(
    schema: JsonSchema,
    options: JsonOptions,
    bytes: ByteArray,
    mimeType: String?,
    filename: String,
): FilePickerEvent {
    val size = bytes.size.toLong()
    val violation = fileSizeViolation(size, options)
    if (violation != null && fileRestrictionEnabled(options)) return FilePickerEvent.RejectedSize(violation)
    return FilePickerEvent.Selected(encodedFileValue(schema, Base64.encode(bytes), mimeType, filename), size)
}

fun fileSizeViolation(originalBytes: Long, options: JsonOptions): FileSizeViolation? {
    if (originalBytes < 0) return null
    val actual = originalBytes.toDouble()
    val bounds = listOf(
        "formatMinimum" to { limit: Double -> actual < limit },
        "formatMaximum" to { limit: Double -> actual > limit },
        "formatExclusiveMinimum" to { limit: Double -> actual <= limit },
        "formatExclusiveMaximum" to { limit: Double -> actual >= limit },
    )
    return bounds.firstNotNullOfOrNull { (name, violates) ->
        val limit = (options[name] as? JsonPrimitive)?.content?.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it >= 0.0 }
        if (limit != null && violates(limit)) FileSizeViolation(name, limit, originalBytes) else null
    }
}

fun fileRestrictionEnabled(options: JsonOptions): Boolean =
    (options["restrict"] as? JsonPrimitive)?.booleanOrNull == true
