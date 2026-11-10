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
                val encoding = (schema["contentEncoding"] as? JsonPrimitive)?.contentOrNull
                val value = if (encoding == "base64" && schema.format != "binary" && schema.format != "uri") {
                    dataUrl.substringAfter(',')
                } else {
                    val encodedName = js("encodeURIComponent(file.name)") as String
                    dataUrl.replaceFirst(";base64,", ";filename=$encodedName;base64,")
                }
                onSelected(JsonPrimitive(value))
            }
            reader.readAsDataURL(file)
        }
    }
    input.click()
}
