// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamAnyOfSimpleExample: ExampleDescription by lazy {
    upstreamExample(
        name = "anyOf_simple",
        label = "AnyOf Simple",
        dataJson = """
        {
          "foo": "foo"
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "foo": {
              "anyOf": [
                {
                  "type": "string"
                },
                {
                  "enum": [
                    "foo",
                    "bar"
                  ]
                }
              ]
            }
          }
        }
        """.trimIndent(),
    )
}
