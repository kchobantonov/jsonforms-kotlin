package com.chobantonov.jsonfroms.examples.upstream.behavior

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.testers.NOT_APPLICABLE
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject

private fun parseExampleSchema(source: String): JsonSchema =
    JsonSchema(Json.parseToJsonElement(source.replace("__DOLLAR__", "$" )).jsonObject)

private fun parseExampleData(source: String): JsonElement =
    Json.parseToJsonElement(source.replace("__DOLLAR__", "$" ))

private val listWithDetailData = parseExampleData(
    """
    {"orders":[{"customer":{"id":"471201","name":"Sirius Cybernetics Corporation","department":"Complaints Division"},"title":"42 killer robots","ordered":true,"processId":"1890004498","assignee":"Philip J. Fry","status":"ordered","startDate":"2018-06-01","endDate":"2018-08-01"},{"customer":{"id":"471202","name":"Very Big Corporation of America"},"title":"1000 gallons of MomCorp Oil","processId":"1890004499","assignee":"Jen Barber","startDate":"2018-07-01","status":"planned"}]}
    """.trimIndent(),
)

private val listWithDetailSchema = parseExampleSchema(
    """
    {"definitions":{"order":{"type":"object","properties":{"customer":{"type":"object","properties":{"id":{"type":"string"},"name":{"type":"string","format":"email"},"department":{"type":"string"}}},"title":{"type":"string","minLength":5,"title":"Official Title"},"ordered":{"type":"boolean"},"processId":{"type":"number","minimum":0},"assignee":{"type":"string"},"startDate":{"type":"string","format":"date"},"endDate":{"type":"string","format":"date"},"status":{"type":"string","enum":["unordered","planned","ordered"]}},"required":["title"]}},"type":"object","properties":{"orders":{"type":"array","items":{"__DOLLAR__ref":"#/definitions/order"}}}}
    """.trimIndent(),
)

private val listWithDetailDetailOption = buildJsonObject {
    put("type", "VerticalLayout")
    put("elements", buildJsonArray {
        add(
            buildJsonObject {
                put("type", "HorizontalLayout")
                put("elements", buildJsonArray {
                    add(buildJsonObject {
                        put("type", "Control")
                        put("scope", "#/properties/title")
                    })
                    add(buildJsonObject {
                        put("type", "Control")
                        put("scope", "#/properties/processId")
                    })
                })
            },
        )
        add(
            buildJsonObject {
                put("type", "Group")
                put("label", "Customer")
                put("elements", buildJsonArray {
                    add(buildJsonObject {
                        put("type", "Control")
                        put("label", "ID")
                        put("scope", "#/properties/customer/properties/id")
                    })
                    add(buildJsonObject {
                        put("type", "Control")
                        put("label", "Name")
                        put("scope", "#/properties/customer/properties/name")
                    })
                    add(buildJsonObject {
                        put("type", "Control")
                        put("label", "Department")
                        put("scope", "#/properties/customer/properties/department")
                    })
                })
            },
        )
        add(
            buildJsonObject {
                put("type", "VerticalLayout")
                put("elements", buildJsonArray {
                    add(
                        buildJsonObject {
                            put("type", "VerticalLayout")
                            put("elements", buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("type", "HorizontalLayout")
                                        put("elements", buildJsonArray {
                                            add(
                                                buildJsonObject {
                                                    put("type", "Control")
                                                    put("scope", "#/properties/ordered")
                                                    put(
                                                        "options",
                                                        buildJsonObject {
                                                            put("toggle", true)
                                                        },
                                                    )
                                                },
                                            )
                                            add(
                                                buildJsonObject {
                                                    put("type", "Control")
                                                    put("scope", "#/properties/assignee")
                                                },
                                            )
                                        })
                                    },
                                )
                                add(
                                    buildJsonObject {
                                        put("type", "HorizontalLayout")
                                        put("elements", buildJsonArray {
                                            add(
                                                buildJsonObject {
                                                    put("type", "Control")
                                                    put("scope", "#/properties/startDate")
                                                },
                                            )
                                            add(
                                                buildJsonObject {
                                                    put("type", "Control")
                                                    put("scope", "#/properties/endDate")
                                                },
                                            )
                                        })
                                    },
                                )
                                add(
                                    buildJsonObject {
                                        put("type", "Control")
                                        put("scope", "#/properties/status")
                                    },
                                )
                            })
                        },
                    )
                })
            },
        )
    })
}

private val listWithDetailUiSchema = ListWithDetailElement(
    scope = "#/properties/orders",
    options = buildJsonObject {
        put("labelRef", "#/items/properties/customer/properties/name")
        put("detail", listWithDetailDetailOption)
    },
)

private val listWithDetailNoLabelRefUiSchema = ListWithDetailElement(
    scope = "#/properties/orders",
    options = buildJsonObject {
        put("detail", listWithDetailDetailOption)
    },
)

val listWithDetailExample = ExampleDescription(
    name = "list-with-detail",
    label = "List With Detail",
    data = listWithDetailData,
    schema = listWithDetailSchema,
    uischema = listWithDetailUiSchema,
)

val listWithDetailNoLabelRefExample = ExampleDescription(
    name = "list-with-detail-no-labelref",
    label = "List With Detail (No Label Ref)",
    data = listWithDetailData,
    schema = listWithDetailSchema,
    uischema = listWithDetailNoLabelRefUiSchema,
)

private val registeredListWithDetailData = parseExampleData(
    """
    {"warehouseitems":[{"name":"Fantasy Book","buyer":{"email":"buyerA@info.org","age":18},"status":"warehouse"},{"name":"Boardgame","buyer":{"email":"buyerB@info.org","age":45},"status":"shipping"},{"name":"Energy Drink","buyer":{"email":"buyerC@info.org","age":90},"status":"delivered"}]}
    """.trimIndent(),
)

private val registeredListWithDetailSchema = parseExampleSchema(
    """
    {"definitions":{"warehouseitem":{"type":"object","properties":{"name":{"type":"string"},"buyer":{"type":"object","properties":{"email":{"type":"string","format":"email"},"age":{"type":"number"}}},"status":{"type":"string","enum":["warehouse","shipping","delivered"]}},"required":["name"]}},"type":"object","properties":{"warehouseitems":{"type":"array","items":{"__DOLLAR__ref":"#/definitions/warehouseitem"}}}}
    """.trimIndent(),
)

private val registeredListWithDetailUiSchema = ListWithDetailElement(
    scope = "#/properties/warehouseitems",
    options = buildJsonObject {
        put("labelRef", "#/items/properties/name")
    },
)

private val registeredWarehouseItemUiSchema = HorizontalLayout(
    elements = listOf(
        ControlElement(scope = "#/properties/buyer/properties/email"),
        ControlElement(scope = "#/properties/status"),
    ),
)

private val registeredWarehouseItemTester = { _: JsonSchema?, schemaPath: String, _: String ->
    if (schemaPath == "#/properties/warehouseitems/items") 10 else NOT_APPLICABLE
}

val listWithDetailRegisteredExample = ExampleDescription(
    name = "list-with-detail-registered",
    label = "List With Detail (Registered Detail UISchema)",
    data = registeredListWithDetailData,
    schema = registeredListWithDetailSchema,
    uischema = registeredListWithDetailUiSchema,
    uischemas = listOf(
        JsonFormsUISchemaRegistryEntry(
            tester = registeredWarehouseItemTester,
            uischema = registeredWarehouseItemUiSchema,
        ),
    ),
)

private val primitiveStringListWithDetailData = parseExampleData(
    """
    {"an-array-of-strings":["foo","bar","foobar"]}
    """.trimIndent(),
)

private val primitiveStringListWithDetailSchema = parseExampleSchema(
    """
    {"type":"object","properties":{"an-array-of-strings":{"type":"array","items":{"type":"string"}}}}
    """.trimIndent(),
)

val listWithDetailPrimitiveStringExample = ExampleDescription(
    name = "list-with-detail-primitive-string",
    label = "List With Detail primitive (string)",
    data = primitiveStringListWithDetailData,
    schema = primitiveStringListWithDetailSchema,
    uischema = ListWithDetailElement(scope = "#/properties/an-array-of-strings"),
)

private val primitiveNumberListWithDetailData = parseExampleData(
    """
    {"an-array-of-numbers":[1,2,3]}
    """.trimIndent(),
)

private val primitiveNumberListWithDetailSchema = parseExampleSchema(
    """
    {"type":"object","properties":{"an-array-of-numbers":{"type":"array","items":{"type":"number"}}}}
    """.trimIndent(),
)

val listWithDetailPrimitiveNumberExample = ExampleDescription(
    name = "list-with-detail-primitive-number",
    label = "List With Detail primitive (number)",
    data = primitiveNumberListWithDetailData,
    schema = primitiveNumberListWithDetailSchema,
    uischema = ListWithDetailElement(scope = "#/properties/an-array-of-numbers"),
)
