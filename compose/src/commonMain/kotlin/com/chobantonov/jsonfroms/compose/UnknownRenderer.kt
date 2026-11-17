package com.chobantonov.jsonfroms.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun UnknownRenderer(
    type: String,
    message: String? = null,
) {
    Text(message ?: "No applicable $type found.")
}
