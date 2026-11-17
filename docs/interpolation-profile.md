# Label interpolation profile

The optional extended Label renderer supports `options.interpolate: true`. Its current expression
evaluator is a **partial, portable implementation** of the canonical CEL profile in spec §9. It
never executes host code. Unsupported expressions contribute empty text and report
`markup.interpolationFailed` beside the remaining Label text.

## Implemented now

- Single-brace expressions and doubled-brace literals; the splitter ignores closing braces inside
  quoted strings.
- Static `textParams`, JSON scalar/object/list values, and gated reads from `data`, `item`, `config`
  and `context`. Only declared parameters and `locale` are visible in the translated text.
- Own-property dot and quoted/indexed bracket lookup, including single-quoted keys containing `]`.
  Missing nested properties and out-of-range indexes are empty and quiet. Unknown roots, wrong types
  and nonnumeric array indexes are errors.
- String/number literals, booleans and null; grouping, arithmetic and ordering with precedence;
  equality/inequality, short-circuit `&&`/`||`, unary `!`, nested conditionals, `size`, `has`, and
  list/object `in` membership. Signed integer arithmetic and unary negation report overflow rather
  than wrapping; the minimum signed integer literal retains its exact value. Arithmetic rejects
  implicit integer/double mixing, remainder requires integers, and whole-valued double results
  retain their numeric type through later operations. Runtime numeric equality and ordering compare
  integers with doubles without rounding a 64-bit integer first; list membership and nested JSON
  equality use the same numeric relation. Boolean ordering uses false before true.
- Ordinary single- and double-quoted CEL strings with punctuation, whitespace, hexadecimal,
  Unicode and octal escapes. Invalid escapes and surrogate code points report author errors.
- Registered `translate`, `number`, `currency`, `percent`, `date`, `time` and `dateTime` functions.
  Formatting uses the target's locale facilities. Date-only and time-only strings retain their
  calendar or clock fields in UTC; ISO date-times with an explicit offset display in the viewer's zone.
  ISO date-times without an offset retain their wall fields in the viewer's local zone. An ambiguous
  local clock time selects the earlier occurrence; a nonexistent spring-gap time delegates to the
  host date parser. JVM tests verify forward normalization; JS and Wasm browser tests cover the
  portable temporal parser, while target-specific gap behavior still needs runtime checks.
- Escaping of substituted values before basic Markdown parsing; authored Markdown remains active.

`JsonForms(context = jsonValue)` supplies inert host values. A missing argument inherits the nearest
parent form's context. `config` is copied into JSON and omits non-JSON host objects such as diagnostic
reporters. The current `item` provider covers expanded array details, ListWithDetail and table side
panels, isolated table row/cell dialogs and tuple dialogs. Draft dialogs read `item` from their
current private form, so labels update before Apply. Other nested detail surfaces still need an
item-context audit.

## Remaining conformance work

- Replace or extend the evaluator to cover the remaining canonical CEL grammar, typed arithmetic,
  collection expressions, raw/triple-quoted strings and macros. The current parser reports
  unsupported syntax; it is not advertised as a complete CEL implementation.
- Broaden temporal parsing only where reference semantics can be preserved across targets. The
  portable parser currently accepts ISO calendar dates, clock times, local date-times, and
  date-times with `Z` or numeric offsets; other date strings report author errors.
- Complete item-context propagation through remaining nested-detail paths, then test live host
  context and item changes on browser and device targets.
- Extend target formatter tests. JVM tests verify English/Bulgarian outputs, date-only and time-only
  zone stability, offset-instant conversion, local date-time wall fields, and daylight-saving
  overlap/gap behavior. All 18 pure interpolation tests execute and pass in JS and Wasm browsers and the iOS
  simulator after the `Intl` constructor fix, including locale formatting and temporal parsing.
  The iOS locale test accepts the platform formatter’s narrow no-break space before AM/PM.
  Mounted browser UI tests still fail before rendering in the Skia test harness; Android runtime behavior and
  target-specific zone/overlap/gap formatting beyond these pure tests remain unverified.
- The integer overflow and minimum-literal regression passes on JVM, iOS simulator and Wasm browser;
  Android and JS compile. JS browser execution of this new case remains open.
- The mixed-arithmetic, integer-only remainder, numeric comparison and Boolean ordering regressions
  pass on JVM, iOS simulator and Wasm browser; all four non-JVM targets compile. JS browser execution
  remains open. The rest of CEL's typed expression model still needs a full conformance pass.
