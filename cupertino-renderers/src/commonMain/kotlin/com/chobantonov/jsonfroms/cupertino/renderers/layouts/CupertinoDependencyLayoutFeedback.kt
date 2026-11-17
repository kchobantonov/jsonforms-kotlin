package com.chobantonov.jsonfroms.cupertino.renderers.layouts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.LocalHandledDependencyErrors
import com.chobantonov.jsonfroms.compose.useJsonForms
import com.chobantonov.jsonfroms.core.i18n.defaultErrorTranslator
import com.chobantonov.jsonfroms.core.i18n.defaultTranslator
import com.chobantonov.jsonfroms.core.i18n.getCombinedErrorMessage
import com.chobantonov.jsonfroms.core.mappers.directLayoutBoundScopes
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.slapps.cupertino.CupertinoText

@Composable
internal fun CupertinoDependencyLayoutFeedback(
    props: ComposeJsonFormsRendererProps,
    elements: List<UiSchemaElement>,
    content: @Composable () -> Unit,
) {
    val scopes = remember(elements) { directLayoutBoundScopes(elements) }
    val handled = LocalHandledDependencyErrors.current
    val errors = useJsonForms().containerErrorIndex.unrepresentedDependencyErrors(
        scopes.controlScopes, scopes.validationScopes, props.path).filterNot { it in handled }
    val i18n = props.state.jsonforms.i18n
    val message = getCombinedErrorMessage(errors,
        i18n.translateError ?: defaultErrorTranslator, i18n.translate ?: defaultTranslator,
        props.schema, props.uischema, props.path)
    val colors = LocalCupertinoColors.current
    Column(Modifier.fillMaxWidth()) {
        if (message.isNotBlank()) CupertinoText(message,
            modifier = Modifier.padding(bottom = 8.dp), color = colors.destructive)
        CompositionLocalProvider(LocalHandledDependencyErrors provides (handled + errors)) {
            content()
        }
    }
}
