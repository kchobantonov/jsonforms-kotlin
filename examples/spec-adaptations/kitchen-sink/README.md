# Kitchen sink portable adaptation

The pinned source fixture has an AG Grid reference table and a Monaco code-editor category. This adaptation omits those two browser-only UI entries while retaining the native reference table, all other portable controls and layouts, the schema, data, configuration, invalid data, and the English/Bulgarian translations. In the invalid-data sample, the AG Grid-only `gridTeam[4].phone` value is restored to its valid source value; the nine portable invalid paths remain. It does not claim AG Grid or Monaco behavior for Kotlin Multiplatform.
