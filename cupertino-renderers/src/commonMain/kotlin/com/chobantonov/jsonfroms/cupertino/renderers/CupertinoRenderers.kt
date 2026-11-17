package com.chobantonov.jsonfroms.cupertino.renderers

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellRenderer
import com.chobantonov.jsonfroms.compose.asRendererProps
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeSpacerRenderer
import com.chobantonov.jsonfroms.compose.composeSpacerTester
import com.chobantonov.jsonfroms.core.store.JsonFormsCellRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.testers.plainStringCellTester
import com.chobantonov.jsonfroms.core.testers.plainNumericCellTester
import com.chobantonov.jsonfroms.core.testers.plainEnumCellTester
import com.chobantonov.jsonfroms.core.testers.plainOneOfEnumCellTester
import com.chobantonov.jsonfroms.core.testers.plainConstCellTester
import com.chobantonov.jsonfroms.core.testers.radioChoiceCellTester
import com.chobantonov.jsonfroms.core.testers.sliderCellTester
import com.chobantonov.jsonfroms.core.testers.plainTemporalCellTester
import com.chobantonov.jsonfroms.core.testers.plainPasswordCellTester
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
    JsonFormsRendererRegistryEntry(cupertinoTupleTester, CupertinoTupleRenderer),
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
    JsonFormsRendererRegistryEntry(cupertinoCategorizationAccordionTester, CupertinoCategorizationAccordionLayout),
    JsonFormsRendererRegistryEntry(cupertinoCategorizationStepperTester, CupertinoCategorizationStepperLayout),
    JsonFormsRendererRegistryEntry(cupertinoArrayLayoutTester, CupertinoArrayLayoutRenderer),
    JsonFormsRendererRegistryEntry(cupertinoLabelRendererTester, CupertinoLabelRenderer),
    JsonFormsRendererRegistryEntry(composeSpacerTester, ComposeSpacerRenderer),
    JsonFormsRendererRegistryEntry(cupertinoListWithDetailTester, CupertinoListWithDetailRenderer),
    JsonFormsRendererRegistryEntry(cupertinoCategorizationTester, CupertinoCategorizationLayout),
    JsonFormsRendererRegistryEntry(cupertinoGroupTester, CupertinoGroupLayout),
    JsonFormsRendererRegistryEntry(cupertinoHorizontalLayoutTester, CupertinoHorizontalLayout),
    JsonFormsRendererRegistryEntry(cupertinoVerticalLayoutTester, CupertinoVerticalLayout),
    JsonFormsRendererRegistryEntry(cupertinoMultiSelectTester, CupertinoMultiSelectRenderer),
    JsonFormsRendererRegistryEntry(cupertinoChipsTester, CupertinoChipsRenderer),
)

val cupertinoCells: List<JsonFormsCellRendererRegistryEntry<ComposeJsonFormsCellRenderer>> = listOf(
    JsonFormsCellRendererRegistryEntry(cupertinoBooleanToggleControlTester,
        ComposeJsonFormsCellRenderer {
            com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoBooleanControl(
                it.asRendererProps(), toggle = true, compact = true)
        }),
    JsonFormsCellRendererRegistryEntry(cupertinoBooleanControlTester,
        ComposeJsonFormsCellRenderer {
            com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoBooleanControl(
                it.asRendererProps(), toggle = false, compact = true)
        }),
    JsonFormsCellRendererRegistryEntry(plainStringCellTester,
        ComposeJsonFormsCellRenderer { CupertinoPlainTextCell(it) }),
    JsonFormsCellRendererRegistryEntry(plainNumericCellTester,
        ComposeJsonFormsCellRenderer { CupertinoPlainNumericCell(it) }),
    JsonFormsCellRendererRegistryEntry(plainEnumCellTester,
        ComposeJsonFormsCellRenderer { CupertinoPlainEnumCell(it) }),
    JsonFormsCellRendererRegistryEntry(plainOneOfEnumCellTester,
        ComposeJsonFormsCellRenderer { CupertinoPlainOneOfEnumCell(it) }),
    JsonFormsCellRendererRegistryEntry(plainConstCellTester,
        ComposeJsonFormsCellRenderer { CupertinoPlainEnumCell(it) }),
    JsonFormsCellRendererRegistryEntry(radioChoiceCellTester,
        ComposeJsonFormsCellRenderer { CupertinoPlainRadioCell(it) }),
    JsonFormsCellRendererRegistryEntry(sliderCellTester,
        ComposeJsonFormsCellRenderer { CupertinoSliderCell(it) }),
    JsonFormsCellRendererRegistryEntry(plainTemporalCellTester,
        ComposeJsonFormsCellRenderer { CupertinoTemporalCell(it) }),
    JsonFormsCellRendererRegistryEntry(plainPasswordCellTester,
        ComposeJsonFormsCellRenderer { CupertinoPasswordCell(it) }),
)
