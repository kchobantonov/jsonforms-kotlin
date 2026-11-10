// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamOneOf1273Example: ExampleDescription by lazy {
    upstreamExample(
        name = "oneOf_1273",
        label = "oneOf - Preselection for Objects (Issue 1273)",
        dataJson = """
        {
          "quantity": {
            "valueLow": 1,
            "valueHigh": 100,
            "unit": "kg"
          }
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "quantity": {
              "oneOf": [
                {
                  "${'$'}ref": "#/definitions/unrangedQuantity"
                },
                {
                  "${'$'}ref": "#/definitions/rangedQuantity"
                }
              ]
            }
          },
          "definitions": {
            "unrangedQuantity": {
              "title": "Value",
              "type": "object",
              "properties": {
                "value": {
                  "type": "number"
                },
                "unit": {
                  "type": "string"
                }
              },
              "required": [
                "value",
                "unit"
              ]
            },
            "rangedQuantity": {
              "title": "Range",
              "type": "object",
              "properties": {
                "valueLow": {
                  "type": "number"
                },
                "valueHigh": {
                  "type": "number",
                  "maximum": 10
                },
                "unit": {
                  "type": "string"
                }
              },
              "required": [
                "valueLow",
                "valueHigh",
                "unit"
              ]
            }
          }
        }
        """.trimIndent(),
    )
}
