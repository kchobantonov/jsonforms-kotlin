# Job application portable adaptation

The pinned source fixture starts with a Ractive `TemplateLayout` header. This adaptation replaces its browser-only markup with a native horizontal row containing the same three ordinary Button actions: English, Bulgarian, and Toggle theme. The eight-step job form, three named `Template` definitions, schema, data, configuration, and English/Bulgarian translations remain. The host handles those actions as form-only presentation changes; they do not mutate application chrome or form data.
