// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamAnyOfOneOfAllOfResolveExample: ExampleDescription by lazy {
    upstreamExample(
        name = "anyOf-oneOf-allOf-resolve",
        label = "AnyOf OneOf AllOf Resolve",
        dataJson = """
        {}
        """.trimIndent(),
        schemaJson = """
        {
          "${'$'}defs": {
            "Base": {
              "type": "object",
              "properties": {
                "width": {
                  "type": "integer"
                }
              }
            },
            "Child": {
              "type": "object",
              "allOf": [
                {
                  "${'$'}ref": "#/${'$'}defs/Base"
                },
                {
                  "properties": {
                    "geometry": {
                      "type": "string"
                    }
                  }
                }
              ]
            }
          },
          "type": "object",
          "properties": {
            "element": {
              "${'$'}ref": "#/${'$'}defs/Child"
            }
          }
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "VerticalLayout",
          "elements": [
            {
              "type": "Label",
              "text": "AllOfRenderer"
            },
            {
              "type": "Control",
              "scope": "#/properties/element"
            },
            {
              "type": "Label",
              "text": "Manual controls"
            },
            {
              "type": "Control",
              "scope": "#/properties/element/properties/width"
            },
            {
              "type": "Control",
              "scope": "#/properties/element/properties/geometry"
            }
          ]
        }
        """.trimIndent(),
    )
}
