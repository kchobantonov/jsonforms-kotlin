// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamOneOfRecursiveExample: ExampleDescription by lazy {
    upstreamExample(
        name = "oneOf-recursive",
        label = "oneOf recursive",
        dataJson = """
        {}
        """.trimIndent(),
        schemaJson = """
        {
          "${'$'}schema": "http://json-schema.org/draft-07/schema#",
          "definitions": {
            "fileOrFolder": {
              "title": "fileOrFolder",
              "oneOf": [
                {
                  "${'$'}ref": "#/definitions/file"
                },
                {
                  "${'$'}ref": "#/definitions/folder"
                }
              ]
            },
            "file": {
              "title": "File",
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                }
              }
            },
            "folder": {
              "type": "object",
              "title": "Folder",
              "properties": {
                "name": {
                  "type": "string"
                },
                "children": {
                  "type": "array",
                  "items": {
                    "${'$'}ref": "#/definitions/fileOrFolder"
                  }
                }
              }
            }
          },
          "type": "object",
          "properties": {
            "root": {
              "type": "array",
              "items": {
                "${'$'}ref": "#/definitions/folder"
              }
            }
          }
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "Control",
          "scope": "#"
        }
        """.trimIndent(),
    )
}
