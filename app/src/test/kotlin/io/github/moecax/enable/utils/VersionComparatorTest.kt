package io.github.moecax.enable.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun `equal versions are not newer`() {
        assertFalse(VersionComparator.isNewer("v0.1.12", "v0.1.12"))
    }

    @Test
    fun `higher patch version is newer`() {
        assertTrue(VersionComparator.isNewer("v0.1.13", "v0.1.12"))
    }

    @Test
    fun `lower patch version is not newer`() {
        assertFalse(VersionComparator.isNewer("v0.1.11", "v0.1.12"))
    }

    @Test
    fun `higher major version is newer regardless of minor patch`() {
        assertTrue(VersionComparator.isNewer("v1.0.0", "v0.9.9"))
    }

    @Test
    fun `leading v is optional and case-insensitive`() {
        assertTrue(VersionComparator.isNewer("1.2.3", "V1.2.2"))
    }

    @Test
    fun `shorter version is zero-padded for comparison`() {
        assertFalse(VersionComparator.isNewer("v1.2", "v1.2.0"))
        assertTrue(VersionComparator.isNewer("v1.3", "v1.2.0"))
        assertFalse(VersionComparator.isNewer("v1.2.0", "v1.2"))
    }

    @Test
    fun `dev build is never flagged as outdated`() {
        assertFalse(VersionComparator.isNewer("v99.0.0", "dev"))
    }

    @Test
    fun `malformed remote version fails closed`() {
        assertFalse(VersionComparator.isNewer("latest-build", "v0.1.12"))
    }

    @Test
    fun `malformed current version fails closed`() {
        assertFalse(VersionComparator.isNewer("v0.1.13", "not-a-version"))
    }
}
