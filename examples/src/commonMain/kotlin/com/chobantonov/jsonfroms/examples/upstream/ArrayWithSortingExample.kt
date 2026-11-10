// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
// Interactive actions: Enable Sorting, Disable Sorting
package com.chobantonov.jsonfroms.examples

internal val upstreamArrayWithSortingExample: ExampleDescription by lazy {
    upstreamExample(
        name = "array-with-sorting",
        label = "Array with sorting",
        dataJson = """
        {
          "comments": [
            {
              "date": "2001-09-11",
              "message": "This is an example message With sorting"
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
            "comments": {
              "type": "array",
              "minItems": 2,
              "maxItems": 8,
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
                "showSortButtons": true,
                "restrict": true
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
                  "showSortButtons": true,
                  "restrict": true
                }
              }
            ]
          }
        }
        """.trimIndent(),
    )
}
