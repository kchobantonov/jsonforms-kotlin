package com.chobantonov.jsonfroms.compose

import androidx.compose.runtime.compositionLocalOf
import com.chobantonov.jsonfroms.core.validation.ErrorObject

/** Prevent the same missing dependency from being announced by nested layout boundaries. */
val LocalHandledDependencyErrors = compositionLocalOf<Set<ErrorObject>> { emptySet() }
