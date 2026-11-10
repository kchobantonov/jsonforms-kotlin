// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamConditionalSchemaCompositionsExample: ExampleDescription by lazy {
    upstreamExample(
        name = "conditional-schema-compositions",
        label = "Conditional Schema Compositions",
        dataJson = """
        {}
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "name": {
              "type": "string",
              "minLength": 1,
              "description": "The task's name"
            },
            "recurrence": {
              "type": "string",
              "enum": [
                "Never",
                "Daily",
                "Weekly",
                "Monthly"
              ]
            }
          },
          "anyOf": [
            {
              "if": {
                "properties": {
                  "recurrence": {
                    "const": "Never"
                  }
                }
              },
              "then": {
                "properties": {
                  "lastname": {
                    "type": "string"
                  },
                  "age": {
                    "type": "number"
                  }
                }
              }
            }
          ]
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "HorizontalLayout",
          "elements": [
            {
              "type": "VerticalLayout",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/name"
                },
                {
                  "type": "Control",
                  "scope": "#/properties/recurrence"
                },
                {
                  "type": "Control",
                  "scope": "#/anyOf/0/then/properties/lastname",
                  "rule": {
                    "effect": "SHOW",
                    "condition": {
                      "scope": "#/properties/recurrence",
                      "schema": {
                        "const": "Never"
                      }
                    }
                  }
                },
                {
                  "type": "Control",
                  "scope": "#/properties/age",
                  "rule": {
                    "effect": "SHOW",
                    "condition": {
                      "scope": "#/properties/recurrence",
                      "schema": {
                        "const": "Never"
                      }
                    }
                  }
                }
              ]
            }
          ]
        }
        """.trimIndent(),
    )
}
