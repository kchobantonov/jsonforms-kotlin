// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamNumbersExample: ExampleDescription by lazy {
    upstreamExample(
        name = "numbers",
        label = "Numbers",
        dataJson = """
        {}
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "price": {
              "type": "number",
              "maximum": 100,
              "minimum": 1,
              "default": 50
            },
            "age": {
              "type": "integer"
            },
            "height": {
              "type": "number"
            }
          }
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "VerticalLayout",
          "elements": [
            {
              "type": "HorizontalLayout",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/price",
                  "label": {
                    "text": "Price"
                  }
                },
                {
                  "type": "Control",
                  "scope": "#/properties/age"
                },
                {
                  "type": "Control",
                  "scope": "#/properties/height",
                  "options": {
                    "step": 0.01
                  }
                },
                {
                  "type": "Control",
                  "scope": "#/properties/price",
                  "label": {
                    "text": "Price with Slider"
                  },
                  "options": {
                    "slider": true
                  }
                }
              ]
            }
          ]
        }
        """.trimIndent(),
    )
}
