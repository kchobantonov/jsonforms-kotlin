# Read-only and enabled-state capability

The portable core resolves read-only independently from enabled state when
`separateReadonlyFromDisabled: true`. With the default `false`, effective read-only
also disables the control. Form-wide `readonly: true` remains authoritative;
`READONLY` and `WRITABLE` rules decide read-only in both directions, while an
unrelated visibility or enablement rule leaves read-only sources in force.

| Renderer family | Text input | Family-wide capability |
| --- | --- | --- |
| Material | Enabled, read-only text inspection is supported for shared text inputs. Clear is guarded. | Unsupported until choice, toggle, slider, picker, cell and container mutation paths have interaction and accessibility evidence. |
| Cupertino | Enabled, read-only text inspection is supported for shared text inputs. Clear is guarded. | Unsupported until the same remaining paths have interaction and accessibility evidence. |

The paired mounted JVM tests in `ReadonlyRulePrecedenceUiTest` verify a live
`WRITABLE` rule overriding an explicit read-only option and the enabled versus
read-only text distinction. The focused iOS simulator run passes the same four
mounted methods. Core `RuntimeTest` verifies precedence with unrelated rules,
form-wide state and matched/unmatched two-way decisions. These tests do not
establish family-wide support on Android, JS or Wasm.
