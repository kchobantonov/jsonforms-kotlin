// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamCategorization2343Example: ExampleDescription by lazy {
    upstreamExample(
        name = "categorization_2343",
        label = "Categorization - Issue 2343",
        dataJson = """
        {}
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "parc": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "id": {
                    "type": "string"
                  }
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
              "type": "Categorization",
              "elements": [
                {
                  "type": "Category",
                  "label": "A1",
                  "elements": [
                    {
                      "type": "Control",
                      "scope": "#/properties/parc",
                      "options": {
                        "detail": {
                          "type": "VerticalLayout",
                          "elements": [
                            {
                              "type": "HorizontalLayout",
                              "elements": [
                                {
                                  "type": "Control",
                                  "scope": "#/properties/id"
                                }
                              ]
                            }
                          ]
                        }
                      }
                    }
                  ]
                },
                {
                  "type": "Category",
                  "label": "A2",
                  "elements": [
                    {
                      "type": "Control",
                      "scope": "#/properties/parc",
                      "options": {
                        "detail": {
                          "type": "VerticalLayout",
                          "elements": [
                            {
                              "type": "HorizontalLayout",
                              "elements": [
                                {
                                  "type": "Control",
                                  "scope": "#/properties/id"
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
          ]
        }
        """.trimIndent(),
    )
}
