# Variant contract audit

`options.variant` selects one portable presentation mode for an applicable UI element. The following modes have distinct behavior in the current Kotlin renderers:

| Element and variant | Added capability | Evidence |
| --- | --- | --- |
| Categorization `stepper` | Sequential category navigation and step status | `SpecCategorizationFixtureUiTest`, `CategoryValidationIndicatorUiTest` |
| Categorization `accordion` | Independently collapsible category sections | `SpecCategorizationFixtureUiTest` |
| Control `chips` | Token-based array editing, including free text when the schema permits it | `ArrayChoiceProfileTest`, `ArrayChoiceActionPolicyUiTest` |
| Control `multi-select` | Multiple finite array choices in one selector | `ArrayChoiceProfileTest`, `ArrayChoiceActionPolicyUiTest` |
| Control `otp` | Fixed-length password boxes around one stored string, with keyboard position editing | `OtpPasswordUiTest` |
| Control `tuple` | Positional array editing with fixed prefix slots and tail rules | `TupleSchemaTest`, `TupleUiTest` |
| HorizontalLayout/VerticalLayout `splitter` | Resizable pane proportions; `resizable` remains a separate Boolean behavior flag | `ExtendedRenderersTest`, `SplitWrapDiagnosticTest` |

The `splitter` selector requires the exact authored JSON string. Case-folded aliases are not selected. Material input chrome uses `options.material.inputVariant`, leaving portable `variant` for presentation selection.

`ag-grid` is an optional web profile request. Current Kotlin renderers show a data-preserving portable table and an unsupported diagnostic; this does not establish AG Grid's added capability. Monaco uses `options.format: "code"`, and TemplateLayout is a separate element, so neither is a portable variant. The ledger tracks their browser-only integration gate. Other variant/option consumers and mounted target behavior remain to be audited before declaring the whole contract conformant.
