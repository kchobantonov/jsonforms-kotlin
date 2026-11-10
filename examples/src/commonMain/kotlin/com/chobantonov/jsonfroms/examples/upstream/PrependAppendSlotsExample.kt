// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamPrependAppendSlotsExample: ExampleDescription by lazy {
    upstreamExample(
        name = "prepend-append-slots",
        label = "Prepend/Append Slots (Basic)",
        dataJson = """
        {
          "displayName": "John Doe",
          "password": "",
          "username": "",
          "email": "user@example.com",
          "temperature": 22
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "displayName": {
              "type": "string",
              "description": "Decorative icon"
            },
            "password": {
              "type": "string",
              "description": "Dynamic strength meter"
            },
            "username": {
              "type": "string",
              "maxLength": 20,
              "description": "Availability checker (try \"admin\" or \"user\")"
            },
            "email": {
              "type": "string",
              "format": "email",
              "description": "Copy to clipboard"
            },
            "temperature": {
              "type": "integer",
              "minimum": -50,
              "maximum": 50,
              "description": "Temperature with dynamic icon and unit (°C)"
            }
          }
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "VerticalLayout",
          "elements": [
            {
              "type": "Label",
              "text": "Prepend Slot Only"
            },
            {
              "type": "Control",
              "scope": "#/properties/displayName",
              "options": {
                "clearable": false
              }
            },
            {
              "type": "Control",
              "scope": "#/properties/password",
              "options": {
                "clearable": false
              }
            },
            {
              "type": "Label",
              "text": "Append Slot Only"
            },
            {
              "type": "Control",
              "scope": "#/properties/username",
              "options": {
                "clearable": false
              }
            },
            {
              "type": "Control",
              "scope": "#/properties/email",
              "options": {
                "clearable": false
              }
            },
            {
              "type": "Label",
              "text": "Both Prepend and Append"
            },
            {
              "type": "Control",
              "scope": "#/properties/temperature",
              "options": {
                "clearable": false
              }
            }
          ]
        }
        """.trimIndent(),
    )
}
