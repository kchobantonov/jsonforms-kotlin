package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.asRendererProps
import com.chobantonov.jsonfroms.core.store.JsonFormsCellRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoCells
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialCells
import com.chobantonov.jsonfroms.material.renderers.materialRenderers

private fun extendedEntries(
    controls: ExtendedControlRenderers,
    layouts: ExtendedLayoutRenderers,
    cupertino: Boolean,
): List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>> = listOf(
    JsonFormsRendererRegistryEntry(buttonTester, controls.button),
    JsonFormsRendererRegistryEntry(colorControlTester, controls.color),
    JsonFormsRendererRegistryEntry(durationControlTester, controls.duration),
    JsonFormsRendererRegistryEntry(cronControlTester, controls.cron),
    JsonFormsRendererRegistryEntry(fileControlTester, controls.file),
    JsonFormsRendererRegistryEntry(nullControlTester, controls.nullValue),
    JsonFormsRendererRegistryEntry(linkTester, linkRenderer(cupertino)),
    JsonFormsRendererRegistryEntry(separatorTester, separatorRenderer(cupertino)),
    JsonFormsRendererRegistryEntry(splitLayoutTester, layouts.split),
    JsonFormsRendererRegistryEntry(slotTester, layouts.slot),
    JsonFormsRendererRegistryEntry(templateTester, layouts.template),
    JsonFormsRendererRegistryEntry(unsupportedTemplateLayoutTester, unsupportedTemplateLayoutRenderer),
    JsonFormsRendererRegistryEntry(codeProfileFallbackTester,
        codeProfileFallbackRenderer(if (cupertino) cupertinoRenderers else materialRenderers)),
    JsonFormsRendererRegistryEntry(imageViewTester, imageViewRenderer(cupertino)),
    JsonFormsRendererRegistryEntry(markupLabelTester, markupLabelRenderer(cupertino),
        handlesLabelInterpolation = true),
)

val materialExtendedRendererEntries = extendedEntries(materialExtendedControls, extendedLayouts, cupertino = false)
val cupertinoExtendedRendererEntries = extendedEntries(cupertinoExtendedControls, extendedLayouts, cupertino = true).map { entry ->
    entry.copy(tester = withIncreasedRank(100, entry.tester))
}

val materialExtendedRenderers = materialRenderers + materialExtendedRendererEntries
val cupertinoExtendedRenderers = cupertinoRenderers + cupertinoExtendedRendererEntries

val materialExtendedCells: List<JsonFormsCellRendererRegistryEntry<ComposeJsonFormsCellRenderer>> = materialCells + listOf(
    JsonFormsCellRendererRegistryEntry(colorControlTester,
        ComposeJsonFormsCellRenderer { ColorControl(it.asRendererProps(), cupertino = false, compact = true) },
        ownsValidationFeedback = true),
    JsonFormsCellRendererRegistryEntry(durationControlTester,
        ComposeJsonFormsCellRenderer { DurationControl(it.asRendererProps(), cupertino = false, compact = true) },
        ownsValidationFeedback = true),
    JsonFormsCellRendererRegistryEntry(cronControlTester,
        ComposeJsonFormsCellRenderer { CronControl(it.asRendererProps(), cupertino = false, compact = true) },
        ownsValidationFeedback = true),
    JsonFormsCellRendererRegistryEntry(nullControlTester,
        ComposeJsonFormsCellRenderer { NullCell(it, cupertino = false) }, ownsValidationFeedback = true),
    JsonFormsCellRendererRegistryEntry(fileControlTester,
        ComposeJsonFormsCellRenderer { FileCell(it, cupertino = false) }, ownsValidationFeedback = true),
)

val cupertinoExtendedCells: List<JsonFormsCellRendererRegistryEntry<ComposeJsonFormsCellRenderer>> = cupertinoCells + listOf(
    JsonFormsCellRendererRegistryEntry(
        withIncreasedRank(100, colorControlTester),
        ComposeJsonFormsCellRenderer { ColorControl(it.asRendererProps(), cupertino = true, compact = true) },
        ownsValidationFeedback = true,
    ),
    JsonFormsCellRendererRegistryEntry(
        withIncreasedRank(100, durationControlTester),
        ComposeJsonFormsCellRenderer { DurationControl(it.asRendererProps(), cupertino = true, compact = true) },
        ownsValidationFeedback = true,
    ),
    JsonFormsCellRendererRegistryEntry(
        withIncreasedRank(100, cronControlTester),
        ComposeJsonFormsCellRenderer { CronControl(it.asRendererProps(), cupertino = true, compact = true) },
        ownsValidationFeedback = true,
    ),
    JsonFormsCellRendererRegistryEntry(
        withIncreasedRank(100, nullControlTester),
        ComposeJsonFormsCellRenderer { NullCell(it, cupertino = true) },
        ownsValidationFeedback = true,
    ),
    JsonFormsCellRendererRegistryEntry(
        withIncreasedRank(100, fileControlTester),
        ComposeJsonFormsCellRenderer { FileCell(it, cupertino = true) },
        ownsValidationFeedback = true,
    ),
)
