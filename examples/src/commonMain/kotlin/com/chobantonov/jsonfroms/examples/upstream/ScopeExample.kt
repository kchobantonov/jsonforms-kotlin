// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamScopeExample: ExampleDescription by lazy {
    upstreamExample(
        name = "scope",
        label = "Scope",
        dataJson = """
        {
          "orderId": "123456",
          "purchaseDate": "1985-06-02",
          "price": 16,
          "shippingAddress": {
            "aptNo": 3,
            "streetNumber": 12
          }
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "orderId": {
              "type": "string"
            },
            "purchaseDate": {
              "type": "string",
              "format": "date"
            },
            "price": {
              "type": "number"
            },
            "shippingAddress": {
              "${'$'}ref": "#/definitions/shippingAddress"
            }
          },
          "definitions": {
            "shippingAddress": {
              "type": "object",
              "properties": {
                "aptNo": {
                  "type": "integer"
                },
                "streetNumber": {
                  "type": "integer"
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
              "type": "HorizontalLayout",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/orderId"
                },
                {
                  "type": "Control",
                  "scope": "#/properties/purchaseDate"
                },
                {
                  "type": "Control",
                  "scope": "#/properties/price"
                }
              ]
            },
            {
              "type": "HorizontalLayout",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/shippingAddress/properties/aptNo"
                },
                {
                  "type": "Control",
                  "scope": "#/properties/shippingAddress/properties/streetNumber"
                }
              ]
            }
          ]
        }
        """.trimIndent(),
    )
}
