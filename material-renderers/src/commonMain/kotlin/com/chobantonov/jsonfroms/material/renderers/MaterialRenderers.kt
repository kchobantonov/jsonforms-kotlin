package com.chobantonov.jsonfroms.material.renderers

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.core.store.JsonFormsCellRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.material.renderers.additional.MaterialLabelRenderer
import com.chobantonov.jsonfroms.material.renderers.additional.MaterialListWithDetailRenderer
import com.chobantonov.jsonfroms.material.renderers.additional.materialLabelRendererTester
import com.chobantonov.jsonfroms.material.renderers.additional.materialListWithDetailTester
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialAllOfRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialAnyOfRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialArrayControlRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialEnumArrayRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.materialArrayControlTester
import com.chobantonov.jsonfroms.material.renderers.complex.materialAllOfControlTester
import com.chobantonov.jsonfroms.material.renderers.complex.materialAnyOfControlTester
import com.chobantonov.jsonfroms.material.renderers.complex.materialEnumArrayRendererTester
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialObjectRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.MaterialOneOfRenderer
import com.chobantonov.jsonfroms.material.renderers.complex.materialObjectControlTester
import com.chobantonov.jsonfroms.material.renderers.complex.materialOneOfControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialAnyOfStringOrEnumControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialBooleanControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialBooleanToggleControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialDateControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialDateTimeControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialEnumControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialIntegerControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialMultiTextControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialNativeControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialNumberControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialOneOfEnumControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialOneOfRadioGroupControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialPasswordControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialRadioGroupControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialSliderControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialTextControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialTimeControl
import com.chobantonov.jsonfroms.material.renderers.controls.materialAnyOfStringOrEnumControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialBooleanControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialBooleanToggleControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialDateControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialDateTimeControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialEnumControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialIntegerControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialMultiTextControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialNativeControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialNumberControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialOneOfEnumControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialOneOfRadioGroupControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialPasswordControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialRadioGroupControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialSliderControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialTextControlTester
import com.chobantonov.jsonfroms.material.renderers.controls.materialTimeControlTester
import com.chobantonov.jsonfroms.material.renderers.layouts.MaterialCategorizationLayout
import com.chobantonov.jsonfroms.material.renderers.layouts.MaterialCategorizationStepperLayout
import com.chobantonov.jsonfroms.material.renderers.layouts.MaterialArrayLayoutRenderer
import com.chobantonov.jsonfroms.material.renderers.layouts.MaterialGroupLayout
import com.chobantonov.jsonfroms.material.renderers.layouts.MaterialHorizontalLayout
import com.chobantonov.jsonfroms.material.renderers.layouts.MaterialVerticalLayout
import com.chobantonov.jsonfroms.material.renderers.layouts.materialArrayLayoutTester
import com.chobantonov.jsonfroms.material.renderers.layouts.materialCategorizationTester
import com.chobantonov.jsonfroms.material.renderers.layouts.materialCategorizationStepperTester
import com.chobantonov.jsonfroms.material.renderers.layouts.materialGroupTester
import com.chobantonov.jsonfroms.material.renderers.layouts.materialHorizontalLayoutTester
import com.chobantonov.jsonfroms.material.renderers.layouts.materialVerticalLayoutTester

val materialRenderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>> = listOf(
    JsonFormsRendererRegistryEntry(
        tester = materialAnyOfStringOrEnumControlTester,
        renderer = MaterialAnyOfStringOrEnumControl,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialAnyOfControlTester,
        renderer = MaterialAnyOfRenderer,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialAllOfControlTester,
        renderer = MaterialAllOfRenderer,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialBooleanControlTester,
        renderer = MaterialBooleanControl,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialBooleanToggleControlTester,
        renderer = MaterialBooleanToggleControl,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialDateControlTester,
        renderer = MaterialDateControl,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialDateTimeControlTester,
        renderer = MaterialDateTimeControl,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialEnumArrayRendererTester,
        renderer = MaterialEnumArrayRenderer,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialEnumControlTester,
        renderer = MaterialEnumControl,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialIntegerControlTester,
        renderer = MaterialIntegerControl,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialNativeControlTester,
        renderer = MaterialNativeControl,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialNumberControlTester,
        renderer = MaterialNumberControl,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialArrayControlTester,
        renderer = MaterialArrayControlRenderer,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialObjectControlTester,
        renderer = MaterialObjectRenderer,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialOneOfControlTester,
        renderer = MaterialOneOfRenderer,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialOneOfEnumControlTester,
        renderer = MaterialOneOfEnumControl,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialOneOfRadioGroupControlTester,
        renderer = MaterialOneOfRadioGroupControl,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialRadioGroupControlTester,
        renderer = MaterialRadioGroupControl,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialPasswordControlTester,
        renderer = MaterialPasswordControl,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialSliderControlTester,
        renderer = MaterialSliderControl,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialMultiTextControlTester,
        renderer = MaterialMultiTextControl,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialTextControlTester,
        renderer = MaterialTextControl,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialTimeControlTester,
        renderer = MaterialTimeControl,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialCategorizationStepperTester,
        renderer = MaterialCategorizationStepperLayout,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialArrayLayoutTester,
        renderer = MaterialArrayLayoutRenderer,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialLabelRendererTester,
        renderer = MaterialLabelRenderer,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialListWithDetailTester,
        renderer = MaterialListWithDetailRenderer,
    ),
    JsonFormsRendererRegistryEntry(
        tester = materialCategorizationTester,
        renderer = MaterialCategorizationLayout,
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
