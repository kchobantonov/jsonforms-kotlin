// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamArrayWithDetailAndRuleExample: ExampleDescription by lazy {
    upstreamExample(
        name = "array-with-detail-and-rule",
        label = "Array with detail and rule",
        dataJson = """
        {
          "occupation": "developer",
          "comments": [
            {
              "date": "2001-09-11",
              "message": "This is an example message"
            },
            {
              "date": "2026-11-10",
              "message": "Get ready for booohay"
            }
          ]
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "name": {
              "type": "string",
              "minLength": 3,
              "description": "Please enter your name"
            },
            "vegetarian": {
              "type": "boolean"
            },
            "birthDate": {
              "type": "string",
              "format": "date",
              "description": "Please enter your birth date."
            },
            "nationality": {
              "type": "string",
              "enum": [
                "DE",
                "IT",
                "JP",
                "US",
                "RU",
                "Other"
              ]
            },
            "occupation": {
              "type": "string"
            },
            "enableArray": {
              "type": "boolean"
            },
            "comments": {
              "type": "array",
              "title": "Messages",
              "items": {
                "type": "object",
                "properties": {
                  "date": {
                    "type": "string",
                    "format": "date"
                  },
                  "message": {
                    "type": "string",
                    "maxLength": 5
                  }
                }
              }
            }
          },
          "required": [
            "occupation",
            "nationality"
          ]
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "VerticalLayout",
          "elements": [
            {
              "type": "Control",
              "scope": "#/properties/occupation"
            },
            {
              "type": "Control",
              "scope": "#/properties/enableArray"
            },
            {
              "type": "Control",
              "scope": "#/properties/comments",
              "rule": {
                "effect": "SHOW",
                "condition": {
                  "type": "OR",
                  "conditions": [
                    {
                      "schema": {
                        "const": "developer"
                      },
                      "scope": "#/properties/occupation"
                    },
                    {
                      "schema": {
                        "const": true
                      },
                      "scope": "/properties/enableArray"
                    }
                  ]
                }
              },
              "options": {
                "detail": {
                  "type": "VerticalLayout",
                  "elements": [
                    {
                      "type": "Control",
                      "scope": "#/properties/message"
                    },
                    {
                      "type": "Control",
                      "scope": "#/properties/date"
                    }
                  ]
                }
              }
            }
          ]
        }
        """.trimIndent(),
    )
}
