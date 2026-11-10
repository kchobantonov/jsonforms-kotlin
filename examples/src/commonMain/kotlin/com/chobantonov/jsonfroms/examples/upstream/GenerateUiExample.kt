// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamGenerateUiExample: ExampleDescription by lazy {
    upstreamExample(
        name = "generate-ui",
        label = "Generate UI Schema",
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
              "format": "date",
              "description": "Please enter your birth date."
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
            }
          }
        }
        """.trimIndent(),
    )
}
