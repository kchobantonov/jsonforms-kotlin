package com.chobantonov.jsonfroms.material.renderers

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.core.store.JsonFormsCellRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialTextControl
import com.chobantonov.jsonfroms.material.renderers.controls.materialTextControlTester
import com.chobantonov.jsonfroms.material.renderers.layouts.MaterialGroupLayout
import com.chobantonov.jsonfroms.material.renderers.layouts.MaterialHorizontalLayout
import com.chobantonov.jsonfroms.material.renderers.layouts.MaterialVerticalLayout
import com.chobantonov.jsonfroms.material.renderers.layouts.materialGroupTester
import com.chobantonov.jsonfroms.material.renderers.layouts.materialHorizontalLayoutTester
import com.chobantonov.jsonfroms.material.renderers.layouts.materialVerticalLayoutTester

val materialRenderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>> = listOf(
    JsonFormsRendererRegistryEntry(
        tester = materialTextControlTester,
        renderer = MaterialTextControl,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialGroupTester,
        renderer = MaterialGroupLayout,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialHorizontalLayoutTester,
        renderer = MaterialHorizontalLayout,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialVerticalLayoutTester,
        renderer = MaterialVerticalLayout,
    ),
)

val materialCells: List<JsonFormsCellRendererRegistryEntry<ComposeJsonFormsCellRenderer>> = emptyList()
