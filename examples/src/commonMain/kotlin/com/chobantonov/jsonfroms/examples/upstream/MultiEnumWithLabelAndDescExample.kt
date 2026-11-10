// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamMultiEnumWithLabelAndDescExample: ExampleDescription by lazy {
    upstreamExample(
        name = "multi-enum-with-label-and-desc",
        label = "Enum - Multi selection with label and desc",
        dataJson = """
        {
          "oneOfMultiEnum": [
            "foo"
          ],
          "multiEnum": [
            "bar"
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
              "description": "Description",
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
              "description": "Description",
              "items": {
                "type": "string",
                "enum": [
                  "foo",
                  "bar",
                  "foobar"
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
              "scope": "#/properties/oneOfMultiEnum",
              "label": "Form Label",
              "options": {
                "showUnfocusedDescription": true
              }
            },
            {
              "type": "Control",
              "scope": "#/properties/multiEnum",
              "label": "Form Label",
              "options": {
                "showUnfocusedDescription": true
              }
            }
          ]
        }
        """.trimIndent(),
    )
}
