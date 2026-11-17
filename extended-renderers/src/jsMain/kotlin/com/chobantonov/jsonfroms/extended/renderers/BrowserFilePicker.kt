package com.chobantonov.jsonfroms.extended.renderers

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

fun browserFilePicker(): ExtendedFilePicker = ExtendedFilePicker { schema, options, onSelected ->
    val input: dynamic = js("document.createElement('input')")
    input.type = "file"
    val accept = (options["accept"] as? JsonPrimitive)?.contentOrNull
        ?: (schema["contentMediaType"] as? JsonPrimitive)?.contentOrNull
    if (accept != null) input.accept = accept
    input.onchange = { _: dynamic ->
        val file: dynamic = input.files[0]
        if (file != null) {
            val reader: dynamic = js("new FileReader()")
            reader.onload = { _: dynamic ->
                val dataUrl = reader.result as String
                onSelected(encodedFileValue(
                    schema,
                    dataUrl.substringAfter(','),
                    file.type as String,
                    file.name as String,
                ))
            }
            reader.readAsDataURL(file)
        }
    }
    input.click()
}

fun browserFilePickerEvents(): ExtendedFilePickerEvents = object : ExtendedFilePickerEvents {
    override fun pick(schema: com.chobantonov.jsonfroms.core.models.JsonSchema,
                      options: com.chobantonov.jsonfroms.core.models.JsonOptions,
                      onEvent: (FilePickerEvent) -> Unit) = openBrowserPicker(schema, options, false, onEvent)

    override fun pickMultiple(schema: com.chobantonov.jsonfroms.core.models.JsonSchema,
                              options: com.chobantonov.jsonfroms.core.models.JsonOptions,
                              onEvent: (FilePickerEvent) -> Unit) = openBrowserPicker(schema, options, true, onEvent)
}

private fun openBrowserPicker(
    schema: com.chobantonov.jsonfroms.core.models.JsonSchema,
    options: com.chobantonov.jsonfroms.core.models.JsonOptions,
    multiple: Boolean,
    onEvent: (FilePickerEvent) -> Unit,
) {
    val input: dynamic = js("document.createElement('input')")
    input.type = "file"
    input.multiple = multiple
    val accept = (options["accept"] as? JsonPrimitive)?.contentOrNull
        ?: (schema["contentMediaType"] as? JsonPrimitive)?.contentOrNull
    if (accept != null) input.accept = accept
    var completed = false
    fun emit(event: FilePickerEvent) {
        if (!completed) {
            completed = true
            input.value = ""
            onEvent(event)
        }
    }
    input.oncancel = { _: dynamic -> emit(FilePickerEvent.Cancelled) }
    input.onchange = { _: dynamic ->
        val count = input.files.length as Int
        if (count == 0) emit(FilePickerEvent.Cancelled)
        else {
            val outcomes = mutableListOf<FilePickerEvent>()
            fun readAt(index: Int) {
                if (completed) return
                if (index >= count) {
                    emit(if (multiple) batchFileEvent(outcomes) else outcomes.first())
                    return
                }
                val file: dynamic = input.files[index]
                val size = (file.size as Double).toLong()
                val violation = fileSizeViolation(size, options)
                if (violation != null && fileRestrictionEnabled(options)) {
                    outcomes += FilePickerEvent.RejectedSize(violation)
                    readAt(index + 1)
                } else {
                    val reader: dynamic = js("new FileReader()")
                    reader.onload = { _: dynamic ->
                        val dataUrl = reader.result as String
                        outcomes += FilePickerEvent.Selected(
                            encodedFileValue(schema, dataUrl.substringAfter(','), file.type as String, file.name as String),
                            size,
                        )
                        readAt(index + 1)
                    }
                    reader.onerror = { _: dynamic -> emit(FilePickerEvent.ReadFailed()) }
                    runCatching { reader.readAsDataURL(file) }.onFailure { emit(FilePickerEvent.ReadFailed(it.message)) }
                }
            }
            readAt(0)
        }
    }
    input.click()
}
