package com.kduniv.aimong.core.util

/**
 * `minimumAppVersion`(예: 1.0.0)과 앱 [currentVersion] 비교.
 */
object AppVersionUtils {

    fun isBelowMinimum(currentVersion: String, minimumAppVersion: String?): Boolean {
        val min = minimumAppVersion?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        val cur = parseSemver(currentVersion) ?: return false
        val req = parseSemver(min) ?: return false
        return compareSemver(cur, req) < 0
    }

    private fun parseSemver(v: String): List<Int>? {
        val core = v.trim().substringBefore('-').substringBefore('+')
        val parts = core.split('.').mapNotNull { it.toIntOrNull() }
        if (parts.isEmpty()) return null
        return parts
    }

    private fun compareSemver(a: List<Int>, b: List<Int>): Int {
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }
}
