package com.chobantonov.jsonfroms.examples

import com.chobantonov.jsonfroms.examples.examples.additionalErrorsExample
import com.chobantonov.jsonfroms.examples.examples.allOfExample
import com.chobantonov.jsonfroms.examples.examples.anyOfExample
import com.chobantonov.jsonfroms.examples.examples.arrayWithDetailExample
import com.chobantonov.jsonfroms.examples.examples.arraysExample
import com.chobantonov.jsonfroms.examples.examples.categorizationExample
import com.chobantonov.jsonfroms.examples.examples.categorizationStepperExample
import com.chobantonov.jsonfroms.examples.examples.controlOptionsExample
import com.chobantonov.jsonfroms.examples.examples.datesExample
import com.chobantonov.jsonfroms.examples.examples.defaultExample
import com.chobantonov.jsonfroms.examples.examples.enumExample
import com.chobantonov.jsonfroms.examples.examples.enumInArrayExample
import com.chobantonov.jsonfroms.examples.examples.generateExample
import com.chobantonov.jsonfroms.examples.examples.generateUiExample
import com.chobantonov.jsonfroms.examples.examples.hugeExample
import com.chobantonov.jsonfroms.examples.examples.i18nExample
import com.chobantonov.jsonfroms.examples.examples.ifThenElseExample
import com.chobantonov.jsonfroms.examples.examples.layoutExamples
import com.chobantonov.jsonfroms.examples.examples.listWithDetailExample
import com.chobantonov.jsonfroms.examples.examples.listWithDetailNoLabelRefExample
import com.chobantonov.jsonfroms.examples.examples.listWithDetailPrimitiveNumberExample
import com.chobantonov.jsonfroms.examples.examples.listWithDetailPrimitiveStringExample
import com.chobantonov.jsonfroms.examples.examples.listWithDetailRegisteredExample
import com.chobantonov.jsonfroms.examples.examples.loginExample
import com.chobantonov.jsonfroms.examples.examples.numbersExample
import com.chobantonov.jsonfroms.examples.examples.objectExample
import com.chobantonov.jsonfroms.examples.examples.objectRootExample
import com.chobantonov.jsonfroms.examples.examples.onChangeExample
import com.chobantonov.jsonfroms.examples.examples.oneOfExample
import com.chobantonov.jsonfroms.examples.examples.personExample
import com.chobantonov.jsonfroms.examples.examples.radioGroupExample
import com.chobantonov.jsonfroms.examples.examples.ruleExample
import com.chobantonov.jsonfroms.examples.examples.ruleInheritanceExample
import com.chobantonov.jsonfroms.examples.examples.scopeExample
import com.chobantonov.jsonfroms.examples.examples.stringArrayExample
import com.chobantonov.jsonfroms.examples.examples.stringExample
import com.chobantonov.jsonfroms.examples.examples.textExample

private val builtInExamples =
    listOf(
        additionalErrorsExample,
        allOfExample,
        anyOfExample,
        arrayWithDetailExample,
        arraysExample,
        categorizationExample,
        categorizationStepperExample,
        controlOptionsExample,
        datesExample,
        defaultExample,
        enumExample,
        enumInArrayExample,
        generateExample,
        generateUiExample,
        hugeExample,
        i18nExample,
        ifThenElseExample,
        listWithDetailExample,
        listWithDetailNoLabelRefExample,
        listWithDetailRegisteredExample,
        listWithDetailPrimitiveStringExample,
        listWithDetailPrimitiveNumberExample,
        loginExample,
        numbersExample,
        objectExample,
        objectRootExample,
        onChangeExample,
        oneOfExample,
        personExample,
        radioGroupExample,
        ruleExample,
        ruleInheritanceExample,
        scopeExample,
        stringArrayExample,
        stringExample,
        textExample,
    ) + layoutExamples + upstreamSnapshotExamples

fun registerBuiltInExamples() {
    clearExamples()
    registerExamples(builtInExamples)
}

fun getBuiltInExamples(): List<ExampleDescription> {
    registerBuiltInExamples()
    return getExamples()
}
