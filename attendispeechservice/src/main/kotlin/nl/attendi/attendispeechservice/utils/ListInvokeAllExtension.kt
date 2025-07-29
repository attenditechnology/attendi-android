package nl.attendi.attendispeechservice.utils

/**
 * Invokes all suspending functions in the list sequentially.
 *
 * This extension creates a snapshot of the list to avoid concurrent modification issues,
 * then calls each suspending function one after another.
 */
internal suspend fun List<suspend () -> Unit>.invokeAll() = this.toList().forEach { it() }

/**
 * Invokes all suspending functions in the iterable sequentially, passing the given argument [arg].
 *
 * This extension creates a snapshot of the iterable to avoid concurrent modification issues,
 * then calls each suspending function one after another with the provided argument.
 *
 * @param arg The argument to pass to each suspending function.
 */
internal suspend fun <T> Iterable<suspend (T) -> Unit>.invokeAll(arg: T) {
    this.toList().forEach { it(arg) }
}