// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
// Interactive actions: Register NestedArray UISchema, Unregister NestedArray UISchema
package com.chobantonov.jsonfroms.examples

internal val upstreamNestedArrayExample: ExampleDescription by lazy {
    upstreamExample(
        name = "nestedArray",
        label = "Nested Array",
        dataJson = """
        {
          "exampleArray": [
            {
              "choices": [
                "This",
                "is",
                "an",
                "example"
              ],
              "name": "Hi there"
            }
          ]
        }
        """.trimIndent(),
        schemaJson = """
        {
          "definitions": {
            "choicesContainer": {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "choices": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                }
              }
            }
          },
          "type": "object",
          "properties": {
            "exampleArray": {
              "type": "array",
              "items": {
                "${'$'}ref": "#/definitions/choicesContainer"
              }
            }
          }
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "HorizontalLayout",
          "elements": [
            {
              "type": "Control",
              "label": {
                "text": "Example Array",
                "show": true
              },
              "scope": "#/properties/exampleArray"
            }
          ]
        }
        """.trimIndent(),
    )
}
