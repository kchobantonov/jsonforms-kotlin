package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers

private fun extendedEntries(
    controls: ExtendedControlRenderers,
    layouts: ExtendedLayoutRenderers,
): List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>> = listOf(
    JsonFormsRendererRegistryEntry(buttonTester, controls.button),
    JsonFormsRendererRegistryEntry(colorControlTester, controls.color),
    JsonFormsRendererRegistryEntry(durationControlTester, controls.duration),
    JsonFormsRendererRegistryEntry(fileControlTester, controls.file),
    JsonFormsRendererRegistryEntry(nullControlTester, controls.nullValue),
    JsonFormsRendererRegistryEntry(splitLayoutTester, layouts.split),
    JsonFormsRendererRegistryEntry(slotTester, layouts.slot),
)

val materialExtendedRendererEntries = extendedEntries(materialExtendedControls, extendedLayouts)
val cupertinoExtendedRendererEntries = extendedEntries(cupertinoExtendedControls, extendedLayouts).map { entry ->
    entry.copy(tester = withIncreasedRank(100, entry.tester))
}

val materialExtendedRenderers = materialRenderers + materialExtendedRendererEntries
val cupertinoExtendedRenderers = cupertinoRenderers + cupertinoExtendedRendererEntries
