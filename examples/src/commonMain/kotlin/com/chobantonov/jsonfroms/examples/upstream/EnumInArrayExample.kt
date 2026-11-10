// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamEnumInArrayExample: ExampleDescription by lazy {
    upstreamExample(
        name = "enumInArray",
        label = "Array containing enums",
        dataJson = """
        []
        """.trimIndent(),
        schemaJson = """
        {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "plainEnum": {
                "type": "string",
                "enum": [
                  "foo",
                  "bar"
                ]
              },
              "oneOfEnum": {
                "type": "string",
                "oneOf": [
                  {
                    "const": "foo",
                    "title": "Foo"
                  },
                  {
                    "const": "bar",
                    "title": "Bar"
                  },
                  {
                    "const": "foobar",
                    "title": "FooBar"
                  }
                ]
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
              "scope": "#"
            }
          ]
        }
        """.trimIndent(),
    )
}
