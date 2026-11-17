package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.extended.renderers.ExtendedFilePickerEvents
import com.chobantonov.jsonfroms.extended.renderers.FilePickerEvent
import com.chobantonov.jsonfroms.extended.renderers.batchFileEvent
import com.chobantonov.jsonfroms.extended.renderers.fileRestrictionEnabled
import com.chobantonov.jsonfroms.extended.renderers.fileSizeViolation
import com.chobantonov.jsonfroms.extended.renderers.selectedFileEvent
import java.io.File
import java.nio.file.Files
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal actual fun createDemoFilePicker(): ExtendedFilePickerEvents? {
    val scope = rememberCoroutineScope()
    return remember(scope) {
        object : ExtendedFilePickerEvents {
            override fun pick(schema: JsonSchema, options: JsonOptions, onEvent: (FilePickerEvent) -> Unit) {
                chooseFiles(schema, options, false, onEvent)
            }

            override fun pickMultiple(schema: JsonSchema, options: JsonOptions, onEvent: (FilePickerEvent) -> Unit) {
                chooseFiles(schema, options, true, onEvent)
            }

            private fun chooseFiles(
                schema: JsonSchema,
                options: JsonOptions,
                multiple: Boolean,
                onEvent: (FilePickerEvent) -> Unit,
            ) {
                SwingUtilities.invokeLater {
                    val chooser = JFileChooser().apply { isMultiSelectionEnabled = multiple }
                    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                        onEvent(FilePickerEvent.Cancelled)
                    } else {
                        val files = if (multiple) chooser.selectedFiles.toList() else listOf(chooser.selectedFile)
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                val outcomes = mutableListOf<FilePickerEvent>()
                                for (file in files) {
                                    val outcome = readPickedDesktopFile(file, schema, options)
                                    outcomes += outcome
                                    if (outcome is FilePickerEvent.ReadFailed) break
                                }
                                if (multiple) batchFileEvent(outcomes) else outcomes.firstOrNull() ?: FilePickerEvent.Cancelled
                            }
                            onEvent(result)
                        }
                    }
                }
            }
        }
    }
}

private fun readPickedDesktopFile(file: File, schema: JsonSchema, options: JsonOptions): FilePickerEvent =
    runCatching {
        val size = file.length()
        val violation = fileSizeViolation(size, options)
        if (violation != null && fileRestrictionEnabled(options)) {
            FilePickerEvent.RejectedSize(violation)
        } else {
            selectedFileEvent(schema, options, file.readBytes(), Files.probeContentType(file.toPath()), file.name)
        }
    }.getOrElse { FilePickerEvent.ReadFailed(it.message) }
