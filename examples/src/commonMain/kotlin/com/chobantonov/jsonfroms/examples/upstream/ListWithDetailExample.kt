// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamListWithDetailExample: ExampleDescription by lazy {
    upstreamExample(
        name = "list-with-detail",
        label = "List With Detail",
        dataJson = """
        {
          "orders": [
            {
              "customer": {
                "id": "471201",
                "name": "Sirius Cybernetics Corporation",
                "department": "Complaints Division"
              },
              "title": "42 killer robots",
              "ordered": true,
              "processId": "1890004498",
              "assignee": "Philip J. Fry",
              "status": "ordered",
              "startDate": "2018-06-01",
              "endDate": "2018-08-01"
            },
            {
              "customer": {
                "id": "471202",
                "name": "Very Big Corporation of America"
              },
              "title": "1000 gallons of MomCorp Oil",
              "processId": "1890004499",
              "assignee": "Jen Barber",
              "startDate": "2018-07-01",
              "status": "planned"
            }
          ]
        }
        """.trimIndent(),
        schemaJson = """
        {
          "definitions": {
            "order": {
              "type": "object",
              "properties": {
                "customer": {
                  "type": "object",
                  "properties": {
                    "id": {
                      "type": "string"
                    },
                    "name": {
                      "type": "string",
                      "format": "email"
                    },
                    "department": {
                      "type": "string"
                    }
                  }
                },
                "title": {
                  "type": "string",
                  "minLength": 5,
                  "title": "Official Title"
                },
                "ordered": {
                  "type": "boolean"
                },
                "processId": {
                  "type": "number",
                  "minimum": 0
                },
                "assignee": {
                  "type": "string"
                },
                "startDate": {
                  "type": "string",
                  "format": "date"
                },
                "endDate": {
                  "type": "string",
                  "format": "date"
                },
                "status": {
                  "type": "string",
                  "enum": [
                    "unordered",
                    "planned",
                    "ordered"
                  ]
                }
              },
              "required": [
                "title"
              ]
            }
          },
          "type": "object",
          "properties": {
            "orders": {
              "type": "array",
              "items": {
                "${'$'}ref": "#/definitions/order"
              }
            }
          }
        }
        """.trimIndent(),
        uischemaJson = """
        {
          "type": "ListWithDetail",
          "scope": "#/properties/orders",
          "options": {
            "labelRef": "#/items/properties/customer/properties/name",
            "detail": {
              "type": "VerticalLayout",
              "elements": [
                {
                  "type": "HorizontalLayout",
                  "elements": [
                    {
                      "type": "Control",
                      "scope": "#/properties/title"
                    },
                    {
                      "type": "Control",
                      "scope": "#/properties/processId"
                    }
                  ]
                },
                {
                  "type": "Group",
                  "label": "Customer",
                  "elements": [
                    {
                      "type": "Control",
                      "label": "ID",
                      "scope": "#/properties/customer/properties/id"
                    },
                    {
                      "type": "Control",
                      "label": "Name",
                      "scope": "#/properties/customer/properties/name"
                    },
                    {
                      "type": "Control",
                      "label": "Department",
                      "scope": "#/properties/customer/properties/department"
                    }
                  ]
                },
                {
                  "type": "VerticalLayout",
                  "elements": [
                    {
                      "type": "VerticalLayout",
                      "elements": [
                        {
                          "type": "HorizontalLayout",
                          "elements": [
                            {
                              "type": "Control",
                              "scope": "#/properties/ordered",
                              "options": {
                                "toggle": true
                              }
                            },
                            {
                              "type": "Control",
                              "scope": "#/properties/assignee"
                            }
                          ]
                        },
                        {
                          "type": "HorizontalLayout",
                          "elements": [
                            {
                              "type": "Control",
                              "scope": "#/properties/startDate"
                            },
                            {
                              "type": "Control",
                              "scope": "#/properties/endDate"
                            }
                          ]
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/status"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          }
        }
        """.trimIndent(),
    )
}
