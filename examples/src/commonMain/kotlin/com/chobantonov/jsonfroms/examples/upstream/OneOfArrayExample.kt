// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamOneOfArrayExample: ExampleDescription by lazy {
    upstreamExample(
        name = "oneOfArray",
        label = "oneOf - Inside array items",
        dataJson = """
        {
          "name": "test",
          "addressOrUsers": [
            {
              "street_address": "1600 Pennsylvania Avenue NW",
              "city": "Washington",
              "state": "DC"
            },
            {
              "name": "User",
              "mail": "user@user.user"
            }
          ]
        }
        """.trimIndent(),
        schemaJson = """
        {
          "${'$'}schema": "http://json-schema.org/draft-07/schema#",
          "definitions": {
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
          },
          "type": "object",
          "properties": {
            "name": {
              "type": "string"
            },
            "addressOrUsers": {
              "type": "array",
              "items": {
                "oneOf": [
                  {
                    "${'$'}ref": "#/definitions/address"
                  },
                  {
                    "${'$'}ref": "#/definitions/user"
                  }
                ]
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
              "type": "Control",
              "scope": "#/properties/addressOrUsers"
            }
          ]
        }
        """.trimIndent(),
    )
}
