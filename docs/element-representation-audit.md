# UI element representation audit

The serialized UI schema parser in `core/models/UiSchemaJson.kt` maps the existing JSON Forms element names to Kotlin model types. The extension uses those names for the concepts in spec §4:

| Concept | Serialized representation | Kotlin model | Evidence |
| --- | --- | --- | --- |
| Editable or viewable data | `Control` with `scope` and options | `ControlElement` | `ExistingRepresentationFixtureUiTest` |
| Horizontal or vertical structure | `HorizontalLayout`, `VerticalLayout` | `HorizontalLayout`, `VerticalLayout` | `ExistingRepresentationFixtureUiTest`, `LayoutDynamicUiTest` |
| Bounded or collapsible section | `Group`, optionally `options.collapsible` | `GroupElement` | `ExistingRepresentationFixtureUiTest`, `GroupExpansionUiTest` |
| Tabs, stepper, accordion | `Categorization`, with `options.variant` for stepper or accordion | `CategorizationElement`, `CategoryElement` | `SpecCategorizationFixtureUiTest` |
| Chips, multi-select, table, switch, slider, code editor | `Control` options | `ControlElement` | `UiSchemaJsonTest`, `PresentationSelectionTest` |
| Rich text | `Label` with text options | `LabelElement` | `LabelInterpolationUiTest` |
| Image, divider, whitespace | `ImageView`, `Separator`, `Spacer` | `ImageViewElement`, `SeparatorElement`, `SpacerElement` | `ImageViewUiTest`, `LayoutSpacerUiTest` |
| Command and navigation | `Button`, `Link` | `UnknownUiSchemaElement` for Button, `LinkElement` for Link | `ButtonActionUiTest`, `LinkUiTest` |

`Button` is an extension element named by the spec. The core parser keeps its authored properties and children in `UnknownUiSchemaElement`; the extended renderer handles it by type, so the core model does not add another command type. `TemplateLayout` and other future or vendor types use the same passthrough. `ListWithDetail` is an existing JSON Forms element with its own model type, not a new alias for table or row detail. The listed concepts therefore do not introduce parallel Kotlin element types. This is a model and fixture audit; complete target and option conformance remains tracked in the requirement ledger.
