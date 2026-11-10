// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamOneOf1265ObjectExample: ExampleDescription by lazy {
    upstreamExample(
        name = "oneOf_1265_object",
        label = "oneOf - Validation for Objects (Issue 1265)",
        dataJson = """
        {
          "coloursOrNumbers": {
            "colour": "Foo"
          }
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "coloursOrNumbers": {
              "oneOf": [
                {
                  "${'$'}ref": "#/definitions/colours"
                },
                {
                  "${'$'}ref": "#/definitions/numbers"
                },
                {
                  "${'$'}ref": "#/definitions/shapes"
                }
              ]
            }
          },
          "additionalProperties": false,
          "definitions": {
            "colours": {
              "title": "Colours",
              "type": "object",
              "properties": {
                "colour": {
                  "title": "Type",
                  "type": "string",
                  "enum": [
                    "Red",
                    "Green",
                    "Blue"
                  ]
                }
              },
              "additionalProperties": false
            },
            "numbers": {
              "title": "Numbers",
              "type": "object",
              "properties": {
                "number": {
                  "title": "Type",
                  "type": "string",
                  "enum": [
                    "One",
                    "Two",
                    "Three"
                  ]
                }
              },
              "additionalProperties": false
            },
            "shapes": {
              "title": "Shapes",
              "type": "object",
              "properties": {
                "shape": {
                  "title": "Type",
                  "type": "string",
                  "enum": [
                    "Circle",
                    "Triangle",
                    "Square"
                  ]
                }
              },
              "additionalProperties": false
            }
          }
        }
        """.trimIndent(),
    )
}
