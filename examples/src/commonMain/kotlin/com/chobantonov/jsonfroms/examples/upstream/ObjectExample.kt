// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamObjectExample: ExampleDescription by lazy {
    upstreamExample(
        name = "object",
        label = "Object",
        dataJson = """
        {
          "address": {
            "street_address": "1600 Pennsylvania Avenue NW",
            "city": "Washington",
            "state": "DC"
          }
        }
        """.trimIndent(),
        schemaJson = """
        {
          "${'$'}schema": "http://json-schema.org/draft-07/schema#",
          "type": "object",
          "properties": {
            "address": {
              "type": "object",
              "properties": {
                "street_address": {
                  "type": "string"
                },
                "city": {
                  "type": "string"
                },
                "state": {
                  "type": "string"
                }
              },
              "required": [
                "street_address",
                "city",
                "state"
              ]
            },
            "user": {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "mail": {
                  "type": "string"
                }
              },
              "required": [
                "name",
                "mail"
              ]
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
              "scope": "#/properties/address"
            },
            {
              "type": "Control",
              "scope": "#/properties/user",
              "rule": {
                "effect": "SHOW",
                "condition": {
                  "type": "LEAF",
                  "scope": "#/properties/address/properties/state",
                  "expectedValue": "DC"
                }
              },
              "options": {
                "detail": {
                  "type": "Group",
                  "label": "User Data",
                  "elements": [
                    {
                      "type": "Control",
                      "scope": "#/properties/name"
                    },
                    {
                      "type": "Control",
                      "scope": "#/properties/mail"
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
