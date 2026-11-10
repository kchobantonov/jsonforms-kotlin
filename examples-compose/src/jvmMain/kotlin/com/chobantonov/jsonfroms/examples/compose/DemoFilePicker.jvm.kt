package com.chobantonov.jsonfroms.examples.compose

import com.chobantonov.jsonfroms.extended.renderers.ExtendedFilePicker
import java.util.Base64
import javax.swing.JFileChooser
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal actual fun createDemoFilePicker(): ExtendedFilePicker? = ExtendedFilePicker { schema, _, onSelected ->
    val chooser = JFileChooser()
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        val file = chooser.selectedFile
        val encoded = Base64.getEncoder().encodeToString(file.readBytes())
        val encoding = (schema["contentEncoding"] as? JsonPrimitive)?.contentOrNull
        val value = if (encoding == "base64" && schema.format != "binary" && schema.format != "uri") {
            encoded
        } else {
            "data:application/octet-stream;filename=${file.name};base64,$encoded"
        }
        onSelected(JsonPrimitive(value))
    }
}
