package com.chobantonov.jsonfroms.examples.compose

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.extended.renderers.ExtendedFilePickerEvents
import com.chobantonov.jsonfroms.extended.renderers.FilePickerEvent
import com.chobantonov.jsonfroms.extended.renderers.batchFileEvent
import com.chobantonov.jsonfroms.extended.renderers.selectedFileEvent
import com.chobantonov.jsonfroms.extended.renderers.fileRestrictionEnabled
import com.chobantonov.jsonfroms.extended.renderers.fileSizeViolation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private data class PendingDemoFile(
    val schema: JsonSchema,
    val options: JsonOptions,
    val onEvent: (FilePickerEvent) -> Unit,
)

@Composable
internal actual fun createDemoFilePicker(): ExtendedFilePickerEvents? {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<PendingDemoFile?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val request = pending
        pending = null
        if (request != null) {
            if (uri == null) request.onEvent(FilePickerEvent.Cancelled)
            else scope.launch {
                val result = withContext(Dispatchers.IO) {
                    readPickedAndroidFile(context, uri, request.schema, request.options)
                }
                request.onEvent(result)
            }
        }
    }
    val multiLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val request = pending
        pending = null
        if (request != null) {
            if (uris.isEmpty()) request.onEvent(FilePickerEvent.Cancelled)
            else scope.launch {
                val result = withContext(Dispatchers.IO) {
                    val outcomes = mutableListOf<FilePickerEvent>()
                    for (uri in uris) {
                        val outcome = readPickedAndroidFile(context, uri, request.schema, request.options)
                        outcomes += outcome
                        if (outcome is FilePickerEvent.ReadFailed) break
                    }
                    batchFileEvent(outcomes)
                }
                request.onEvent(result)
            }
        }
    }
    return remember(context, launcher, multiLauncher) {
        object : ExtendedFilePickerEvents {
            override fun pick(schema: JsonSchema, options: JsonOptions, onEvent: (FilePickerEvent) -> Unit) {
                pending = PendingDemoFile(schema, options, onEvent)
                launcher.launch(demoMimeTypes(schema, options))
            }

            override fun pickMultiple(schema: JsonSchema, options: JsonOptions, onEvent: (FilePickerEvent) -> Unit) {
                pending = PendingDemoFile(schema, options, onEvent)
                multiLauncher.launch(demoMimeTypes(schema, options))
            }
        }
    }
}

private fun readPickedAndroidFile(context: Context, uri: Uri, schema: JsonSchema, options: JsonOptions): FilePickerEvent =
    runCatching {
        val resolver = context.contentResolver
        val metadata = resolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) null else {
                val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                val name = if (nameColumn >= 0) cursor.getString(nameColumn) else null
                val size = if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) cursor.getLong(sizeColumn) else -1L
                name to size
            }
        }
        val name = metadata?.first ?: uri.lastPathSegment ?: "attachment"
        val size = metadata?.second ?: -1L
        val violation = fileSizeViolation(size, options)
        if (violation != null && fileRestrictionEnabled(options)) {
            FilePickerEvent.RejectedSize(violation)
        } else {
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Could not open selected file")
            selectedFileEvent(schema, options, bytes, resolver.getType(uri), name)
        }
    }.getOrElse { FilePickerEvent.ReadFailed(it.message) }

private fun demoMimeTypes(schema: JsonSchema, options: JsonOptions): Array<String> {
    val accept = (options["accept"] as? JsonPrimitive)?.contentOrNull
        ?: (schema["contentMediaType"] as? JsonPrimitive)?.contentOrNull
    val types = accept?.split(',')?.mapNotNull { entry ->
        val token = entry.trim()
        when {
            token.contains('/') -> token
            token.startsWith('.') -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(token.drop(1).lowercase())
            else -> null
        }
    }?.distinct().orEmpty()
    return if (types.isEmpty()) arrayOf("*/*") else types.toTypedArray()
}
