package io.github.ulitink.hprof

import field_values.A
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException

internal class LoaderTest {

    private fun loadDump(hprofPath: String): Collection<Any?> {
        val file = File(hprofPath)
        if (!file.exists()) throw IOException("File not found")
        val loader = HprofLoader()
        HprofParser(DataInputStream(FileInputStream(file)), loader).parse()
        val applier = HprofApplier(loader)
        applier.loadHeapFromHprof()
        return applier.instances.values
    }

    @Test
    @Throws(Exception::class)
    fun testFieldValues() {
        val objects = loadDump("src/test/hprofs/field_values.hprof")
        var aFound = false
        for (o in objects) {
            if (o !is A) continue
            assertFalse(aFound)
            aFound = true
            assertEquals(o.myInt, 42)
            assertEquals(o.myLong, 419)
            assertEquals(o.myChar, 'c')
        }
        assertTrue(aFound)
    }
}
