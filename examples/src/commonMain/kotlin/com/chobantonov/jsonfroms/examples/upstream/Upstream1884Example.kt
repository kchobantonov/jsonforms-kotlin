// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstream1884Example: ExampleDescription by lazy {
    upstreamExample(
        name = "1884",
        label = "Issue 1884 - Nested enable/disable",
        dataJson = """
        {
          "firstName": "Max",
          "lastName": "Power",
          "committer": false
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "required": [
            "age"
          ],
          "properties": {
            "firstName": {
              "type": "string",
              "minLength": 2,
              "maxLength": 20
            },
            "lastName": {
              "type": "string",
              "minLength": 5,
              "maxLength": 15
            },
            "age": {
              "type": "integer",
              "minimum": 18,
              "maximum": 100
            },
            "gender": {
              "type": "string",
              "enum": [
                "Male",
                "Female",
                "Undisclosed"
              ]
            },
            "height": {
              "type": "number"
            },
            "dateOfBirth": {
              "type": "string",
              "format": "date"
            },
            "rating": {
              "type": "integer"
            },
            "committer": {
              "type": "boolean"
            },
            "address": {
              "type": "object",
              "properties": {
                "street": {
                  "type": "string"
                },
                "streetnumber": {
                  "type": "string"
                },
                "postalCode": {
                  "type": "string"
                },
                "city": {
                  "type": "string"
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
              "type": "Label",
              "text": "Toggle the committer boolean to enable/disable the address block."
            },
            {
              "type": "HorizontalLayout",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/firstName"
                },
                {
                  "type": "Control",
                  "scope": "#/properties/lastName"
                }
              ]
            },
            {
              "type": "HorizontalLayout",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/age"
                },
                {
                  "type": "Control",
                  "scope": "#/properties/dateOfBirth"
                }
              ]
            },
            {
              "type": "HorizontalLayout",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/height"
                },
                {
                  "type": "Control",
                  "scope": "#/properties/gender"
                },
                {
                  "type": "Control",
                  "scope": "#/properties/committer"
                }
              ]
            },
            {
              "type": "Group",
              "label": "Address for Shipping T-Shirt",
              "elements": [
                {
                  "type": "HorizontalLayout",
                  "elements": [
                    {
                      "type": "Control",
                      "scope": "#/properties/address/properties/street"
                    },
                    {
                      "type": "Control",
                      "scope": "#/properties/address/properties/streetnumber"
                    }
                  ]
                },
                {
                  "type": "HorizontalLayout",
                  "elements": [
                    {
                      "type": "Control",
                      "scope": "#/properties/address/properties/postalCode"
                    },
                    {
                      "type": "Control",
                      "scope": "#/properties/address/properties/city"
                    }
                  ]
                }
              ],
              "rule": {
                "effect": "ENABLE",
                "condition": {
                  "scope": "#/properties/committer",
                  "schema": {
                    "const": true
                  }
                }
              }
            }
          ]
        }
        """.trimIndent(),
    )
}
