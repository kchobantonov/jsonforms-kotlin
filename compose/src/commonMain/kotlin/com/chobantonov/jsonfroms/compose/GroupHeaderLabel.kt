package com.chobantonov.jsonfroms.compose

import androidx.compose.runtime.compositionLocalOf

/** The first rendered Group header Label can publish its resolved text for the Group name. */
val LocalGroupHeaderLabelText = compositionLocalOf<((String?) -> Unit)?> { null }
