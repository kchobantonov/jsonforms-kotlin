package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.util.JsonPointer
import com.chobantonov.jsonfroms.core.util.getInvalidProperty
import com.chobantonov.jsonfroms.core.validation.ErrorObject

/** Presence is indexed once for the displayable validation result, then shared by containers. */
class ContainerErrorIndex private constructor(
    private val prefixCounts: Map<List<String>, Int>,
    private val childCounts: Map<List<String>, Map<String, Int>>,
    private val errorsByPrefix: Map<List<String>, List<ErrorObject>>,
    private val missingDependenciesByTrigger: Map<List<String>, List<ErrorObject>>,
) {
    fun containsAny(scopes: List<List<String>>, currentPath: String = ""): Boolean {
        val context = pathSegments(currentPath)
        return scopes.any { scope -> context + scope in prefixCounts }
    }

    /** Overlapping scopes are reduced to their shortest ancestors so an error counts once. */
    fun countAny(scopes: List<List<String>>, currentPath: String = ""): Int {
        val context = pathSegments(currentPath)
        val minimal = mutableListOf<List<String>>()
        scopes.map { context + it }.distinct().sortedBy { it.size }.forEach { scope ->
            if (minimal.none { ancestor ->
                scope.size >= ancestor.size && scope.subList(0, ancestor.size) == ancestor
            }) minimal += scope
        }
        return minimal.sumOf { prefixCounts[it] ?: 0 }
    }

    /** Immediate-child counts are prepared alongside prefix counts, not rescanned per caller. */
    fun countsByImmediateChild(currentPath: String): Map<String, Int> =
        childCounts[pathSegments(currentPath)].orEmpty()

    /** Retrieve displayable value and descendant errors without a per-cell full-list scan. */
    fun errorsAtOrBelow(currentPath: String): List<ErrorObject> =
        errorsByPrefix[pathSegments(currentPath)].orEmpty()

    /** Descendant errors with no binding in the selected detail description. */
    fun unrepresentedDetailErrors(
        validationScopes: List<List<String>>,
        currentPath: String = "",
    ): List<ErrorObject> {
        val context = pathSegments(currentPath)
        val covered = validationScopes.map { context + it }
        return errorsByPrefix[context].orEmpty().filter { error ->
            val target = errorPathSegments(error)
            target.size > context.size && covered.none { scope -> target.startsWithSegments(scope) }
        }
    }

    /** Dependency errors for a trigger whose missing target has no authored control in this Group. */
    fun unrepresentedDependencyErrors(
        controlScopes: List<List<String>>,
        validationScopes: List<List<String>>,
        currentPath: String = "",
    ): List<ErrorObject> {
        val context = pathSegments(currentPath)
        val covered = validationScopes.map { context + it }
        return controlScopes.map { context + it }.distinct().flatMap { trigger ->
            missingDependenciesByTrigger[trigger].orEmpty().filter { error ->
                val target = errorPathSegments(error)
                covered.none { scope -> target.startsWithSegments(scope) }
            }
        }
    }

    fun unrepresentedDependencyCount(
        controlScopes: List<List<String>>,
        validationScopes: List<List<String>>,
        currentPath: String = "",
    ): Int = unrepresentedDependencyErrors(controlScopes, validationScopes, currentPath).size

    companion object {
        fun build(
            schemaErrors: List<ErrorObject>,
            additionalErrors: List<ErrorObject>,
            mode: ValidationMode,
        ): ContainerErrorIndex {
            val displayable = if (mode == ValidationMode.ValidateAndShow) {
                schemaErrors + additionalErrors
            } else {
                additionalErrors
            }
            val prefixCounts = mutableMapOf<List<String>, Int>()
            val childCounts = mutableMapOf<List<String>, MutableMap<String, Int>>()
            val errorsByPrefix = mutableMapOf<List<String>, MutableList<ErrorObject>>()
            val missingDependenciesByTrigger = mutableMapOf<List<String>, MutableList<ErrorObject>>()
            displayable.forEach { error ->
                val path = errorPathSegments(error)
                dependencyTriggerPath(error)?.let { trigger ->
                    missingDependenciesByTrigger.getOrPut(trigger) { mutableListOf() } += error
                }
                for (depth in 0..path.size) {
                    val prefix = path.take(depth)
                    prefixCounts[prefix] = (prefixCounts[prefix] ?: 0) + 1
                    errorsByPrefix.getOrPut(prefix) { mutableListOf() } += error
                    if (depth < path.size) {
                        val counts = childCounts.getOrPut(prefix) { mutableMapOf() }
                        val child = path[depth]
                        counts[child] = (counts[child] ?: 0) + 1
                    }
                }
            }
            return ContainerErrorIndex(
                prefixCounts,
                childCounts.mapValues { it.value.toMap() },
                errorsByPrefix.mapValues { it.value.toList() },
                missingDependenciesByTrigger.mapValues { it.value.toList() },
            )
        }
    }
}

private fun List<String>.startsWithSegments(prefix: List<String>): Boolean =
    size >= prefix.size && subList(0, prefix.size) == prefix

/** Only direct missing-property failures belong to the containing object's dependency. */
private fun dependencyTriggerPath(error: ErrorObject): List<String>? {
    if (getInvalidProperty(error) == null) return null
    val schemaPath = error.schemaPath ?: return null
    val segments = schemaPath.removePrefix("#").trim('/').split('/')
    val dependencyIndex = segments.indexOfLast { it == "dependencies" ||
        it == "dependentRequired" || it == "dependentSchemas" }
    if (dependencyIndex < 0 || dependencyIndex + 1 >= segments.size) return null
    val directArray = dependencyIndex + 2 == segments.size &&
        error.keyword in setOf("dependencies", "dependentRequired")
    val directSchema = dependencyIndex + 3 == segments.size &&
        segments.last() == "required" && error.keyword == "required"
    if (!directArray && !directSchema) return null
    return pathSegments(error.instancePath) + JsonPointer.decode(segments[dependencyIndex + 1])
}

/** Validation replaces its error lists; reference keys avoid comparing every error on rerender. */
class ContainerErrorIndexCache {
    private var schemaErrors: List<ErrorObject>? = null
    private var additionalErrors: List<ErrorObject>? = null
    private var mode: ValidationMode? = null
    private var index: ContainerErrorIndex? = null

    fun get(
        schemaErrors: List<ErrorObject>,
        additionalErrors: List<ErrorObject>,
        mode: ValidationMode,
    ): ContainerErrorIndex {
        if (index == null || this.schemaErrors !== schemaErrors ||
            this.additionalErrors !== additionalErrors || this.mode != mode
        ) {
            this.schemaErrors = schemaErrors
            this.additionalErrors = additionalErrors
            this.mode = mode
            index = ContainerErrorIndex.build(schemaErrors, additionalErrors, mode)
        }
        return index ?: error("Container error index was not built")
    }
}

/**
 * Groups array errors in one pass so item headers can read their errors without
 * scanning the validation result once per item. The array's own error stays separate.
 */
data class ArrayErrorGroups(
    val descendants: List<ErrorObject>,
    val byIndex: Map<Int, List<ErrorObject>>,
)

fun groupArrayErrors(errors: List<ErrorObject>, arrayPath: String): ArrayErrorGroups {
    val arraySegments = pathSegments(arrayPath)
    val descendants = mutableListOf<ErrorObject>()
    val byIndex = mutableMapOf<Int, MutableList<ErrorObject>>()
    errors.forEach { error ->
        val segments = errorPathSegments(error)
        if (segments.size <= arraySegments.size ||
            segments.subList(0, arraySegments.size) != arraySegments
        ) return@forEach
        descendants += error
        val index = segments[arraySegments.size].toIntOrNull()?.takeIf { it >= 0 }
            ?: return@forEach
        byIndex.getOrPut(index) { mutableListOf() } += error
    }
    return ArrayErrorGroups(descendants, byIndex)
}

internal fun errorPathSegments(error: ErrorObject): List<String> {
    val base = pathSegments(error.dataPath ?: error.instancePath)
    return getInvalidProperty(error)?.let { base + it } ?: base
}

internal fun pathSegments(path: String): List<String> = when {
    path.isEmpty() || path == "#" -> emptyList()
    path.startsWith("/") || path.startsWith("#/") ->
        path.removePrefix("#").split('/').drop(1).map(JsonPointer::decode)
    else -> path.split('.').filter(String::isNotEmpty).map(JsonPointer::decode)
}
