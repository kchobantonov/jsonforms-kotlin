// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamListWithDetailRegisteredExample: ExampleDescription by lazy {
    upstreamExample(
        name = "list-with-detail-registered",
        label = "List With Detail (Registered Detail UISchema)",
        dataJson = """
        {
          "warehouseitems": [
            {
              "name": "Fantasy Book",
              "buyer": {
                "email": "buyerA@info.org",
                "age": 18
              },
              "status": "warehouse"
            },
            {
              "name": "Boardgame",
              "buyer": {
                "email": "buyerB@info.org",
                "age": 45
              },
              "status": "shipping"
            },
            {
              "name": "Energy Drink",
              "buyer": {
                "email": "buyerC@info.org",
                "age": 90
              },
              "status": "delivered"
            }
          ]
        }
        """.trimIndent(),
        schemaJson = """
        {
          "definitions": {
            "warehouseitem": {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "buyer": {
                  "type": "object",
                  "properties": {
                    "email": {
                      "type": "string",
                      "format": "email"
                    },
                    "age": {
                      "type": "number"
                    }
                  }
                },
                "status": {
                  "type": "string",
                  "enum": [
                    "warehouse",
                    "shipping",
                    "delivered"
                  ]
                }
              },
              "required": [
                "name"
              ]
            }
          },
          "type": "object",
          "properties": {
            "warehouseitems": {
              "type": "array",
              "items": {
                "${'$'}ref": "#/definitions/warehouseitem"
              }
            }
          }
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "ListWithDetail",
          "scope": "#/properties/warehouseitems",
          "options": {
            "labelRef": "#/items/properties/name"
          }
        }
        """.trimIndent(),
    )
}
