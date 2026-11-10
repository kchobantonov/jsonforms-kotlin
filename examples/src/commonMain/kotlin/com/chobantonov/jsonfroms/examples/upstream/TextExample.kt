// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamTextExample: ExampleDescription by lazy {
    upstreamExample(
        name = "text",
        label = "Text Control Options",
        dataJson = """
        {
          "zipCode": "12345",
          "zipCodeWithoutTrim": "12345678",
          "zipCodeWithoutRestrict": "12345678"
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "zipCode": {
              "type": "string",
              "maxLength": 5
            },
            "zipCodeWithoutTrim": {
              "type": "string",
              "maxLength": 5
            },
            "zipCodeWithoutRestrict": {
              "type": "string",
              "maxLength": 5
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
                  "scope": "#/properties/zipCode",
                  "label": "ZIP Code (with trim and restrict options)",
                  "options": {
                    "trim": true,
                    "restrict": true
                  }
                },
                {
                  "type": "Control",
                  "scope": "#/properties/zipCodeWithoutTrim",
                  "label": "ZIP Code (without trimming)",
                  "options": {
                    "trim": false,
                    "restrict": true
                  }
                },
                {
                  "type": "Control",
                  "scope": "#/properties/zipCodeWithoutRestrict",
                  "label": "ZIP Code (without restricting)",
                  "options": {
                    "trim": true,
                    "restrict": false
                  }
                }
              ]
            }
          ]
        }
        """.trimIndent(),
    )
}
