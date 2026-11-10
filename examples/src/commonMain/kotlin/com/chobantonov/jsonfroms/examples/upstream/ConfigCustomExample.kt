// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamConfigCustomExample: ExampleDescription by lazy {
    upstreamExample(
        name = "configCustom",
        label = "Configuration (Custom)",
        dataJson = """
        {
          "postalCode": "12345"
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "postalCode": {
              "type": "string",
              "description": "A Postal Code",
              "maxLength": 5
            },
            "recurrenceInterval": {
              "type": "integer",
              "description": "A recurrence interval"
            }
          },
          "required": [
            "postalCode"
          ]
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
                  "scope": "#/properties/postalCode",
                  "label": "Postal Code"
                },
                {
                  "type": "Control",
                  "scope": "#/properties/recurrenceInterval",
                  "label": "Recurrence Interval"
                }
              ]
            }
          ]
        }
        """.trimIndent(),
        configJson = """
        {
          "restrict": true,
          "trim": true,
          "showUnfocusedDescription": true,
          "hideRequiredAsterisk": true
        }
        """.trimIndent(),
    )
}
