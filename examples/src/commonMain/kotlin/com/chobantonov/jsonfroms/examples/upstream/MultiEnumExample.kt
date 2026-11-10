// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamMultiEnumExample: ExampleDescription by lazy {
    upstreamExample(
        name = "multi-enum",
        label = "Enum - Multi selection",
        dataJson = """
        {
          "oneOfMultiEnum": [
            "foo"
          ],
          "multiEnum": [
            "bar"
          ],
          "multiSelect": [
            "Banana",
            "Cherry"
          ]
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "oneOfMultiEnum": {
              "type": "array",
              "uniqueItems": true,
              "items": {
                "oneOf": [
                  {
                    "const": "foo",
                    "title": "My Foo"
                  },
                  {
                    "const": "bar",
                    "title": "My Bar"
                  },
                  {
                    "const": "foobar",
                    "title": "My FooBar"
                  }
                ]
              }
            },
            "multiEnum": {
              "type": "array",
              "uniqueItems": true,
              "items": {
                "type": "string",
                "enum": [
                  "foo",
                  "bar",
                  "foobar"
                ]
              }
            },
            "multiSelect": {
              "type": "array",
              "title": "Select Fruits",
              "uniqueItems": true,
              "items": {
                "type": "string",
                "enum": [
                  "Apple",
                  "Banana",
                  "Cherry",
                  "Date",
                  "Elderberry"
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
              "scope": "#/properties/oneOfMultiEnum"
            },
            {
              "type": "Control",
              "scope": "#/properties/multiEnum"
            },
            {
              "type": "Control",
              "scope": "#/properties/multiSelect",
              "options": {
                "format": "multiselect"
              }
            }
          ]
        }
        """.trimIndent(),
    )
}
