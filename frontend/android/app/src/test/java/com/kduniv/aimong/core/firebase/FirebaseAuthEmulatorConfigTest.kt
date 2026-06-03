package com.kduniv.aimong.core.firebase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseAuthEmulatorConfigTest {
    @Test
    fun debugBuildUsesAndroidEmulatorHostAndDefaultAuthPort() {
        val config = FirebaseAuthEmulatorConfig.create(isDebugBuild = true)

        assertTrue(config.enabled)
        assertEquals("10.0.2.2", config.host)
        assertEquals(9099, config.port)
    }

    @Test
    fun releaseBuildDoesNotUseAuthEmulator() {
        val config = FirebaseAuthEmulatorConfig.create(isDebugBuild = false)

        assertFalse(config.enabled)
    }
}
