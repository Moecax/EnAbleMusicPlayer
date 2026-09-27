package io.github.moecax.enable.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseNotesFormatterTest {

    @Test
    fun `headings lose their hash markers`() {
        assertEquals("Fixes", ReleaseNotesFormatter.toPlainText("### Fixes"))
    }

    @Test
    fun `list items become bullets and keep indentation`() {
        assertEquals(
            "• one\n  • two",
            ReleaseNotesFormatter.toPlainText("- one\n  * two")
        )
    }

    @Test
    fun `links keep only their text`() {
        assertEquals(
            "Automated release from commit abc123",
            ReleaseNotesFormatter.toPlainText(
                "Automated release from commit [`abc123`](https://github.com/x/y/commit/abc123)"
            )
        )
    }

    @Test
    fun `bold markers are stripped`() {
        assertEquals("fix: thing", ReleaseNotesFormatter.toPlainText("**fix: thing**"))
    }

    @Test
    fun `changelog section renders as plain text`() {
        val markdown = """

            ### Fixes

            - persist and restore last-played song

        """.trimIndent()
        assertEquals(
            "Fixes\n\n• persist and restore last-played song",
            ReleaseNotesFormatter.toPlainText(markdown)
        )
    }
}
