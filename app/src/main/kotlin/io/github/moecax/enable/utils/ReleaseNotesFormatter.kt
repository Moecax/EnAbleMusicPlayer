package io.github.moecax.enable.utils

/**
 * Flattens GitHub release-note Markdown into plain text for the changelog
 * dialog. No Android dependencies, so it's plain-JUnit testable.
 */
object ReleaseNotesFormatter {
    private val heading = Regex("""^\s{0,3}#{1,6}\s+""")
    private val bullet = Regex("""^(\s*)[-*+]\s+""")
    private val link = Regex("""\[([^\]]*)]\([^)]*\)""")
    private val bold = Regex("""(\*\*|__)(.+?)\1""")
    private val code = Regex("""`([^`]*)`""")

    fun toPlainText(markdown: String): String =
        markdown.lines().joinToString("\n") { line ->
            line
                .replace(heading, "")
                .replace(bullet, "$1• ")
                .replace(link, "$1")
                .replace(bold, "$2")
                .replace(code, "$1")
        }.trim()
}
