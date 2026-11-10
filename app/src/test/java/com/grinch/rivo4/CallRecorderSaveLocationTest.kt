package com.grinch.rivo4

import com.grinch.rivo4.controller.CallRecorder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CallRecorderSaveLocationTest {

    @Test
    fun testIsWritableDirectorySuccess() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "rivo_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        try {
            assertTrue(CallRecorder.isWritableDirectory(tempDir))
            assertTrue(tempDir.exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testIsWritableDirectoryCreatesAndKeepsWritableDir() {
        val tempParent = File(System.getProperty("java.io.tmpdir"), "rivo_test_parent_${System.currentTimeMillis()}")
        tempParent.mkdirs()
        val nonExistentChild = File(tempParent, "child_subfolder")
        try {
            assertFalse(nonExistentChild.exists())
            assertTrue(CallRecorder.isWritableDirectory(nonExistentChild))
            assertTrue(nonExistentChild.exists())
        } finally {
            tempParent.deleteRecursively()
        }
    }

    @Test
    fun testIsWritableDirectoryCleansUpOnProbeFailure() {
        val nonExistentDirUnderInvalidPath = File("/proc/non_existent_dir_${System.currentTimeMillis()}")
        val result = CallRecorder.isWritableDirectory(nonExistentDirUnderInvalidPath)
        assertFalse(result)
        assertFalse(nonExistentDirUnderInvalidPath.exists())
    }
}
