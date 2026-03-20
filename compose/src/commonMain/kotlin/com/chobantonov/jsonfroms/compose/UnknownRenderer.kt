package com.chobantonov.jsonfroms.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun UnknownRenderer(
    type: String,
) {
    Text("No applicable $type found.")
}
