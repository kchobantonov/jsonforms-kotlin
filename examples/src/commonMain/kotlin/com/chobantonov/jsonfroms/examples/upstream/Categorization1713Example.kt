// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamCategorization1713Example: ExampleDescription by lazy {
    upstreamExample(
        name = "categorization_1713",
        label = "Categorization - Issue 1713",
        dataJson = """
        {
          "provideAddress": true,
          "vegetarian": false
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "experiments": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "ID": {
                    "type": "string"
                  }
                },
                "additionalProperties": false,
                "additionalItems": false
              }
            }
          },
          "required": [
            "experiments"
          ]
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "Categorization",
          "elements": [
            {
              "type": "Category",
              "label": "Experiments",
              "elements": [
                {
                  "type": "ListWithDetail",
                  "scope": "#/properties/experiments",
                  "options": {
                    "labelRef": "#/items/properties/ID",
                    "detail": {
                      "type": "VerticalLayout",
                      "elements": [
                        {
                          "type": "HorizontalLayout",
                          "elements": [
                            {
                              "type": "Control",
                              "scope": "#/properties/ID"
                            }
                          ]
                        },
                        {
                          "type": "Categorization",
                          "elements": [
                            {
                              "type": "Category",
                              "label": "Sequential",
                              "elements": [
                                {
                                  "type": "VerticalLayout",
                                  "elements": [
                                    {
                                      "type": "Control",
                                      "scope": "#/properties/ID"
                                    }
                                  ]
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  }
                }
              ]
            }
          ]
        }
        """.trimIndent(),
    )
}
