package br.com.imoveisregla.core.data

import org.junit.Assert.fail

/** Suspend-friendly assertThrows. */
suspend inline fun <reified T : Throwable> expectThrows(crossinline block: suspend () -> Unit): T {
    try {
        block()
    } catch (e: Throwable) {
        if (e is T) return e
        fail("Expected ${T::class.java.simpleName} but got ${e::class.java.name}: ${e.message}")
    }
    fail("Expected ${T::class.java.simpleName} but nothing was thrown")
    throw AssertionError()
}
