# Destructive confirmation adaptation

The pinned upstream fixture uses AG Grid for `charges`. This adaptation selects the portable native table (`options.table: true`) for that control and updates its schema description and English/Bulgarian translation to describe single-row deletion. Data, confirmation configuration, and every other control remain upstream. Native table row deletion is exercised in both renderer families on JVM, iOS, JS and Wasm. AG Grid selection and batch deletion are outside the Kotlin renderer scope.
