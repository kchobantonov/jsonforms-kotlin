// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamLoginExample: ExampleDescription by lazy {
    upstreamExample(
        name = "login",
        label = "Login Form",
        dataJson = """
        {
          "username": "john.doe@email.com"
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "username": {
              "type": "string",
              "description": "Login Name"
            },
            "password": {
              "type": "string",
              "format": "password",
              "description": "Login password"
            }
          },
          "required": [
            "username",
            "password"
          ]
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "VerticalLayout",
          "elements": [
            {
              "type": "Label",
              "text": "Login Information"
            },
            {
              "type": "HorizontalLayout",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/username"
                },
                {
                  "type": "Control",
                  "scope": "#/properties/password"
                }
              ]
            }
          ]
        }
        """.trimIndent(),
    )
}
