package no.thorbear.sonar.plugins.android

import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidPluginTest {

    @Test
    fun getExtensions() {
        assertEquals(5, AndroidPlugin().extensions.size)
    }
}