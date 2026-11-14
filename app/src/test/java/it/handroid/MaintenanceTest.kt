package it.handroid

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenanceTest {
    @Test
    fun versioneNuova() {
        assertTrue(Maintenance.isNewer("0.2.0", "0.1.0-m1"))
        assertTrue(Maintenance.isNewer("0.1.1", "0.1.0-m1"))
        assertTrue(Maintenance.isNewer("1.0", "0.9.9"))
    }

    @Test
    fun versioneUgualeOVecchia() {
        assertFalse(Maintenance.isNewer("0.1.0", "0.1.0-m1"))
        assertFalse(Maintenance.isNewer("0.0.9", "0.1.0-m1"))
        assertFalse(Maintenance.isNewer("0.1", "0.1.0"))
    }
}
