package com.chobantonov.jsonfroms.compose

import androidx.compose.runtime.compositionLocalOf
import com.chobantonov.jsonfroms.core.models.Scoped
import com.chobantonov.jsonfroms.core.models.UiSchemaElement

/** The dispatch target and data path identify a recursive object-detail handoff. */
data class ObjectDetailSignature(val path: String, val type: String, val scope: String?)

fun objectDetailSignature(uiSchema: UiSchemaElement, path: String): ObjectDetailSignature =
    ObjectDetailSignature(path, uiSchema.type, (uiSchema as? Scoped)?.scope)

val LocalObjectDetailStack = compositionLocalOf<Set<ObjectDetailSignature>> { emptySet() }
