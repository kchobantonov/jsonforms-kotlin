package com.chobantonov.jsonfroms.core.store

import com.chobantonov.jsonfroms.core.validation.ErrorObject

/** Additional errors stay with their producer; renderer IDs must identify a stable editor instance. */
data class OwnedAdditionalErrorStore(
    val hostErrors: List<ErrorObject> = emptyList(),
    private val owners: Map<String, OwnedErrors> = emptyMap(),
    private val pendingOwners: Set<String> = emptySet(),
) {
    data class OwnedErrors(val targetPath: String, val errors: List<ErrorObject>)

    val errors: List<ErrorObject> get() = hostErrors + owners.values.flatMap(OwnedErrors::errors)
    val pending: Boolean get() = pendingOwners.isNotEmpty()

    /** Return producers whose target overlaps an edited JSON Pointer path. */
    fun ownersAffectedBy(path: String): Set<String> = owners.filterValues { entry ->
        pathsOverlap(entry.targetPath, path)
    }.keys

    private fun pathsOverlap(first: String, second: String): Boolean =
        first == second || first.isEmpty() || second.isEmpty() ||
            first.startsWith("$second/") || second.startsWith("$first/")

    fun replaceHost(errors: List<ErrorObject>): OwnedAdditionalErrorStore = copy(hostErrors = errors)

    fun publish(ownerId: String, targetPath: String, errors: List<ErrorObject>): OwnedAdditionalErrorStore =
        if (errors.isEmpty()) clear(ownerId) else copy(
            owners = owners + (ownerId to OwnedErrors(targetPath, errors)),
            pendingOwners = pendingOwners - ownerId,
        )

    fun setPending(ownerId: String, value: Boolean): OwnedAdditionalErrorStore = copy(
        pendingOwners = if (value) pendingOwners + ownerId else pendingOwners - ownerId,
    )

    fun clear(ownerId: String): OwnedAdditionalErrorStore = copy(
        owners = owners - ownerId,
        pendingOwners = pendingOwners - ownerId,
    )

    /** Call only after the host has identified the same logical target at [newTargetPath]. */
    fun move(ownerId: String, newTargetPath: String): OwnedAdditionalErrorStore {
        val entry = owners[ownerId] ?: return this
        val old = entry.targetPath
        val moved = entry.errors.map { error ->
            val suffix = when {
                error.instancePath == old -> ""
                old.isEmpty() -> error.instancePath
                error.instancePath.startsWith("$old/") -> error.instancePath.removePrefix(old)
                else -> return@map error
            }
            error.copy(instancePath = newTargetPath + suffix)
        }
        return copy(owners = owners + (ownerId to entry.copy(targetPath = newTargetPath, errors = moved)))
    }
}
