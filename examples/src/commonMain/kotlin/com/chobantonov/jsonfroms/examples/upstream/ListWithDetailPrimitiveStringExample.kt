// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamListWithDetailPrimitiveStringExample: ExampleDescription by lazy {
    upstreamExample(
        name = "list-with-detail-primitive-string",
        label = "List With Detail primitive (string)",
        dataJson = """
        {
          "an-array-of-strings": [
            "foo",
            "bar",
            "foobar"
          ]
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "an-array-of-strings": {
              "type": "array",
              "items": {
                "type": "string"
              }
            }
          }
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "ListWithDetail",
          "scope": "#/properties/an-array-of-strings"
        }
        """.trimIndent(),
    )
}
