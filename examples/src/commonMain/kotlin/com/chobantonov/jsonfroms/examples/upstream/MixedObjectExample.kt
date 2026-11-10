// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamMixedObjectExample: ExampleDescription by lazy {
    upstreamExample(
        name = "mixed-object",
        label = "Mixed Object",
        dataJson = """
        {
          "mixedSimple": "String",
          "nullableString": null,
          "nullableObject": {
            "shape": "circle",
            "size": 5
          }
        }
        """.trimIndent(),
        schemaJson = """
        {
          "${'$'}schema": "http://json-schema.org/draft-07/schema#",
          "title": "Mixed Types Example",
          "type": "object",
          "properties": {
            "mixedSimple": {
              "type": [
                "string",
                "boolean",
                "integer"
              ],
              "description": "This property can be a string, boolean, or integer."
            },
            "nullableString": {
              "type": [
                "string",
                "null"
              ]
            },
            "nullableObject": {
              "type": [
                "object",
                "null"
              ],
              "description": "A nullable object whose properties should still be rendered.",
              "properties": {
                "shape": {
                  "type": "string"
                },
                "size": {
                  "type": "number"
                }
              }
            },
            "mixed": {
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
          },
          "required": [
            "mixedSimple"
          ]
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "VerticalLayout",
          "elements": [
            {
              "type": "Control",
              "scope": "#/properties/mixedSimple"
            },
            {
              "type": "Control",
              "scope": "#/properties/nullableString"
            },
            {
              "type": "Control",
              "scope": "#/properties/nullableObject"
            },
            {
              "type": "Control",
              "scope": "#/properties/mixed"
            }
          ]
        }
        """.trimIndent(),
    )
}
