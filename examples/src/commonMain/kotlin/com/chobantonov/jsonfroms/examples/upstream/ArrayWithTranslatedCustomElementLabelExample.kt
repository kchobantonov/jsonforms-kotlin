// Generated from @jsonforms/examples 3.9.0-alpha.2 (2c5fae62a8aace19970ac037d122fa6428cd0436).
package com.chobantonov.jsonfroms.examples

internal val upstreamArrayWithTranslatedCustomElementLabelExample: ExampleDescription by lazy {
    upstreamExample(
        name = "array-with-translated-custom-element-label",
        label = "Array with translated custom element label",
        dataJson = """
        {
          "article": {
            "title": "title",
            "comments": [
              {
                "visibility": "PUBLIC",
                "status": "NEW",
                "author": {
                  "name": "John",
                  "type": "WRITER",
                  "role": "ROLE_1"
                }
              },
              {
                "visibility": "PRIVATE",
                "status": "REVIEWED",
                "author": {
                  "name": "John",
                  "type": "AUTHOR",
                  "role": "ROLE_2"
                }
              }
            ]
          }
        }
        """.trimIndent(),
        schemaJson = """
        {
          "type": "object",
          "properties": {
            "article": {
              "type": "object",
              "properties": {
                "title": {
                  "type": "string"
                },
                "comments": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "visibility": {
                        "type": "string",
                        "enum": [
                          "PUBLIC",
                          "PRIVATE"
                        ]
                      },
                      "status": {
                        "type": "string",
                        "oneOf": [
                          {
                            "const": "NEW"
                          },
                          {
                            "const": "REVIEWED",
                            "title": "Reviewed comment"
                          }
                        ]
                      },
                      "author": {
                        "type": "object",
                        "properties": {
                          "name": {
                            "type": "string"
                          },
                          "type": {
                            "type": "string",
                            "enum": [
                              "AUTHOR",
                              "WRITER"
                            ]
                          },
                          "role": {
                            "type": "string",
                            "oneOf": [
                              {
                                "const": "ROLE_1"
                              },
                              {
                                "const": "ROLE_2",
                                "title": "Second role"
                              }
                            ]
                          }
                        }
                      }
                    }
                  }
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
              "type": "Group",
              "label": "Standard array control with elementLabelProp pointing on an direct enum (expected translated PUBLIC/PRIVATE)",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/article/properties/comments",
                  "options": {
                    "elementLabelProp": "visibility",
                    "detail": {
                      "type": "VerticalLayout",
                      "elements": [
                        {
                          "type": "Control",
                          "scope": "#/properties/visibility"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/status"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/name"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/type"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/role"
                        }
                      ]
                    }
                  }
                }
              ]
            },
            {
              "type": "Group",
              "label": "Standard array control with elementLabelProp pointing on an direct oneOf (expected translated NEW/Reviewed comment)",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/article/properties/comments",
                  "options": {
                    "elementLabelProp": "status",
                    "detail": {
                      "type": "VerticalLayout",
                      "elements": [
                        {
                          "type": "Control",
                          "scope": "#/properties/visibility"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/status"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/name"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/type"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/role"
                        }
                      ]
                    }
                  }
                }
              ]
            },
            {
              "type": "Group",
              "label": "ListWithDetail with elementLabelProp pointing on an direct enum (expected translated PUBLIC/PRIVATE)",
              "elements": [
                {
                  "type": "ListWithDetail",
                  "scope": "#/properties/article/properties/comments",
                  "options": {
                    "elementLabelProp": "visibility",
                    "detail": {
                      "type": "VerticalLayout",
                      "elements": [
                        {
                          "type": "Control",
                          "scope": "#/properties/visibility"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/status"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/name"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/type"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/role"
                        }
                      ]
                    }
                  }
                }
              ]
            },
            {
              "type": "Group",
              "label": "ListWithDetail with elementLabelProp pointing on an direct oneOf (expected translated NEW/Reviewed comment)",
              "elements": [
                {
                  "type": "ListWithDetail",
                  "scope": "#/properties/article/properties/comments",
                  "options": {
                    "elementLabelProp": "status",
                    "detail": {
                      "type": "VerticalLayout",
                      "elements": [
                        {
                          "type": "Control",
                          "scope": "#/properties/visibility"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/status"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/name"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/type"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/role"
                        }
                      ]
                    }
                  }
                }
              ]
            },
            {
              "type": "Group",
              "label": "Standard array control with elementLabelProp pointing on an deep enum (expected translated WRITER/AUTHOR)",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/article/properties/comments",
                  "options": {
                    "elementLabelProp": "author.type",
                    "detail": {
                      "type": "VerticalLayout",
                      "elements": [
                        {
                          "type": "Control",
                          "scope": "#/properties/visibility"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/status"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/name"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/type"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/role"
                        }
                      ]
                    }
                  }
                }
              ]
            },
            {
              "type": "Group",
              "label": "Standard array control with elementLabelProp pointing on an deep oneOf (expected translated ROLE_1/Second role)",
              "elements": [
                {
                  "type": "Control",
                  "scope": "#/properties/article/properties/comments",
                  "options": {
                    "elementLabelProp": "author.role",
                    "detail": {
                      "type": "VerticalLayout",
                      "elements": [
                        {
                          "type": "Control",
                          "scope": "#/properties/visibility"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/status"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/name"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/type"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/role"
                        }
                      ]
                    }
                  }
                }
              ]
            },
            {
              "type": "Group",
              "label": "ListWithDetail with elementLabelProp pointing on an deep enum (expected translated WRITER/AUTHOR)",
              "elements": [
                {
                  "type": "ListWithDetail",
                  "scope": "#/properties/article/properties/comments",
                  "options": {
                    "elementLabelProp": "author.type",
                    "detail": {
                      "type": "VerticalLayout",
                      "elements": [
                        {
                          "type": "Control",
                          "scope": "#/properties/visibility"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/status"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/name"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/type"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/role"
                        }
                      ]
                    }
                  }
                }
              ]
            },
            {
              "type": "Group",
              "label": "ListWithDetail with elementLabelProp pointing on an deep oneOf (expected translated ROLE_1/Second role)",
              "elements": [
                {
                  "type": "ListWithDetail",
                  "scope": "#/properties/article/properties/comments",
                  "options": {
                    "elementLabelProp": "author.role",
                    "detail": {
                      "type": "VerticalLayout",
                      "elements": [
                        {
                          "type": "Control",
                          "scope": "#/properties/visibility"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/status"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/name"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/type"
                        },
                        {
                          "type": "Control",
                          "scope": "#/properties/author/properties/role"
                        }
                      ]
                    }
                  }
                }
              ]
            }
          ]
        }
        """.trimIndent(),
    )
}
