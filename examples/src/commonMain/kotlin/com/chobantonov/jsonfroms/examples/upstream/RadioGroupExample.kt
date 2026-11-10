// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamRadioGroupExample: ExampleDescription by lazy {
    upstreamExample(
        name = "radio-group",
        label = "Radio Group",
        dataJson = """
        {}
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "exampleRadioEnum": {
              "type": "string",
              "enum": [
                "One",
                "Two",
                "Three"
              ]
            },
            "exampleRadioOneOfEnum": {
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
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "VerticalLayout",
          "elements": [
            {
              "type": "Group",
              "label": "Simple enum",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/exampleRadioEnum",
                  "options": {
                    "format": "radio",
                    "orientation": "vertical"
                  }
                }
              ]
            },
            {
              "type": "Group",
              "label": "One of Enum",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/exampleRadioOneOfEnum",
                  "options": {
                    "format": "radio"
                  }
                }
              ]
            }
          ]
        }
        """.trimIndent(),
    )
}
