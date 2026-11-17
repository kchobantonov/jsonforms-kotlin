package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.ImageViewElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

enum class ImageSourceDiagnostic(val code: String) {
    NoSource("image.noSource"),
    ScopeNotString("image.scopeNotString"),
    NonStringSource("image.nonStringSource"),
    UrlRefused("image.urlRefused"),
}

data class ImageSourceResolution(
    val url: String? = null,
    val diagnostic: ImageSourceDiagnostic? = null,
)

/** Resolves an ImageView without coercing data or falling back from a defined src. */
fun resolveImageSource(
    element: ImageViewElement,
    schema: JsonSchema,
    rootSchema: JsonSchema,
    data: JsonElement?,
    path: String,
    policy: UrlPolicy,
): ImageSourceResolution {
    val source = if (element.src != null) {
        element.src
    } else {
        val scope = element.scope ?: return ImageSourceResolution(diagnostic = ImageSourceDiagnostic.NoSource)
        val scopedSchema = resolveSchema(schema, scope, rootSchema)
            ?: return ImageSourceResolution(diagnostic = ImageSourceDiagnostic.ScopeNotString)
        val types = deriveTypes(scopedSchema)
        if (types.isNotEmpty() && "string" !in types) {
            return ImageSourceResolution(diagnostic = ImageSourceDiagnostic.ScopeNotString)
        }
        val scopedPath = composePaths(path, toDataPath(scope))
        when (val value = Resolve.data(data, scopedPath)) {
            null, JsonNull -> null
            is JsonPrimitive -> if (value.isString) value.content
                else return ImageSourceResolution(diagnostic = ImageSourceDiagnostic.NonStringSource)
            else -> return ImageSourceResolution(diagnostic = ImageSourceDiagnostic.NonStringSource)
        }
    }
    if (source.isNullOrEmpty()) return ImageSourceResolution()
    return if (isAllowedImageUrl(source, policy)) ImageSourceResolution(url = source)
        else ImageSourceResolution(diagnostic = ImageSourceDiagnostic.UrlRefused)
}
