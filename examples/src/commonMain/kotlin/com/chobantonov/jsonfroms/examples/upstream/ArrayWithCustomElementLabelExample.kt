// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamArrayWithCustomElementLabelExample: ExampleDescription by lazy {
    upstreamExample(
        name = "array-with-custom-element-label",
        label = "Array with custom element label",
        dataJson = """
        {
          "comments": [
            {
              "message1": "This is an example message",
              "message2": "This is an example message 2"
            },
            {
              "message1": "Get ready for booohay 1",
              "message2": "Get ready for booohay 2"
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
              "title": "Messages",
              "items": {
                "type": "object",
                "properties": {
                  "message1": {
                    "type": "string"
                  },
                  "message2": {
                    "type": "string"
                  }
                }
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
              "scope": "#/properties/comments",
              "options": {
                "elementLabelProp": "message2",
                "detail": {
                  "type": "VerticalLayout",
                  "elements": [
                    {
                      "type": "Control",
                      "scope": "#/properties/message1"
                    },
                    {
                      "type": "Control",
                      "scope": "#/properties/message2"
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
