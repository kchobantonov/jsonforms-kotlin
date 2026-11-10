// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamNestedCategorizationExample: ExampleDescription by lazy {
    upstreamExample(
        name = "nestedCategorization",
        label = "Nested Categorization",
        dataJson = """
        {}
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "cat1": {
              "type": "object",
              "properties": {
                "subcat11": {
                  "type": "string"
                }
              }
            },
            "cat2": {
              "type": "object",
              "properties": {
                "subcat21": {
                  "type": "string"
                },
                "subcat22": {
                  "type": "string"
                }
              }
            },
            "cat3": {
              "type": "object",
              "properties": {
                "subcat31": {
                  "type": "string"
                },
                "subcat32": {
                  "type": "string"
                },
                "subcat33": {
                  "type": "string"
                }
              }
            }
          }
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "Categorization",
          "elements": [
            {
              "type": "Category",
              "label": "Cat1",
              "elements": [
                {
                  "type": "Categorization",
                  "elements": [
                    {
                      "type": "Category",
                      "label": "SubCat1-1",
                      "elements": [
                        {
                          "type": "Control",
                          "scope": "#/properties/cat1/properties/subcat11"
                        }
                      ]
                    }
                  ]
                }
              ]
            },
            {
              "type": "Category",
              "label": "Cat2",
              "elements": [
                {
                  "type": "Categorization",
                  "elements": [
                    {
                      "type": "Category",
                      "label": "SubCat2-1",
                      "elements": [
                        {
                          "type": "Control",
                          "scope": "#/properties/cat2/properties/subcat21"
                        }
                      ]
                    },
                    {
                      "type": "Category",
                      "label": "SubCat2-2",
                      "elements": [
                        {
                          "type": "Control",
                          "scope": "#/properties/cat2/properties/subcat22"
                        }
                      ]
                    }
                  ]
                }
              ]
            },
            {
              "type": "Category",
              "label": "Cat3",
              "elements": [
                {
                  "type": "Categorization",
                  "elements": [
                    {
                      "type": "Category",
                      "label": "SubCat3-1",
                      "elements": [
                        {
                          "type": "Control",
                          "scope": "#/properties/cat3/properties/subcat31"
                        }
                      ]
                    },
                    {
                      "type": "Category",
                      "label": "SubCat3-2",
                      "elements": [
                        {
                          "type": "Control",
                          "scope": "#/properties/cat3/properties/subcat32"
                        }
                      ]
                    },
                    {
                      "type": "Category",
                      "label": "SubCat3-3",
                      "elements": [
                        {
                          "type": "Control",
                          "scope": "#/properties/cat3/properties/subcat33"
                        }
                      ]
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
