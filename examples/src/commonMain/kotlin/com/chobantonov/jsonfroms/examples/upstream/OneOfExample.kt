// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamOneOfExample: ExampleDescription by lazy {
    upstreamExample(
        name = "oneOf",
        label = "oneOf",
        dataJson = """
        {
          "name": "test",
          "addressOrUser": {
            "name": "User",
            "mail": "mail@example.com"
          }
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
              ],
              "additionalProperties": false
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
              ],
              "additionalProperties": false
            }
          },
          "type": "object",
          "properties": {
            "name": {
              "type": "string"
            },
            "addressOrUser": {
              "oneOf": [
                {
                  "${'$'}ref": "#/definitions/address"
                },
                {
                  "${'$'}ref": "#/definitions/user"
                }
              ]
            }
          },
          "required": [
            "name"
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
              "scope": "#/properties/addressOrUser"
            }
          ]
        }
        """.trimIndent(),
    )
}
