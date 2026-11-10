// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamJsonEditorExample: ExampleDescription by lazy {
    upstreamExample(
        name = "json-editor",
        label = "JSON Editor",
        schemaJson = """
        {
          "type": [
            "array",
            "boolean",
            "integer",
            "null",
            "number",
            "object",
            "string"
          ],
          "additionalProperties": true,
          "items": {
            "type": [
              "array",
              "boolean",
              "integer",
              "null",
              "number",
              "object",
              "string"
            ]
          }
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
