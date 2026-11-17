package com.chobantonov.jsonfroms.core.validation

import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class CronFormatTest {
    @Test fun sixFieldDialectAndAliases() {
        for (alias in listOf("@yearly", "@annually", "@monthly", "@weekly",
                "@daily", "@midnight", "@hourly")) {
            assertEquals(null, cronProblem(alias), alias)
        }
        assertEquals(listOf("0", "0", "0", "*", "*", "*"), splitCron("@DAILY"))
        assertEquals("sixFields", cronProblem("0 9 * * *"))
        assertEquals("sixFields", cronProblem("0 0 9 * * * 2030"))
        assertEquals("sixFields", cronProblem("@reboot"))
        assertEquals("sixFields", cronProblem(""))
        assertEquals(null, cronProblem("  0 0 9 * * *  "))
    }

    @Test fun rangesStepsNamesAndDaySelectors() {
        assertEquals(setOf(0, 15, 30, 45), cronFieldValues("0/15", CronField.Minute))
        assertEquals(setOf(9, 13, 17), cronFieldValues("9-17/4", CronField.Hour))
        assertEquals(setOf(1, 5, 6, 7, 20),
            cronFieldValues("1,5-7,20", CronField.DayOfMonth))
        assertEquals(setOf(1, 2, 3, 4, 5),
            cronFieldValues("MON-FRI", CronField.DayOfWeek))
        assertEquals(setOf(0), cronFieldValues("0,7", CronField.DayOfWeek))
        assertEquals(setOf(0, 2, 4, 6), cronFieldValues("7/2", CronField.DayOfWeek))
        assertEquals(null, cronFieldValues("FRI-7", CronField.DayOfWeek))
        assertEquals(null, cronProblem("0 0 9 ? * FRI-7"))
        assertEquals(setOf(1, 12), cronFieldValues("JAN,DEC", CronField.Month))
        assertEquals(setOf(0), cronFieldValues("0/999999999999999999999", CronField.Minute))
        assertEquals(setOf(1), cronFieldValues("*/999999999999999999999", CronField.Month))
        for (expression in listOf("0 0 9 L * ?", "0 0 9 LW * ?",
                "0 0 9 L-3 * ?", "0 0 9 L-0 * ?", "0 0 9 15W * ?", "0 0 9 ? * FRI#2",
                "0 0 9 ? * 5L", "0 0 9 ? * FRI-MON",
                "0 0 9 ? * FRI-MON/999999999999999999999")) {
            assertEquals(null, cronProblem(expression), expression)
        }
        assertEquals(null, cronFieldValues("L", CronField.DayOfMonth))
        assertEquals(null, cronFieldValues("FRI#2", CronField.DayOfWeek))
    }

    @Test fun malformedFieldsReportTheirOwnPosition() {
        assertEquals("hour", cronProblem("0 0 25 * * *"))
        assertEquals("minute", cronProblem("0 99 * * * *"))
        assertEquals("month", cronProblem("0 0 9 * 13 *"))
        assertEquals("dayOfWeek", cronProblem("0 0 9 * * NOTADAY"))
        assertEquals("minute", cronProblem("0 ? 9 * * *"))
        assertEquals("dayOfMonth", cronProblem("0 0 9 32W * ?"))
        assertEquals("dayOfWeek", cronProblem("0 0 9 ? * FRI#6"))
        assertEquals("minute", cronProblem("0 0/0 9 * * *"))
        assertEquals("minute", cronProblem("0 0/+1 9 * * *"))
        assertEquals("dayOfMonth", cronProblem("0 0 9 L-31 * ?"))
    }

    @Test fun schemaFormatValidatesHiddenCronWithoutRendererErrors() {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"job":{"type":"string","format":"cron"}}
        }""").jsonObject)
        val validator = OptimumCodeJsonSchemaValidator()
        for (valid in listOf("@weekly", "0 0 9 ? * FRI#2", "0 0 9 * * *")) {
            assertEquals(emptyList(), validator.validate(schema,
                Json.parseToJsonElement("""{"job":"$valid"}""")), valid)
        }
        val errors = validator.validate(schema,
            Json.parseToJsonElement("""{"job":"0 9 * * *"}"""))
        assertEquals("/job", errors.single().instancePath)
        assertEquals("format", errors.single().keyword)
        val uiOnlySchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"job":{"type":"string"}}
        }""").jsonObject)
        assertEquals(emptyList(), validator.validate(uiOnlySchema,
            Json.parseToJsonElement("""{"job":"0 9 * * *"}""")))
    }
}
