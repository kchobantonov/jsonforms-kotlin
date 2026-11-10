// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamMixedExample: ExampleDescription by lazy {
    upstreamExample(
        name = "mixed",
        label = "Mixed",
        schemaJson = """
        {
          "${'$'}schema": "http://json-schema.org/draft-07/schema#",
          "type": [
            "string",
            "boolean",
            "integer",
            "null"
          ]
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "Control",
          "scope": "#/"
        }
        """.trimIndent(),
    )
}
