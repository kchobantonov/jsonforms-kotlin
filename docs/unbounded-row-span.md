# Unbounded horizontal row spans

For a measured row width `W`, grid count `G`, gap `g`, and clamped span `s`, the shared mapper resolves a span as:

`max(0, (s / G) × W − (1 − s / G) × g)`

This is algebraically equal to `s × ((W − (G − 1) × g) / G) + (s − 1) × g`. The same expression feeds wrapped-row fitting and final widths. A pure test checks equality across widths, gaps and spans.

Compose rows normally use their measured width. If an ancestor, such as horizontal scrolling, supplies an unbounded maximum, Material and Cupertino use the current window container width as an explicit row basis and constrain the row to that width. This keeps span widths finite and preserves the percentage-of-row expression. A narrower nested scroll viewport can clip that window-width row; the scroll container can reveal the rest. If the window has no positive width yet, children remain in authored order in a vertical fallback until a usable window width is available. The mounted scroll test checks both families' unbounded row widths and confirms no form write.

The window-width basis is an approximation when a nested scroll viewport is narrower than the window. Direct Android device geometry and further constrained-parent scenarios remain open in the ledger.
