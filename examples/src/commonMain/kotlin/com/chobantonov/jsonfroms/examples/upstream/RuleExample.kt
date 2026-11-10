// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamRuleExample: ExampleDescription by lazy {
    upstreamExample(
        name = "rule",
        label = "Rule",
        dataJson = """
        {
          "name": "John Doe",
          "dead": false,
          "vegetables": false
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "name": {
              "type": "string"
            },
            "dead": {
              "type": "boolean"
            },
            "kindOfDead": {
              "type": "string",
              "enum": [
                "Zombie",
                "Vampire",
                "Ghoul"
              ]
            },
            "vegetables": {
              "type": "boolean"
            },
            "kindOfVegetables": {
              "type": "string",
              "enum": [
                "All",
                "Some",
                "Only potatoes"
              ]
            },
            "vitaminDeficiency": {
              "type": "string",
              "enum": [
                "None",
                "Vitamin A",
                "Vitamin B",
                "Vitamin C"
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
              "label": "Name",
              "scope": "#/properties/name"
            },
            {
              "type": "Group",
              "elements": [
                {
                  "type": "Control",
                  "label": "Is Dead?",
                  "scope": "#/properties/dead"
                },
                {
                  "type": "Control",
                  "label": "Kind of dead",
                  "scope": "#/properties/kindOfDead",
                  "rule": {
                    "effect": "ENABLE",
                    "condition": {
                      "scope": "#/properties/dead",
                      "schema": {
                        "const": true
                      }
                    }
                  }
                }
              ]
            },
            {
              "type": "Group",
              "elements": [
                {
                  "type": "Control",
                  "label": "Eats vegetables?",
                  "scope": "#/properties/vegetables"
                },
                {
                  "type": "Control",
                  "label": "Kind of vegetables",
                  "scope": "#/properties/kindOfVegetables",
                  "rule": {
                    "effect": "HIDE",
                    "condition": {
                      "scope": "#/properties/vegetables",
                      "schema": {
                        "const": false
                      }
                    }
                  }
                },
                {
                  "type": "Control",
                  "label": "Vitamin deficiency?",
                  "scope": "#/properties/vitaminDeficiency",
                  "rule": {
                    "effect": "SHOW",
                    "condition": {
                      "scope": "#"
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
