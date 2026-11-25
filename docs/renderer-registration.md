# Renderer registration and option namespaces

These are the ranks declared by the current Kotlin registries. A tester returns `-1` when it does not apply. The dispatcher chooses the highest applicable rank; at equal rank, the **earlier entry in the supplied list** wins. This rule applies independently to renderer and compact-cell registries. The numbers describe selection priority, not feature completeness. See the [ordered registry inventory](renderer-registry-inventory.json) and the [behavior catalog](renderer-behavior-catalog.md) for selection conditions and limitations.

Hosts should supply one family registry: `materialRenderers` or `cupertinoRenderers`, or its matching extended registry. A host renderer can override a built-in entry by using a higher rank. At the same rank, prepend the host entry. Appending an equal-rank entry leaves the built-in entry selected. The explicit tuple variant has rank **1000** in either family, so an unconditional host override of it needs a higher rank. A mounted [registration test](../examples-compose/src/commonTest/kotlin/com/chobantonov/jsonfroms/examples/compose/RendererRegistrationUiTest.kt) exercises high-rank append and equal-rank prepend with both families and verifies that selection makes no form write.

## Base renderer ranks

The two registry lists have the same entry order. A range means the tester chooses a rank based on the schema or authored option; it does not make every rank in that range applicable to every Control. The Cupertino entries marked *shared* intentionally reuse the Material tester without a 100-point offset.

| Presentation or element | Material | Cupertino |
| --- | ---: | ---: |
| Mixed Control | 20 | 120 |
| String plus finite `anyOf` suggestion | 5 | 105 |
| General `anyOf`, `allOf`, `oneOf` | 3 | 103 |
| Boolean checkbox | 2 | 102 |
| Boolean toggle | 3 | 103 |
| Date, Time | 4 | 104 |
| Date-time | 2 | 102 |
| Automatic finite array checkbox | 4 | 4 shared |
| Enum Control | 2 or 3 | 102 or 103 |
| Integer, Number | 2 | 102 |
| Native date/time text fallback | 2 | 2 shared priority |
| Tuple Control | 6; explicit `variant: "tuple"` 1000 | same shared |
| Array Control | 3; explicit table/columns/AG Grid fallback 5 | same shared |
| Object Control | 2; conditional-fields projection 4 | 102 or 104 |
| Const `oneOf` selector | 5 | 105 |
| Enum or `oneOf` radio | 20 | 120 |
| Password | 2 | 102 |
| Bounded numeric slider | 4 | 104 |
| Multiline string | 2 | 102 |
| Ordinary string | 1 | 101 |
| Categorization accordion | 3 | 3 shared |
| Categorization stepper | 2 | 2 shared |
| Nested array layout | 4 | 4 shared |
| Plain Label | 1 | 101 |
| Spacer | 1 | 1 shared |
| ListWithDetail | 4 | 4 shared |
| Single-level Categorization | 1 | 1 shared |
| Group | 2 | 102 |
| HorizontalLayout | 2 | 102 |
| VerticalLayout | 1 | 101 |
| Multi-select or chips array | 6 | 6 shared priority |

## Extended renderer ranks

`materialExtendedRenderers` appends these entries after the Material base list. `cupertinoExtendedRenderers` appends the matching Cupertino entries after its base list and adds 100 to each applicable extended rank. This offset does not alter `-1` for a nonmatching tester. For example, a Material date and time format conflict at rank 4 is resolved by base list order; a duration and cron conflict at rank 3 is resolved by extended list order.

| Extended presentation or element | Material | Cupertino |
| --- | ---: | ---: |
| Button | 1 | 101 |
| Color, duration, cron string Control | 3 | 103 |
| Scalar File; array File | 2; 7 | 102; 107 |
| Null Control | 2 | 102 |
| Link, Separator | 2 | 102 |
| Split layout | 5 | 105 |
| Slot, named Template | 2 | 102 |
| Unsupported TemplateLayout diagnostic | 2 | 102 |
| Unavailable Monaco code profile fallback | 30 | 130 |
| ImageView | 2 | 102 |
| Interpolated or markup Label | 2 | 102 |

## Compact-cell ranks

The Material and Cupertino base cell registries use the same shared compact-cell testers except for Boolean controls. The extended cell registries append color, duration, cron, null and File cells, with the same 100-point Cupertino offset as the extended renderers. A nonmatching cell falls back through the table's ordinary renderer path; these ranks do not change the outer renderer registry.

| Cell presentation | Material | Cupertino |
| --- | ---: | ---: |
| Boolean checkbox; toggle | 2; 3 | 102; 103 |
| Plain string; numeric | 1; 2 | 1; 2 |
| Finite enum; scalar const; const `oneOf` | 103; 104; 105 | same shared |
| Radio choice; slider | 120; 110 | same shared |
| Date/time/date-time; password | 104; 104 | same shared |
| Extended color, duration, cron | 3 | 103 |
| Extended null; scalar File; array File | 2; 2; 7 | 102; 102; 107 |

## Wire names and escape hatches

Per-element `options` stay flat for portable selection keys such as `format`, `variant`, `multi`, `toggle`, `table`, `columnDefs`, `focus` and `clearable`. Global portable conventions such as `restrict`, `showUnfocusedDescription`, `hideRequiredAsterisk`, `disableAdd` and `disableRemove` stay at top level in `config`. The shared extended blocks use the literal `config.jsonformsExtended` key, including `layoutDefaults`, `dialog`, `radio`, `tuple`, `confirmation`, `dynamicValues`, `security`, `markup` and the other documented extended options. A wrong-case or relocated key is not an alias.

The Material-specific input style lives at `options.material.inputVariant`, with its form default at `config.jsonformsExtended.material.inputVariant`. This namespace does not redefine portable `options.variant`. Cupertino has no separate input-style option namespace in the current pack; Cupertino-specific UI is selected by supplying its registry. Unknown React, Svelte and vendor option bags are preserved by the parser/editor but are not interpreted by these Kotlin renderers. A parsed Button script requires explicit `config.jsonformsExtended.security.allowScriptEvaluation` permission and a capable host before execution. Browser-only AG Grid, Monaco and TemplateLayout contracts remain explicitly unsupported or use the documented portable fallback; preserving their authored options does not activate those engines.

The [structured option policy](structured-option-merge-policy.md) gives per-field precedence for nested blocks. The [browser integration review](web-only-integration-review.md) records the web-only boundary and diagnostics. The registry source files are [Material](../material-renderers/src/commonMain/kotlin/com/chobantonov/jsonfroms/material/renderers/MaterialRenderers.kt), [Cupertino](../cupertino-renderers/src/commonMain/kotlin/com/chobantonov/jsonfroms/cupertino/renderers/CupertinoRenderers.kt), and [extended](../extended-renderers/src/commonMain/kotlin/com/chobantonov/jsonfroms/extended/renderers/ExtendedRenderers.kt).
