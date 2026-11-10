// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamOneOf1273SimpleExample: ExampleDescription by lazy {
    upstreamExample(
        name = "oneOf_1273_simple",
        label = "oneOf - Preselection for Primitives (Issue 1273 )",
        dataJson = """
        {
          "quantity": 5
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "quantity": {
              "oneOf": [
                {
                  "type": "string"
                },
                {
                  "type": "number"
                }
              ]
            }
          }
        }
        """.trimIndent(),
    )
}
