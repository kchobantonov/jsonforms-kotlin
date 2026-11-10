// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamLayoutGroupExample: ExampleDescription by lazy {
    upstreamExample(
        name = "layout-group",
        label = "Layout Group",
        dataJson = """
        {
          "name": "John Doe",
          "vegetarian": false,
          "birthDate": "1985-06-02",
          "personalData": {
            "age": 34
          },
          "postalCode": "12345"
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
              "format": "date"
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
            "personalData": {
              "type": "object",
              "properties": {
                "age": {
                  "type": "integer",
                  "description": "Please enter your age."
                },
                "height": {
                  "type": "number"
                },
                "drivingSkill": {
                  "type": "number",
                  "maximum": 10,
                  "minimum": 1,
                  "default": 7
                }
              },
              "required": [
                "age",
                "height"
              ]
            },
            "occupation": {
              "type": "string"
            },
            "postalCode": {
              "type": "string",
              "maxLength": 5
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
          "type": "Group",
          "label": "My Group",
          "elements": [
            {
              "type": "Control",
              "label": "Name",
              "scope": "#/properties/name"
            },
            {
              "type": "Control",
              "label": "Birth Date",
              "scope": "#/properties/birthDate"
            }
          ]
        }
        """.trimIndent(),
    )
}
