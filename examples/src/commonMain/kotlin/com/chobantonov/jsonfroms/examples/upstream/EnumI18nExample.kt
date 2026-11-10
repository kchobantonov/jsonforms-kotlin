// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamEnumI18nExample: ExampleDescription by lazy {
    upstreamExample(
        name = "enum-i18n",
        label = "Enums (i18n)",
        dataJson = """
        {
          "country": "DE"
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "country": {
              "type": "string",
              "enum": [
                "DE",
                "IT",
                "JP",
                "US",
                "RU",
                "Other"
              ]
            },
            "countryNoAutocomplete": {
              "type": "string",
              "enum": [
                "DE",
                "IT",
                "JP",
                "US",
                "RU",
                "Other"
              ]
            },
            "status": {
              "type": "string",
              "oneOf": [
                {
                  "const": "pending",
                  "title": "Pending"
                },
                {
                  "const": "approved",
                  "title": "Approved"
                },
                {
                  "const": "rejected",
                  "title": "Rejected"
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
              "label": "Enum with i18n (Autocomplete)",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/country",
                  "label": "Country (with autocomplete)"
                }
              ]
            },
            {
              "type": "Group",
              "label": "Enum with i18n (Dropdown)",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/countryNoAutocomplete",
                  "label": "Country (dropdown)",
                  "options": {
                    "autocomplete": false
                  }
                }
              ]
            },
            {
              "type": "Group",
              "label": "OneOf Enum with i18n",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/status",
                  "label": "Status"
                }
              ]
            }
          ]
        }
        """.trimIndent(),
    )
}
