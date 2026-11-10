// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamOneOf1265SimpleExample: ExampleDescription by lazy {
    upstreamExample(
        name = "oneOf_1265_simple",
        label = "oneOf - Validation for Primitives (Issue 1265)",
        dataJson = """
        {
          "coloursOrNumbers": "Foo"
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
          "definitions": {
            "colours": {
              "title": "Colours",
              "type": "string",
              "enum": [
                "Red",
                "Green",
                "Blue"
              ]
            },
            "numbers": {
              "title": "Numbers",
              "type": "string",
              "enum": [
                "One",
                "Two",
                "Three"
              ]
            },
            "shapes": {
              "title": "Shapes",
              "type": "string",
              "enum": [
                "Circle",
                "Triangle",
                "Square"
              ]
            }
          }
        }
        """.trimIndent(),
    )
}
