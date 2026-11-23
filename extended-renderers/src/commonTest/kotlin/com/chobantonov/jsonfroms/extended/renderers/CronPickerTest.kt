package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.core.validation.CronField
import kotlin.test.Test
import kotlin.test.assertEquals

class CronPickerTest {
    @Test
    fun periodsExposeOnlyRelevantFields() {
        assertEquals(listOf(CronField.DayOfWeek, CronField.Hour, CronField.Minute, CronField.Second),
            cronPeriodFields(CronPeriod.Week))
        assertEquals(listOf(CronField.Month, CronField.DayOfMonth, CronField.Hour, CronField.Minute,
            CronField.Second), cronPeriodFields(CronPeriod.Year))
        assertEquals(CronPeriod.Day, cronPeriodOf("0 0 9 * * *"))
        assertEquals(CronPeriod.Year, cronPeriodOf("@yearly"))
    }

    @Test
    fun periodSwitchDropsHiddenConstraints() {
        assertEquals(listOf("0", "0", "9", "*", "*", "MON"),
            cronForPeriod("0 0 9 15 6 MON", CronPeriod.Week))
        assertEquals(listOf("0", "0", "9", "*", "*", "*"),
            cronForPeriod("0 0 9 15 6 MON", CronPeriod.Day))
    }

    @Test
    fun editingUpperTimePinsOnlyWildcardLowerTime() {
        val initial = List(6) { "*" }
        assertEquals(listOf("0", "0", "9", "*", "*", "*"),
            cronEditedField(initial, CronField.Hour, "9"))
        assertEquals(listOf("0", "15", "*", "*", "*", "*"),
            cronEditedField(initial, CronField.Minute, "15"))
        assertEquals(listOf("30", "0", "9", "*", "*", "*"),
            cronEditedField(listOf("30", "*", "*", "*", "*", "*"), CronField.Hour, "9"))
        assertEquals(listOf("*", "*", "invalid", "*", "*", "*"),
            cronEditedField(initial, CronField.Hour, "invalid"))
    }

    @Test
    fun pickerFormatsChoiceSetsAndPreservesEquivalentExpressions() {
        assertEquals("0-2", cronFormatChoices(CronField.Second, listOf(0, 1, 2)))
        assertEquals("*/15", cronFormatChoices(CronField.Minute, listOf(0, 15, 30, 45)))
        assertEquals("MON,WED", cronFormatChoices(CronField.DayOfWeek, listOf(1, 3)))
        assertEquals("*", cronFormatChoices(CronField.Hour, emptyList()))
        assertEquals((0..6).toList(), cronChoiceValues(CronField.DayOfWeek))
        assertEquals(true, cronSameExpression("@daily", "0 0 0 * * *"))
        assertEquals(true, cronSameExpression("0 0/15 * * * *", "0 0,15,30,45 * * * *"))
    }
}
