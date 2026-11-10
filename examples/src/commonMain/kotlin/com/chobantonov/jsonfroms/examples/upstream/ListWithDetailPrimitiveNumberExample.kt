// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamListWithDetailPrimitiveNumberExample: ExampleDescription by lazy {
    upstreamExample(
        name = "list-with-detail-primitive-number",
        label = "List With Detail primitive (number)",
        dataJson = """
        {
          "an-array-of-numbers": [
            1,
            2,
            3
          ]
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "an-array-of-numbers": {
              "type": "array",
              "items": {
                "type": "number"
              }
            }
          }
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "ListWithDetail",
          "scope": "#/properties/an-array-of-numbers"
        }
        """.trimIndent(),
    )
}
