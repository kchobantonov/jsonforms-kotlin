package com.chobantonov.jsonfroms.core.store

interface Action<T> {
    val type: T
}

interface AnyAction : Action<String> {
    val extraProps: Map<String, Any?>
}

fun interface Dispatch<A> {
    fun dispatch(action: A): A
}

fun interface Unsubscribe {
    fun unsubscribe()
}

fun interface Reducer<S, A> {
    operator fun invoke(state: S?, action: A): S
}

interface Store<S> {
    fun getState(): S
}
