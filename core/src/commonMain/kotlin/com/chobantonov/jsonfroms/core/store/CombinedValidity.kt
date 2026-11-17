package com.chobantonov.jsonfroms.core.store

/**
 * Validity is independent of error presentation. A hidden schema error still invalidates the
 * document, and an empty error list under NoValidation does not establish validity.
 */
enum class CombinedValidityStatus {
    Valid,
    Invalid,
    Pending,
    Stale,
    Unvalidated,
}

data class CombinedValidity(
    val status: CombinedValidityStatus,
    val schemaErrorCount: Int,
    val additionalErrorCount: Int,
    val pending: Boolean = false,
    val stale: Boolean = false,
    val validationMode: ValidationMode = ValidationMode.ValidateAndShow,
)

/**
 * [pending] and [stale] are supplied by integrations that perform asynchronous validation.
 * Confirmed errors take precedence; otherwise pending work prevents a valid result.
 */
fun getCombinedValidity(
    state: JsonFormsState<*, *>,
    pending: Boolean = false,
    stale: Boolean = false,
): CombinedValidity = combinedValidity(state, state.jsonforms.core.additionalErrors.size, pending, stale)

/** Uses the live owner store, including host and renderer errors, as the additional-error source. */
fun getCombinedValidity(
    state: JsonFormsState<*, *>,
    ownedErrors: OwnedAdditionalErrorStore,
    stale: Boolean = false,
): CombinedValidity = combinedValidity(state, ownedErrors.errors.size, ownedErrors.pending, stale)

private fun combinedValidity(
    state: JsonFormsState<*, *>,
    additionalCount: Int,
    pending: Boolean,
    stale: Boolean,
): CombinedValidity {
    val core = state.jsonforms.core
    val schemaCount = core.errors.size
    val status = when {
        schemaCount + additionalCount > 0 -> CombinedValidityStatus.Invalid
        pending -> CombinedValidityStatus.Pending
        stale -> CombinedValidityStatus.Stale
        core.validationMode == ValidationMode.NoValidation -> CombinedValidityStatus.Unvalidated
        else -> CombinedValidityStatus.Valid
    }
    return CombinedValidity(status, schemaCount, additionalCount, pending, stale, core.validationMode)
}
