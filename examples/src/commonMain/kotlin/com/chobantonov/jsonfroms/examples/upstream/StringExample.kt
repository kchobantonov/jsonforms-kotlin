// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamStringExample: ExampleDescription by lazy {
    upstreamExample(
        name = "string",
        label = "String",
        dataJson = """
        "This is a test string"
        """.trimIndent(),
        schemaJson = """
        {
          "type": "string",
          "title": "String",
          "description": "The form output will be a string"
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
