// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamValidationNestedSameNameExample: ExampleDescription by lazy {
    upstreamExample(
        name = "validation-nested-same-name",
        label = "Validation - 3x nested properties with same name",
        dataJson = """
        {
          "name": {
            "name": {}
          }
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "name": {
              "type": "object",
              "properties": {
                "name": {
                  "type": "object",
                  "properties": {
                    "name": {
                      "type": "string"
                    }
                  },
                  "required": [
                    "name"
                  ]
                }
              },
              "required": [
                "name"
              ]
            }
          }
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "Control",
          "scope": "#/properties/name/properties/name/properties/name",
          "label": "Name"
        }
        """.trimIndent(),
    )
}
