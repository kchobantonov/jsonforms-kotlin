// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamAnyOfExample: ExampleDescription by lazy {
    upstreamExample(
        name = "anyOf",
        label = "anyOf",
        dataJson = """
        {
          "addressOrUser": {
            "street_address": "1600 Pennsylvania Avenue NW",
            "city": "Washington",
            "state": "DC"
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
            },
            "users": {
              "type": "array",
              "items": {
                "${'$'}ref": "#/definitions/user"
              }
            },
            "addresses": {
              "type": "array",
              "items": {
                "${'$'}ref": "#/definitions/address"
              }
            }
          },
          "type": "object",
          "properties": {
            "addressOrUser": {
              "anyOf": [
                {
                  "${'$'}ref": "#/definitions/address"
                },
                {
                  "${'$'}ref": "#/definitions/user"
                }
              ]
            },
            "addressesOrUsers": {
              "anyOf": [
                {
                  "${'$'}ref": "#/definitions/addresses"
                },
                {
                  "${'$'}ref": "#/definitions/users"
                }
              ]
            },
            "addressesOrUsersAnyOfItems": {
              "type": "array",
              "items": {
                "anyOf": [
                  {
                    "${'$'}ref": "#/definitions/addresses"
                  },
                  {
                    "${'$'}ref": "#/definitions/users"
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
              "scope": "#/properties/addressOrUser"
            },
            {
              "type": "Control",
              "scope": "#/properties/addressesOrUsers",
              "label": "Addresses or Users (AnyOf Schema)"
            },
            {
              "type": "Control",
              "scope": "#/properties/addressesOrUsersAnyOfItems",
              "label": "Addresses or Users (AnyOf Array Items)"
            }
          ]
        }
        """.trimIndent(),
    )
}
