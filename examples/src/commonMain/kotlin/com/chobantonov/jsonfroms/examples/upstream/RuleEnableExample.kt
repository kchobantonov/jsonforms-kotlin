// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
// Interactive actions: Enable/Disable top layout, Show/Hide bottom layout
package com.chobantonov.jsonfroms.examples

internal val upstreamRuleEnableExample: ExampleDescription by lazy {
    upstreamExample(
        name = "rule-enable",
        label = "Rule Inheritance",
        dataJson = """
        {
          "toggleTopLayout": true,
          "toggleBottomLayout": true,
          "toggleControl": true
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "toggleTopLayout": {
              "type": "boolean"
            },
            "topString": {
              "type": "string"
            },
            "middleNumber": {
              "type": "number"
            },
            "toggleBottomLayout": {
              "type": "boolean"
            },
            "bottomBoolean": {
              "type": "boolean"
            }
          }
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "VerticalLayout",
          "rule": {
            "effect": "ENABLE",
            "condition": {
              "scope": "#/properties/toggleTopLayout",
              "schema": {
                "const": true
              }
            }
          },
          "elements": [
            {
              "type": "Control",
              "scope": "#/properties/topString"
            },
            {
              "type": "HorizontalLayout",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/middleNumber"
                },
                {
                  "type": "Group",
                  "label": "group",
                  "rule": {
                    "effect": "SHOW",
                    "condition": {
                      "scope": "#/properties/toggleBottomLayout",
                      "schema": {
                        "const": true
                      }
                    }
                  },
                  "elements": [
                    {
                      "type": "Control",
                      "scope": "#/properties/bottomBoolean"
                    }
                  ]
                }
              ]
            }
          ]
        }
        """.trimIndent(),
    )
}
