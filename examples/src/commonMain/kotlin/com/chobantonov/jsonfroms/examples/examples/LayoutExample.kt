package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.StringControlLabel
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.examples.ExampleDescription

private val sharedData = personData
private val sharedSchema = personSchema

val layoutVerticalUiSchema = VerticalLayout(
    elements = listOf(
        ControlElement(
            label = StringControlLabel("Name"),
            scope = "#/properties/name",
        ),
        ControlElement(
            label = StringControlLabel("Birth Date"),
            scope = "#/properties/birthDate",
        ),
    ),
)

val layoutHorizontalUiSchema = HorizontalLayout(
    elements = listOf(
        ControlElement(
            label = StringControlLabel("Name"),
            scope = "#/properties/name",
        ),
        ControlElement(
            label = StringControlLabel("Birth Date"),
            scope = "#/properties/birthDate",
        ),
    ),
)

val layoutGroupUiSchema = GroupElement(
    label = "My Group",
    elements = listOf(
        ControlElement(
            label = StringControlLabel("Name"),
            scope = "#/properties/name",
        ),
        ControlElement(
            label = StringControlLabel("Birth Date"),
            scope = "#/properties/birthDate",
        ),
    ),
)

val layoutComplexUiSchema = GroupElement(
    label = "My Group",
    elements = listOf(
        HorizontalLayout(
            elements = listOf(
                VerticalLayout(
                    elements = listOf(
                        ControlElement(
                            label = StringControlLabel("Name"),
                            scope = "#/properties/name",
                        ),
                        ControlElement(
                            label = StringControlLabel("Birth Date"),
                            scope = "#/properties/birthDate",
                        ),
                    ),
                ),
                VerticalLayout(
                    elements = listOf(
                        ControlElement(
                            label = StringControlLabel("Name"),
                            scope = "#/properties/name",
                        ),
                        ControlElement(
                            label = StringControlLabel("Birth Date"),
                            scope = "#/properties/birthDate",
                        ),
                    ),
                ),
            ),
        ),
    ),
)

val layoutExamples = listOf(
    ExampleDescription(
        name = "layout-vertical",
        label = "Layout Vertical",
        data = sharedData,
        schema = sharedSchema,
        uischema = layoutVerticalUiSchema,
    ),
    ExampleDescription(
        name = "layout-horizontal",
        label = "Layout Horizontal",
        data = sharedData,
        schema = sharedSchema,
        uischema = layoutHorizontalUiSchema,
    ),
    ExampleDescription(
        name = "layout-group",
        label = "Layout Group",
        data = sharedData,
        schema = sharedSchema,
        uischema = layoutGroupUiSchema,
    ),
    ExampleDescription(
        name = "layout-complex",
        label = "Layout Complex",
        data = sharedData,
        schema = sharedSchema,
        uischema = layoutComplexUiSchema,
    ),
)
