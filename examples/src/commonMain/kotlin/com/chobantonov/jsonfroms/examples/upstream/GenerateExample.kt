// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamGenerateExample: ExampleDescription by lazy {
    upstreamExample(
        name = "generate",
        label = "Generate both Schemas",
        dataJson = """
        {
          "name": "John Doe",
          "vegetarian": false,
          "birthDate": "1985-06-02",
          "personalData": {
            "age": 34
          },
          "postalCode": "12345"
        }
        """.trimIndent(),
    )
}
