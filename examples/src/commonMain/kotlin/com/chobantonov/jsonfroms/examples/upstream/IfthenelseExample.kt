// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamIfthenelseExample: ExampleDescription by lazy {
    upstreamExample(
        name = "ifthenelse",
        label = "If Then Else",
        dataJson = """
        {}
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "b": {
              "type": "boolean"
            },
            "c": {
              "type": "string",
              "minLength": 1
            }
          },
          "if": {
            "properties": {
              "b": {
                "enum": [
                  false
                ]
              }
            }
          },
          "then": {
            "required": [
              "c"
            ]
          }
        }
        """.trimIndent(),
    )
}
