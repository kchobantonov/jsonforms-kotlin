package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.chobantonov.jsonfroms.examples.toUiSchemaElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

private data class ExtendedFixture(
    val name: String,
    val label: String,
    val data: String,
    val schema: String,
    val uischema: String,
)

private val excludedExtendedFixtures = setOf("ag-grid", "template-layout", "template-slot")

private val extendedFixtures = listOf(
    ExtendedFixture(
        name = "ag-grid",
        label = "AG Grid array renderer",
        data = """{
  "people": [
    {
      "firstName": "Ada",
      "lastName": "Lovelace",
      "age": 36,
      "role": "Researcher",
      "favoriteColor": "#7c3aed",
      "tenure": "P2Y6M",
      "date": "2026-07-18",
      "time": "09:30:00",
      "dateTime": "2026-07-18T09:30:00",
      "active": true,
      "address": {
        "street": "12 St James's Square",
        "city": "London",
        "country": "UK",
        "postalCode": "SW1Y"
      },
      "phoneNumbers": ["+44 20 1234 5678"]
    },
    {
      "firstName": "Grace",
      "lastName": "Hopper",
      "age": 85,
      "role": "Leader",
      "favoriteColor": "#0ea5e9cc",
      "tenure": "P5Y",
      "date": "2026-07-20",
      "time": "14:45:00",
      "dateTime": "2026-07-20T14:45:00",
      "active": false,
      "address": {
        "street": "1 Navy Way",
        "city": "Arlington",
        "country": "USA",
        "postalCode": "22201"
      },
      "phoneNumbers": ["+1 703 555 0100", "+1 703 555 0101"]
    }
  ]
}""",
        schema = """{
  "type": "object",
  "properties": {
    "people": {
      "type": "array",
      "title": "People",
      "items": {
        "type": "object",
        "required": ["firstName", "lastName"],
        "properties": {
          "firstName": { "type": "string", "title": "First name" },
          "lastName": { "type": "string", "title": "Last name" },
          "age": { "type": "integer", "title": "Age", "minimum": 0 },
          "role": {
            "type": "string",
            "title": "Role",
            "enum": ["Engineer", "Researcher", "Leader"]
          },
          "favoriteColor": {
            "type": "string",
            "format": "color",
            "pattern": "^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})__DOLLAR__",
            "title": "Favorite color"
          },
          "tenure": {
            "type": "string",
            "format": "duration",
            "title": "Tenure"
          },
          "date": {
            "type": "string",
            "format": "date",
            "title": "Date"
          },
          "time": {
            "type": "string",
            "format": "time",
            "title": "Time"
          },
          "dateTime": {
            "type": "string",
            "format": "date-time",
            "title": "DateTime"
          },
          "active": { "type": "boolean", "title": "Active" },
          "address": {
            "type": "object",
            "title": "Address",
            "properties": {
              "street": { "type": "string", "title": "Street" },
              "city": { "type": "string", "title": "City" },
              "country": { "type": "string", "title": "Country" },
              "postalCode": { "type": "string", "title": "Postal code" }
            }
          },
          "phoneNumbers": {
            "type": "array",
            "title": "Phone numbers",
            "items": { "type": "string" }
          }
        }
      }
    }
  }
}""",
        uischema = """{
  "type": "VerticalLayout",
  "elements": [
    {
      "type": "Control",
      "scope": "#/properties/people",
      "options": {
        "variant": "ag-grid",
        "restrict": true,
        "showSortButtons": true,
        "gridHeight": "430px",
        "cells": {
          "address": {
            "display": { "type": "Control", "scope": "#/properties/street" },
            "detail": {
              "type": "VerticalLayout",
              "elements": [
                { "type": "Control", "scope": "#/properties/street" },
                {
                  "type": "HorizontalLayout",
                  "elements": [
                    { "type": "Control", "scope": "#/properties/city" },
                    { "type": "Control", "scope": "#/properties/postalCode" }
                  ]
                },
                { "type": "Control", "scope": "#/properties/country" }
              ]
            }
          },
          "phoneNumbers": {
            "detail": { "type": "Control", "scope": "#" }
          }
        },
        "agGridOptions": {
          "columnDefs": [
            { "field": "firstName", "width": 170 },
            { "field": "lastName", "width": 170 },
            { "field": "age", "width": 100 },
            { "field": "role", "width": 170 },
            { "field": "favoriteColor", "width": 180 },
            { "field": "tenure", "width": 160 },
            { "field": "date", "width": 170 },
            { "field": "time", "width": 180 },
            { "field": "dateTime", "width": 230 },
            { "field": "active", "width": 110, "filter": false },
            { "field": "address", "width": 220, "filter": false },
            { "field": "phoneNumbers", "width": 220, "filter": false }
          ],
          "pagination": true,
          "paginationPageSize": 10,
          "paginationPageSizeSelector": [10, 20, 50, 100]
        }
      }
    }
  ]
}""",
    ),
    ExtendedFixture(
        name = "button",
        label = "Button",
        data = """{
  "username": "john.doe@email.com",
  "status": "Idle"
}""",
        schema = """{
  "__DOLLAR__schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "username": {
      "type": "string",
      "description": "Login name"
    },
    "status": {
      "type": "string",
      "description": "Action result"
    },
    "lastTriggeredBy": {
      "type": "string",
      "description": "Last executed action"
    }
  }
}""",
        uischema = """{
  "type": "VerticalLayout",
  "elements": [
    {
      "type": "Control",
      "scope": "#/properties/username",
      "label": "Username"
    },
    {
      "type": "HorizontalLayout",
      "elements": [
        {
          "type": "Button",
          "label": "Apply Action",
          "action": "applyStatus",
          "color": "primary",
          "params": {
            "statusByLocale": {
              "en": "Updated from handle-action callback",
              "bg": "Обновено от callback на handle-action"
            }
          }
        },
        {
          "type": "Button",
          "label": "Run Host Action",
          "color": "alternative",
          "action": "runScript"
        },
        {
          "type": "Button",
          "label": "Simulate Submit",
          "action": "simulateHttpSubmit",
          "color": "secondary",
          "params": {
            "submittingMessageByLocale": {
              "en": "Please wait, action in progress...",
              "bg": "Моля, изчакайте, действието се изпълнява..."
            },
            "submittedMessageByLocale": {
              "en": "Action complete! Click OK to continue. Check your javascript console for the JSON data from your form.",
              "bg": "Действието завърши! Натиснете OK, за да продължите. Проверете конзолата на браузъра за JSON данните от формата."
            },
            "statusByLocale": {
              "en": "Simulated HTTP request completed",
              "bg": "Симулираната HTTP заявка завърши"
            }
          }
        },
        {
          "type": "Button",
          "label": "Switch to English",
          "color": "secondary",
          "action": "changeLang",
          "params": {
            "lang": "en",
            "localeLabel": "English",
            "statusByLocale": {
              "en": "Language changed to English",
              "bg": "Езикът е сменен на английски"
            }
          }
        },
        {
          "type": "Button",
          "label": "Switch to Bulgarian",
          "color": "secondary",
          "action": "changeLang",
          "params": {
            "lang": "bg",
            "localeLabel": "Български",
            "statusByLocale": {
              "en": "Language changed to Bulgarian",
              "bg": "Езикът е сменен на български"
            }
          }
        }
      ]
    },
    {
      "type": "Control",
      "scope": "#/properties/status",
      "label": "Status",
      "options": {
        "showUnfocusedDescription": true
      }
    },
    {
      "type": "Control",
      "scope": "#/properties/lastTriggeredBy",
      "label": "Last Triggered By"
    }
  ]
}""",
    ),
    ExtendedFixture(
        name = "color",
        label = "Color",
        data = """{
  "shortColor": "#0af",
  "brandColor": "#3b82f6",
  "overlayColor": "#3b82f680"
}""",
        schema = """{
  "type": "object",
  "properties": {
    "shortColor": {
      "type": "string",
      "format": "color",
      "pattern": "^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})__DOLLAR__",
      "title": "Short color"
    },
    "brandColor": {
      "type": "string",
      "format": "color",
      "pattern": "^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})__DOLLAR__",
      "title": "Brand color"
    },
    "overlayColor": {
      "type": "string",
      "format": "color",
      "pattern": "^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})__DOLLAR__",
      "title": "Overlay color"
    }
  }
}""",
        uischema = """{
  "type": "VerticalLayout",
  "elements": [
    { "type": "Control", "scope": "#/properties/shortColor" },
    { "type": "Control", "scope": "#/properties/brandColor" },
    { "type": "Control", "scope": "#/properties/overlayColor" }
  ]
}""",
    ),
    ExtendedFixture(
        name = "duration",
        label = "Duration",
        data = """{
  "duration": "P3D"
}""",
        schema = """{
  "__DOLLAR__schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "duration": {
      "type": "string",
      "format": "duration"
    }
  }
}""",
        uischema = """{
  "type": "VerticalLayout",
  "elements": [
    {
      "type": "Control",
      "scope": "#/properties/duration",
      "label": "Duration",
      "options": {
        "placeholder": "P3Y6M4DT12H30M5S or P2W"
      }
    }
  ]
}""",
    ),
    ExtendedFixture(
        name = "extended-control-options",
        label = "Extended Control Options",
        data = """{
  "multilineString": "Multi-\nline\nexample\nwith icons",
  "slider": 4,
  "trimText": "abcdefg",
  "restrictText": "abcde",
  "toggle": false,
  "phone": "1115554444888",
  "formattedPhone": "(333)-555-4444",
  "placeholderInfo": [
    {
      "placeholder": "#",
      "format": "Number (0-9)"
    },
    {
      "placeholder": "A",
      "format": "Letter in any case (a-z,A-Z)"
    },
    {
      "placeholder": "N",
      "format": "Number or letter (a-z,A-Z,0-9)"
    },
    {
      "placeholder": "X",
      "format": "Any symbol"
    }
  ]
}""",
        schema = """{
  "__DOLLAR__schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "multilineString": {
      "type": "string",
      "description": "Multiline Example"
    },
    "slider": {
      "type": "number",
      "minimum": 1,
      "maximum": 5,
      "default": 2,
      "description": "Slider Example"
    },
    "trimText": {
      "type": "string",
      "description": "Trim indicates whether the control shall grab the full width available"
    },
    "restrictText": {
      "type": "string",
      "maxLength": 5,
      "description": "Restricts the input length to the set value (in this case: 5)"
    },
    "unfocusedDescription": {
      "type": "string",
      "description": "This description is shown even when the control is not focused"
    },
    "hideRequiredAsterisk": {
      "type": "string",
      "description": "Hides the \"*\" symbol, when the field is required"
    },
    "toggle": {
      "type": "boolean",
      "description": "The \"toggle\" option renders boolean values as a toggle."
    },
    "phone": {
      "type": "string",
      "pattern": "^[0-9]{13}__DOLLAR__"
    },
    "formattedPhone": {
      "type": "string",
      "pattern": "^\\([0-9]{3}\\)-[0-9]{3}-[0-9]{4}__DOLLAR__"
    },
    "placeholderInfo": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "placeholder": {
            "type": "string"
          },
          "format": {
            "type": "string"
          }
        }
      }
    }
  },
  "required": ["hideRequiredAsterisk", "restrictText", "phone"]
}""",
        uischema = """{
  "type": "VerticalLayout",
  "elements": [
    {
      "type": "Control",
      "scope": "#/properties/multilineString",
      "options": {
        "multi": true,
        "clearable": false
      }
    },
    {
      "type": "Control",
      "scope": "#/properties/slider",
      "options": {
        "slider": true
      }
    },
    {
      "type": "Control",
      "scope": "#/properties/trimText",
      "options": {
        "trim": true
      }
    },
    {
      "type": "Control",
      "scope": "#/properties/restrictText",
      "options": {
        "restrict": true
      }
    },
    {
      "type": "Control",
      "scope": "#/properties/unfocusedDescription",
      "options": {
        "showUnfocusedDescription": true
      }
    },
    {
      "type": "Control",
      "scope": "#/properties/hideRequiredAsterisk",
      "options": {
        "hideRequiredAsterisk": true
      }
    },
    {
      "type": "Control",
      "scope": "#/properties/toggle",
      "label": "Boolean as Toggle",
      "options": {
        "toggle": true
      }
    },
    {
      "type": "Group",
      "label": "Masked input",
      "elements": [
        {
          "type": "Label",
          "text": "Supported default mask replacers: (If you place value null then it will ignore the default)"
        },
        {
          "type": "Control",
          "scope": "#/properties/placeholderInfo",
          "options": {
            "readonly": true
          }
        }
      ]
    },
    {
      "type": "Control",
      "scope": "#/properties/phone",
      "label": "Phone (format not stored)",
      "options": {
        "mask": "(SSS) SSS - SSSS #SSS",
        "maskReplacers": {
          "S": "\\d",
          "#": null
        }
      }
    },
    {
      "type": "Control",
      "scope": "#/properties/formattedPhone",
      "label": "Phone (format is stored)",
      "options": {
        "placeholder": "(###)-###-####",
        "mask": "(###)-###-####",
        "returnMaskedValue": true,
        "skeleton": {
          "input": {
            "class": "preset-tonal-success"
          }
        }
      }
    }
  ]
}""",
    ),
    ExtendedFixture(
        name = "file",
        label = "File",
        data = """{}""",
        schema = """{
  "__DOLLAR__schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "imageDataUri": {
      "type": "string",
      "format": "uri",
      "contentEncoding": "base64",
      "contentMediaType": "image/*",
      "description": "Image with maximum size of 1MB encoded as data URI"
    },
    "fileDataUriWithFileName": {
      "type": "string",
      "format": "binary",
      "formatMaximum": "1048576",
      "description": "File with maximum size of 1MB encoded as data URI and including the file name"
    },
    "base64String": {
      "type": "string",
      "contentEncoding": "base64",
      "description": "File with maximum size of 1MB encoded as base64"
    }
  }
}""",
        uischema = """{
  "type": "VerticalLayout",
  "elements": [
    {
      "type": "Control",
      "scope": "#/properties/imageDataUri",
      "options": {
        "showUnfocusedDescription": true,
        "formatMaximum": "1048576",
        "vuetify": {
          "v-file-input": {
            "showSize": true
          }
        }
      }
    },
    {
      "type": "Control",
      "scope": "#/properties/fileDataUriWithFileName",
      "options": {
        "showUnfocusedDescription": true,
        "vuetify": {
          "v-file-input": {
            "showSize": true
          }
        }
      }
    },
    {
      "type": "Control",
      "scope": "#/properties/base64String",
      "options": {
        "showUnfocusedDescription": true,
        "formatMaximum": 1048576,
        "vuetify": {
          "v-file-input": {
            "showSize": true
          }
        }
      }
    }
  ]
}""",
    ),
    ExtendedFixture(
        name = "null-control",
        label = "Null",
        data = """{
  "alreadyNull": null
}""",
        schema = """{
  "__DOLLAR__schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "alreadyNull": {
      "type": "null",
      "description": "Checked means the property is stored as a literal null value."
    },
    "optionalNull": {
      "type": "null",
      "description": "Unchecked removes the property from the data object."
    }
  }
}""",
        uischema = """{
  "type": "VerticalLayout",
  "elements": [
    {
      "type": "Control",
      "scope": "#/properties/alreadyNull",
      "label": "Already null",
      "options": {
        "showUnfocusedDescription": true
      }
    },
    {
      "type": "Control",
      "scope": "#/properties/optionalNull",
      "label": "Optional null",
      "options": {
        "showUnfocusedDescription": true
      }
    }
  ]
}""",
    ),
    ExtendedFixture(
        name = "split-layout",
        label = "Split Layout",
        data = """{
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada@example.com",
  "role": "Developer",
  "newsletter": true,
  "timeZone": "Europe/Sofia"
}""",
        schema = """{
  "__DOLLAR__schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "firstName": {
      "type": "string",
      "title": "First Name"
    },
    "lastName": {
      "type": "string",
      "title": "Last Name"
    },
    "email": {
      "type": "string",
      "format": "email",
      "title": "Email"
    },
    "role": {
      "type": "string",
      "title": "Role",
      "enum": ["Developer", "QA", "Product Manager", "Designer"]
    },
    "newsletter": {
      "type": "boolean",
      "title": "Newsletter"
    },
    "timeZone": {
      "type": "string",
      "title": "Time Zone"
    }
  },
  "required": ["firstName", "email"]
}""",
        uischema = """{
  "type": "VerticalLayout",
  "elements": [
    {
      "type": "HorizontalLayout",
      "options": {
        "variant": "splitter"
      },
      "elements": [
        {
          "type": "Control",
          "scope": "#/properties/firstName"
        },
        {
          "type": "Control",
          "scope": "#/properties/lastName"
        },
        {
          "type": "Control",
          "scope": "#/properties/email"
        }
      ]
    },
    {
      "type": "VerticalLayout",
      "options": {
        "variant": "splitter",
        "height": "26rem"
      },
      "elements": [
        {
          "type": "Control",
          "scope": "#/properties/role"
        },
        {
          "type": "Control",
          "scope": "#/properties/newsletter"
        },
        {
          "type": "Control",
          "scope": "#/properties/timeZone"
        }
      ]
    }
  ]
}""",
    ),
    ExtendedFixture(
        name = "template-layout",
        label = "Template Layout",
        data = """{
  "name": "John Doe",
  "vegetarian": false,
  "birthDate": "1985-06-02",
  "nationality": "US",
  "personalData": {
    "age": 34,
    "height": 182,
    "drivingSkill": 7
  },
  "occupation": "Engineer",
  "postalCode": "12345"
}""",
        schema = """{
  "__DOLLAR__schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "name": {
      "type": "string",
      "minLength": 3,
      "description": "Please enter your name",
      "i18n": "name"
    },
    "vegetarian": {
      "type": "boolean",
      "i18n": "vegetarian"
    },
    "birthDate": {
      "type": "string",
      "format": "date",
      "i18n": "birth"
    },
    "nationality": {
      "type": "string",
      "enum": ["US", "BG", "DE", "JP", "RU", "Other"],
      "i18n": "nationality"
    },
    "personalData": {
      "type": "object",
      "properties": {
        "age": {
          "type": "integer",
          "description": "Please enter your age.",
          "i18n": "personal-data.age"
        },
        "height": {
          "type": "number",
          "i18n": "height"
        },
        "drivingSkill": {
          "type": "number",
          "maximum": 10,
          "minimum": 1,
          "default": 7,
          "i18n": "personal-data.driving"
        }
      },
      "required": ["age", "height"]
    },
    "occupation": {
      "type": "string",
      "i18n": "occupation"
    },
    "postalCode": {
      "type": "string",
      "maxLength": 5,
      "i18n": "postal-code"
    }
  },
  "required": ["occupation", "nationality"]
}""",
        uischema = """{
  "type": "TemplateLayout",
  "lang": "ractive",
  "template": "<section style='display:grid; gap:1.5rem; max-width:72rem; margin:0 auto; padding:1.5rem 0; color:inherit;'><div style='display:grid; gap:1rem;'>{{> hero}}{{> locale}}</div><div style='display:grid; gap:1.5rem; grid-template-columns:repeat(auto-fit,minmax(18rem,1fr)); align-items:start;'>{{> profile}}{{> metrics}}</div><div style='display:grid; gap:1rem;'>{{> summary}}{{> details}}</div></section>",
  "elements": [
    {
      "type": "TemplateLayout",
      "name": "hero",
      "template": "<div style='display:grid; gap:0.75rem; padding:1.5rem; border-radius:1rem; border:1px solid color-mix(in srgb, currentColor 16%, transparent); background:linear-gradient(135deg, color-mix(in srgb, currentColor 10%, transparent), color-mix(in srgb, currentColor 3%, transparent)); box-shadow:0 18px 36px color-mix(in srgb, currentColor 10%, transparent);'><div style='display:flex; gap:0.75rem; align-items:center; flex-wrap:wrap;'><strong style='font-size:1.2rem;'>{{translate('template-layout.hero.title', 'Template Layout Demo')}}</strong><span style='font-size:0.9rem; opacity:0.72;'>{{translate('template-layout.hero.subtitle', 'Ractive partials and named JSON Forms slots')}}</span></div><div style='opacity:0.82; max-width:62ch;'>{{translate('template-layout.hero.body', 'This example mixes templated sections with standard controls, and the content reacts immediately to your form data and locale.')}}</div></div>"
    },
    {
      "type": "TemplateLayout",
      "name": "locale",
      "template": "<div style='display:flex; justify-content:space-between; gap:1rem; align-items:center; flex-wrap:wrap; padding:1rem 1.25rem; border-radius:0.95rem; border:1px solid color-mix(in srgb, currentColor 12%, transparent); background:color-mix(in srgb, currentColor 4%, transparent);'><div style='display:grid; gap:0.25rem;'><span style='font-size:0.95rem; font-weight:600;'>{{translate('template-layout.locale.title', 'Preview language')}}</span><span style='font-size:0.85rem; opacity:0.72;'>{{translate('template-layout.locale.subtitle', 'The buttons below trigger the shared action handler.')}}</span></div><div style='display:flex; gap:0.75rem; flex-wrap:wrap;'>{{> english}}{{> bulgarian}}</div></div>",
      "elements": [
        {
          "type": "Button",
          "name": "english",
          "label": "English",
          "action": "changeLang",
          "color": "secondary",
          "params": {
            "lang": "en"
          }
        },
        {
          "type": "Button",
          "name": "bulgarian",
          "label": "Български",
          "action": "changeLang",
          "color": "primary",
          "params": {
            "lang": "bg"
          }
        }
      ]
    },
    {
      "type": "TemplateLayout",
      "name": "profile",
      "template": "<div style='display:grid; gap:1rem; padding:1.25rem; border-radius:1rem; border:1px solid color-mix(in srgb, currentColor 12%, transparent); background:color-mix(in srgb, currentColor 3%, transparent);'><div style='display:grid; gap:0.25rem;'><strong>{{translate('template-layout.profile.title', 'Profile')}}</strong><span style='font-size:0.85rem; opacity:0.72;'>{{translate('template-layout.profile.subtitle', 'Core identity fields rendered inside a custom layout block.')}}</span></div><div style='display:grid; gap:1rem;'>{{> name}}{{> birthDate}}{{> nationality}}</div></div>",
      "elements": [
        {
          "type": "Control",
          "name": "name",
          "scope": "#/properties/name"
        },
        {
          "type": "Control",
          "name": "birthDate",
          "scope": "#/properties/birthDate"
        },
        {
          "type": "Control",
          "name": "nationality",
          "scope": "#/properties/nationality"
        }
      ]
    },
    {
      "type": "TemplateLayout",
      "name": "metrics",
      "template": "<div style='display:grid; gap:1rem; padding:1.25rem; border-radius:1rem; border:1px solid color-mix(in srgb, currentColor 12%, transparent); background:color-mix(in srgb, currentColor 3%, transparent);'><div style='display:grid; gap:0.25rem;'><strong>{{translate('template-layout.metrics.title', 'Details')}}</strong><span style='font-size:0.85rem; opacity:0.72;'>{{translate('template-layout.metrics.subtitle', 'Nested values and booleans can live in the same templated area.')}}</span></div><div style='display:grid; gap:1rem;'>{{> age}}{{> height}}{{> vegetarian}}</div></div>",
      "elements": [
        {
          "type": "Control",
          "name": "age",
          "scope": "#/properties/personalData/properties/age"
        },
        {
          "type": "Control",
          "name": "height",
          "scope": "#/properties/personalData/properties/height"
        },
        {
          "type": "Control",
          "name": "vegetarian",
          "scope": "#/properties/vegetarian"
        }
      ]
    },
    {
      "type": "TemplateLayout",
      "name": "summary",
      "template": "<div style='display:grid; gap:0.75rem; padding:1.25rem; border-radius:1rem; border:1px dashed color-mix(in srgb, currentColor 22%, transparent); background:linear-gradient(180deg, color-mix(in srgb, currentColor 5%, transparent), transparent);'><strong>{{translate('template-layout.summary.title', 'Live summary')}}</strong><span>{{translate('template-layout.summary.prefix', 'Currently editing the profile for')}} <strong>{{data.name || translate('template-layout.summary.fallbackName', 'an unnamed person')}}</strong>.</span><span>{{translate('template-layout.summary.locale', 'Active locale')}}: <code style='padding:0.2rem 0.45rem; border-radius:999px; border:1px solid color-mix(in srgb, currentColor 18%, transparent); background:color-mix(in srgb, currentColor 6%, transparent); color:inherit;'>{{context.locale || 'en'}}</code></span></div>"
    },
    {
      "type": "VerticalLayout",
      "name": "details",
      "elements": [
        {
          "type": "Control",
          "scope": "#/properties/occupation",
          "options": {
            "suggestion": [
              "Accountant",
              "Engineer",
              "Freelancer",
              "Journalist",
              "Physician",
              "Student",
              "Teacher",
              "Other"
            ]
          }
        },
        {
          "type": "Control",
          "scope": "#/properties/postalCode"
        }
      ]
    }
  ]
}""",
    ),
    ExtendedFixture(
        name = "template-slot",
        label = "Template/Slot Layout",
        data = """{
  "name": "John Doe",
  "vegetarian": false,
  "birthDate": "1985-06-02",
  "personalData": {
    "age": 34,
    "drivingSkill": 7
  },
  "postalCode": "12345"
}""",
        schema = """{
  "__DOLLAR__schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "name": {
      "type": "string",
      "minLength": 3,
      "description": "Please enter your name",
      "i18n": "name"
    },
    "vegetarian": {
      "type": "boolean",
      "i18n": "vegetarian"
    },
    "birthDate": {
      "type": "string",
      "format": "date",
      "i18n": "birth"
    },
    "nationality": {
      "type": "string",
      "enum": ["US", "BG", "DE", "JP", "RU", "Other"],
      "i18n": "nationality"
    },
    "personalData": {
      "type": "object",
      "properties": {
        "age": {
          "type": "integer",
          "description": "Please enter your age.",
          "i18n": "personal-data.age"
        },
        "height": {
          "type": "number",
          "i18n": "height"
        },
        "drivingSkill": {
          "type": "number",
          "maximum": 10,
          "minimum": 1,
          "default": 7,
          "i18n": "personal-data.driving"
        }
      },
      "required": ["age", "height"]
    },
    "occupation": {
      "type": "string",
      "i18n": "occupation"
    },
    "postalCode": {
      "type": "string",
      "maxLength": 5,
      "i18n": "postal-code"
    }
  },
  "required": ["occupation", "nationality"]
}""",
        uischema = """{
  "type": "Template",
  "name": "my-template",
  "elements": [
    {
      "type": "Label",
      "name": "header",
      "text": "Custom Header"
    },
    {
      "type": "Label",
      "name": "footer",
      "text": "Overridden Footer"
    }
  ]
}""",
    ),
).filterNot { it.name in excludedExtendedFixtures }

val extendedRendererExamples: List<ExampleDescription> = extendedFixtures.map { fixture ->
    ExampleDescription(
        name = fixture.name,
        label = fixture.label,
        data = Json.parseToJsonElement(fixture.data.replace("__DOLLAR__", "$")),
        schema = JsonSchema(Json.parseToJsonElement(fixture.schema.replace("__DOLLAR__", "$")) as JsonObject),
        uischema = (Json.parseToJsonElement(fixture.uischema.replace("__DOLLAR__", "$")) as JsonObject).toUiSchemaElement(),
    )
}
