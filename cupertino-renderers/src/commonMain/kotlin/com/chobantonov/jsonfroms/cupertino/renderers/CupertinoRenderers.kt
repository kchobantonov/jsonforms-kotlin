package com.chobantonov.jsonfroms.cupertino.renderers

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.core.store.JsonFormsCellRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.cupertino.renderers.additional.CupertinoLabelRenderer
import com.chobantonov.jsonfroms.cupertino.renderers.additional.CupertinoListWithDetailRenderer
import com.chobantonov.jsonfroms.cupertino.renderers.additional.cupertinoLabelRendererTester
import com.chobantonov.jsonfroms.cupertino.renderers.additional.cupertinoListWithDetailTester
import com.chobantonov.jsonfroms.cupertino.renderers.complex.*
import com.chobantonov.jsonfroms.cupertino.renderers.controls.*
import com.chobantonov.jsonfroms.cupertino.renderers.layouts.*

val cupertinoRenderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>> = listOf(
    JsonFormsRendererRegistryEntry(cupertinoMixedControlTester, CupertinoMixedRenderer),
    JsonFormsRendererRegistryEntry(cupertinoAnyOfStringOrEnumControlTester, CupertinoAnyOfStringOrEnumControl),
    JsonFormsRendererRegistryEntry(cupertinoAnyOfControlTester, CupertinoAnyOfRenderer),
    JsonFormsRendererRegistryEntry(cupertinoAllOfControlTester, CupertinoAllOfRenderer),
    JsonFormsRendererRegistryEntry(cupertinoBooleanControlTester, CupertinoBooleanControl),
    JsonFormsRendererRegistryEntry(cupertinoBooleanToggleControlTester, CupertinoBooleanToggleControl),
    JsonFormsRendererRegistryEntry(cupertinoDateControlTester, CupertinoDateControl),
    JsonFormsRendererRegistryEntry(cupertinoDateTimeControlTester, CupertinoDateTimeControl),
    JsonFormsRendererRegistryEntry(cupertinoEnumArrayRendererTester, CupertinoEnumArrayRenderer),
    JsonFormsRendererRegistryEntry(cupertinoEnumControlTester, CupertinoEnumControl),
    JsonFormsRendererRegistryEntry(cupertinoIntegerControlTester, CupertinoIntegerControl),
    JsonFormsRendererRegistryEntry(cupertinoNativeControlTester, CupertinoNativeControl),
    JsonFormsRendererRegistryEntry(cupertinoNumberControlTester, CupertinoNumberControl),
    JsonFormsRendererRegistryEntry(cupertinoArrayControlTester, CupertinoArrayControlRenderer),
    JsonFormsRendererRegistryEntry(cupertinoObjectControlTester, CupertinoObjectRenderer),
    JsonFormsRendererRegistryEntry(cupertinoOneOfControlTester, CupertinoOneOfRenderer),
    JsonFormsRendererRegistryEntry(cupertinoOneOfEnumControlTester, CupertinoOneOfEnumControl),
    JsonFormsRendererRegistryEntry(cupertinoOneOfRadioGroupControlTester, CupertinoOneOfRadioGroupControl),
    JsonFormsRendererRegistryEntry(cupertinoRadioGroupControlTester, CupertinoRadioGroupControl),
    JsonFormsRendererRegistryEntry(cupertinoPasswordControlTester, CupertinoPasswordControl),
    JsonFormsRendererRegistryEntry(cupertinoSliderControlTester, CupertinoSliderControl),
    JsonFormsRendererRegistryEntry(cupertinoMultiTextControlTester, CupertinoMultiTextControl),
    JsonFormsRendererRegistryEntry(cupertinoTextControlTester, CupertinoTextControl),
    JsonFormsRendererRegistryEntry(cupertinoTimeControlTester, CupertinoTimeControl),
    JsonFormsRendererRegistryEntry(cupertinoCategorizationStepperTester, CupertinoCategorizationStepperLayout),
    JsonFormsRendererRegistryEntry(cupertinoArrayLayoutTester, CupertinoArrayLayoutRenderer),
    JsonFormsRendererRegistryEntry(cupertinoLabelRendererTester, CupertinoLabelRenderer),
    JsonFormsRendererRegistryEntry(cupertinoListWithDetailTester, CupertinoListWithDetailRenderer),
    JsonFormsRendererRegistryEntry(cupertinoCategorizationTester, CupertinoCategorizationLayout),
    JsonFormsRendererRegistryEntry(cupertinoGroupTester, CupertinoGroupLayout),
    JsonFormsRendererRegistryEntry(cupertinoHorizontalLayoutTester, CupertinoHorizontalLayout),
    JsonFormsRendererRegistryEntry(cupertinoVerticalLayoutTester, CupertinoVerticalLayout),
)

val cupertinoCells: List<JsonFormsCellRendererRegistryEntry<ComposeJsonFormsCellRenderer>> = emptyList()
