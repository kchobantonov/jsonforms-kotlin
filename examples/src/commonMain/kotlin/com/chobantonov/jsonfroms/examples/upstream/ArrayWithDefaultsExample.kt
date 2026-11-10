// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamArrayWithDefaultsExample: ExampleDescription by lazy {
    upstreamExample(
        name = "array-with-defaults",
        label = "Array with defaults",
        dataJson = """
        {}
        """.trimIndent(),
        schemaJson = """
        {
          "definitions": {
            "itemsType": {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string",
                  "default": "foo1"
                },
                "name_noDefault": {
                  "type": "string"
                },
                "description": {
                  "type": "string",
                  "default": "bar"
                },
                "done": {
                  "type": "boolean",
                  "default": false
                },
                "rating": {
                  "type": "integer",
                  "default": 5
                },
                "cost": {
                  "type": "number",
                  "default": 5.5
                },
                "date": {
                  "type": "string",
                  "format": "date",
                  "default": "2026-11-10"
                }
              }
            },
            "stringDef": {
              "type": "string",
              "default": "excellent"
            },
            "numberDef": {
              "type": "number",
              "default": 10
            },
            "intDef": {
              "type": "integer",
              "default": 11
            },
            "boolDef": {
              "type": "boolean",
              "default": true
            },
            "arrayDef": {
              "type": "array",
              "default": [
                "a",
                "b",
                "c"
              ]
            }
          },
          "type": "object",
          "properties": {
            "objectArray": {
              "type": "array",
              "items": {
                "${'$'}ref": "#/definitions/itemsType"
              }
            },
            "stringArray": {
              "type": "array",
              "items": {
                "type": "string",
                "default": "123"
              }
            },
            "objectArrayWithPropertyRefs": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "string1": {
                    "${'$'}ref": "#/definitions/stringDef"
                  },
                  "string2": {
                    "type": "string"
                  },
                  "number": {
                    "${'$'}ref": "#/definitions/numberDef"
                  },
                  "int": {
                    "${'$'}ref": "#/definitions/intDef"
                  },
                  "bool": {
                    "${'$'}ref": "#/definitions/boolDef"
                  },
                  "array": {
                    "${'$'}ref": "#/definitions/arrayDef"
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
              "type": "Control",
              "scope": "#/properties/objectArray"
            },
            {
              "type": "Control",
              "scope": "#/properties/stringArray"
            },
            {
              "type": "Control",
              "scope": "#/properties/objectArrayWithPropertyRefs"
            }
          ]
        }
        """.trimIndent(),
    )
}
