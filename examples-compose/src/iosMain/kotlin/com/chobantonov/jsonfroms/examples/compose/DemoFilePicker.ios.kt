package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.extended.renderers.ExtendedFilePickerEvents
import com.chobantonov.jsonfroms.extended.renderers.FilePickerEvent
import com.chobantonov.jsonfroms.extended.renderers.batchFileEvent
import com.chobantonov.jsonfroms.extended.renderers.selectedFileEvent
import com.chobantonov.jsonfroms.extended.renderers.fileRestrictionEnabled
import com.chobantonov.jsonfroms.extended.renderers.fileSizeViolation
import kotlinx.coroutines.Dispatchers
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.darwin.NSObject
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UIKit.UIDocumentPickerViewController

private class DemoDocumentPickerDelegate(
    private val selected: (List<NSURL>) -> Unit,
    private val cancelled: () -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val urls = didPickDocumentsAtURLs.filterIsInstance<NSURL>()
        if (urls.isEmpty()) cancelled() else selected(urls)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        cancelled()
    }
}

@Composable
internal actual fun createDemoFilePicker(): ExtendedFilePickerEvents? {
    val scope = rememberCoroutineScope()
    var activeDelegate by remember { mutableStateOf<DemoDocumentPickerDelegate?>(null) }
    return remember(scope) {
        object : ExtendedFilePickerEvents {
            override fun pick(schema: JsonSchema, options: JsonOptions, onEvent: (FilePickerEvent) -> Unit) {
                showPicker(schema, options, false, onEvent)
            }

            override fun pickMultiple(schema: JsonSchema, options: JsonOptions, onEvent: (FilePickerEvent) -> Unit) {
                showPicker(schema, options, true, onEvent)
            }

            private fun showPicker(
                schema: JsonSchema,
                options: JsonOptions,
                multiple: Boolean,
                onEvent: (FilePickerEvent) -> Unit,
            ) {
                val presenter = UIApplication.sharedApplication.keyWindow?.rootViewController
                if (presenter == null) {
                    onEvent(FilePickerEvent.ReadFailed("No active iOS window"))
                    return
                }
                val delegate = DemoDocumentPickerDelegate(
                    selected = { urls ->
                        scope.launch {
                            val result = withContext(Dispatchers.Default) {
                                val outcomes = mutableListOf<FilePickerEvent>()
                                for (url in urls) {
                                    val outcome = readPickedFile(url, schema, options)
                                    outcomes += outcome
                                    if (outcome is FilePickerEvent.ReadFailed) break
                                }
                                if (multiple) batchFileEvent(outcomes) else outcomes.first()
                            }
                            activeDelegate = null
                            onEvent(result)
                        }
                    },
                    cancelled = {
                        activeDelegate = null
                        onEvent(FilePickerEvent.Cancelled)
                    },
                )
                activeDelegate = delegate
                val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeData))
                picker.allowsMultipleSelection = multiple
                picker.delegate = delegate
                presenter.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun readPickedFile(url: NSURL, schema: JsonSchema, options: JsonOptions): FilePickerEvent {
    val access = url.startAccessingSecurityScopedResource()
    try {
        val path = url.path ?: return FilePickerEvent.ReadFailed("Selected file has no path")
        val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null)
        val size = (attributes?.get(NSFileSize) as? NSNumber)?.longLongValue ?: -1L
        val violation = fileSizeViolation(size, options)
        if (violation != null && fileRestrictionEnabled(options)) return FilePickerEvent.RejectedSize(violation)
        val data = NSFileManager.defaultManager.contentsAtPath(path) ?: return FilePickerEvent.ReadFailed("Could not read selected file")
        val actualSize = data.length.toLong()
        val actualViolation = fileSizeViolation(actualSize, options)
        if (actualViolation != null && fileRestrictionEnabled(options)) return FilePickerEvent.RejectedSize(actualViolation)
        val bytes = data.bytes?.reinterpret<ByteVar>()?.readBytes(data.length.toInt())
            ?: return FilePickerEvent.ReadFailed("Could not access selected file bytes")
        val mime = (schema["contentMediaType"] as? JsonPrimitive)?.contentOrNull
        return selectedFileEvent(schema, options, bytes, mime, url.lastPathComponent ?: "attachment")
    } catch (exception: Throwable) {
        return FilePickerEvent.ReadFailed(exception.message)
    } finally {
        if (access) url.stopAccessingSecurityScopedResource()
    }
}
