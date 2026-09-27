package io.github.moecax.enable.utils

/**
 * Pure dot-separated version string comparison for the GitHub release
 * update checker. No Android dependencies, so it's plain-JUnit testable.
 */
object VersionComparator {
    /**
     * Returns true only if [remote] is a strictly greater dot-separated
     * version than [current]. Fails closed (returns false) if either
     * string can't be parsed as dot-separated non-negative integers, or if
     * [current] is the "dev" sentinel used by local/debug builds — this
     * never produces a false-positive "update available" prompt.
     */
    fun isNewer(remote: String, current: String): Boolean {
        if (current == "dev") return false
        val remoteParts = parse(remote) ?: return false
        val currentParts = parse(current) ?: return false

        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r != c) return r > c
        }
        return false
    }

    private fun parse(version: String): List<Int>? {
        val stripped = version.trim().removePrefix("v").removePrefix("V")
        if (stripped.isEmpty()) return null
        return stripped.split(".").map { it.toIntOrNull() ?: return null }
    }
}
