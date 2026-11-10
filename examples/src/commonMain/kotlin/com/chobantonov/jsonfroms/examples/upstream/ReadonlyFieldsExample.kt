// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamReadonlyFieldsExample: ExampleDescription by lazy {
    upstreamExample(
        name = "Readonly Fields",
        label = "Readonly examples",
        dataJson = """
        {
          "readonly": "readonly by schema",
          "readonlyByUISchema": "readonly by ui schema",
          "notReadonly": "normal field"
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "readonly": {
              "type": "string",
              "readOnly": true
            },
            "readonlyByUISchema": {
              "type": "string"
            },
            "notReadonly": {
              "type": "string"
            }
          }
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "VerticalLayout",
          "elements": [
            {
              "type": "Control",
              "scope": "#/properties/readonly",
              "label": "A readonly field"
            },
            {
              "type": "Control",
              "scope": "#/properties/readonlyByUISchema",
              "label": "A readonly field by ui schema",
              "options": {
                "readonly": true
              }
            },
            {
              "type": "Control",
              "scope": "#/properties/notReadonly",
              "label": "A normal field"
            }
          ]
        }
        """.trimIndent(),
    )
}
