// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamOnChangeExample: ExampleDescription by lazy {
    upstreamExample(
        name = "onChange",
        label = "On Change Listener",
        dataJson = """
        {}
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "name": {
              "type": "string",
              "minLength": 1
            },
            "description": {
              "type": "string",
              "minLength": 1
            }
          },
          "required": [
            "name",
            "description"
          ]
        }
        """.trimIndent(),
    )
}
