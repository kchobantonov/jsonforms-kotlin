// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamStringArrayExample: ExampleDescription by lazy {
    upstreamExample(
        name = "stringArray",
        label = "Array of Strings",
        dataJson = """
        {
          "comments": [
            "one string",
            "two strings"
          ]
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "comments": {
              "description": "Description for array of String Type",
              "type": "array",
              "items": {
                "type": "string",
                "maxLength": 5
              }
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
              "scope": "#/properties/comments"
            }
          ]
        }
        """.trimIndent(),
    )
}
