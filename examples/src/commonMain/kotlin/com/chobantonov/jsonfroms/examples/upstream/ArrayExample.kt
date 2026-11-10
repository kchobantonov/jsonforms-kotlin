// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
// Interactive actions: Enable Sorting, Disable Sorting, Toggle readonly
package com.chobantonov.jsonfroms.examples

internal val upstreamArrayExample: ExampleDescription by lazy {
    upstreamExample(
        name = "array",
        label = "Array",
        dataJson = """
        {
          "comments": [
            {
              "date": "2001-09-11",
              "message": "This is an example message"
            },
            {
              "date": "2026-11-10",
              "message": "Get ready for booohay",
              "oneOfEnum": "test"
            }
          ]
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "comments": {
              "type": "array",
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
                  },
                  "enum": {
                    "type": "string",
                    "const": "foo"
                  },
                  "oneOfEnum": {
                    "type": "string",
                    "oneOf": [
                      {
                        "const": "foo"
                      },
                      {
                        "const": "bar"
                      }
                    ]
                  }
                }
              }
            },
            "foo": {
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
              "scope": "#/properties/foo"
            },
            {
              "type": "Control",
              "scope": "#/properties/comments",
              "options": {
                "showSortButtons": false
              }
            }
          ]
        }
        """.trimIndent(),
        configJson = """
        {
          "withSort": {
            "type": "VerticalLayout",
            "elements": [
              {
                "type": "Control",
                "scope": "#/properties/foo"
              },
              {
                "type": "Control",
                "scope": "#/properties/comments",
                "options": {
                  "showSortButtons": false
                }
              }
            ]
          }
        }
        """.trimIndent(),
    )
}
