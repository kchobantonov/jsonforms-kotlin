// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamDefaultExample: ExampleDescription by lazy {
    upstreamExample(
        name = "default",
        label = "Default",
        dataJson = """
        {
          "name": "Send email to Adrian",
          "name_noDefault": "Send email to Adrian",
          "description": "Confirm if you have passed the subject\nHereby ...",
          "done": true,
          "rating": 1,
          "cost": 3.14,
          "dueDate": "2019-05-01"
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "name": {
              "type": "string",
              "default": "foo"
            },
            "name_noDefault": {
              "type": "string"
            },
            "description": {
              "type": "string",
              "default": "bar"
            },
            "done": {
              "type": "boolean",
              "default": false
            },
            "rating": {
              "type": "integer",
              "default": 5
            },
            "cost": {
              "type": "number",
              "default": 5.5
            },
            "dueDate": {
              "type": "string",
              "format": "date",
              "default": "2019-04-01"
            }
          },
          "required": [
            "name",
            "name_noDefault"
          ]
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "VerticalLayout",
          "elements": [
            {
              "type": "Control",
              "scope": "#/properties/name"
            },
            {
              "type": "Control",
              "scope": "#/properties/name_noDefault"
            },
            {
              "type": "Control",
              "label": false,
              "scope": "#/properties/done"
            },
            {
              "type": "Control",
              "scope": "#/properties/description",
              "options": {
                "multi": true
              }
            },
            {
              "type": "Control",
              "scope": "#/properties/rating"
            },
            {
              "type": "Control",
              "scope": "#/properties/cost"
            },
            {
              "type": "Control",
              "scope": "#/properties/dueDate"
            }
          ]
        }
        """.trimIndent(),
    )
}
