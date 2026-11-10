// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamDatesExample: ExampleDescription by lazy {
    upstreamExample(
        name = "dates",
        label = "Dates",
        dataJson = """
        {
          "schemaBased": {
            "date": "2026-11-10",
            "time": "13:37:00",
            "datetime": "2026-11-10T19:24:07.313Z"
          },
          "uiSchemaBased": {
            "date": "2024-01",
            "time": "13:37:00",
            "datetime": "1999/12/11 10:05 am"
          }
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "schemaBased": {
              "type": "object",
              "properties": {
                "date": {
                  "type": "string",
                  "format": "date",
                  "description": "schema-based date picker"
                },
                "time": {
                  "type": "string",
                  "format": "time",
                  "description": "schema-based time picker"
                },
                "datetime": {
                  "type": "string",
                  "format": "date-time",
                  "description": "schema-based datetime picker"
                }
              }
            },
            "uiSchemaBased": {
              "type": "object",
              "properties": {
                "date": {
                  "type": "string",
                  "description": "does not allow to select days"
                },
                "time": {
                  "type": "string",
                  "description": "24 hour format"
                },
                "datetime": {
                  "type": "string",
                  "description": "uischema-based datetime picker"
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
              "type": "HorizontalLayout",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/schemaBased/properties/date"
                },
                {
                  "type": "Control",
                  "scope": "#/properties/schemaBased/properties/time"
                },
                {
                  "type": "Control",
                  "scope": "#/properties/schemaBased/properties/datetime"
                }
              ]
            },
            {
              "type": "HorizontalLayout",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/uiSchemaBased/properties/date",
                  "label": "Year Month Picker",
                  "options": {
                    "format": "date",
                    "clearLabel": "Clear it!",
                    "cancelLabel": "Abort",
                    "okLabel": "Do it",
                    "views": [
                      "year",
                      "month"
                    ],
                    "dateFormat": "YYYY.MM",
                    "dateSaveFormat": "YYYY-MM"
                  }
                },
                {
                  "type": "Control",
                  "scope": "#/properties/uiSchemaBased/properties/time",
                  "options": {
                    "format": "time",
                    "ampm": true
                  }
                },
                {
                  "type": "Control",
                  "scope": "#/properties/uiSchemaBased/properties/datetime",
                  "options": {
                    "format": "date-time",
                    "dateTimeFormat": "DD-MM-YY hh:mm:a",
                    "dateTimeSaveFormat": "YYYY/MM/DD h:mm a",
                    "ampm": true
                  }
                }
              ]
            }
          ]
        }
        """.trimIndent(),
    )
}
