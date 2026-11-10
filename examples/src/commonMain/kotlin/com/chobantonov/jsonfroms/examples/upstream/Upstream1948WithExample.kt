// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstream1948WithExample: ExampleDescription by lazy {
    upstreamExample(
        name = "1948_with",
        label = "Issue 1948 - Array renderer selection (with schema)",
        dataJson = """
        {
          "import": [
            {
              "document": "Document1",
              "package": "Package1",
              "prefix": "Prefix"
            }
          ]
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "definitions": {
            "import": {
              "title": "Import",
              "type": "object",
              "properties": {
                "eClass": {
                  "const": "http://my_schema/1.0.0#//Import"
                },
                "document": {
                  "type": "string"
                },
                "package": {
                  "type": "string"
                },
                "prefix": {
                  "type": "string"
                }
              }
            }
          },
          "properties": {
            "import": {
              "type": "array",
              "items": {
                "${'$'}ref": "#/definitions/import"
              }
            }
          }
        }
        """.trimIndent(),
    )
}
