package com.reader.core.common

import com.reader.core.common.dispatcher.StandardDispatchersProvider
import com.reader.core.common.io.NioFileReader
import com.reader.core.common.io.useNioReader
import com.reader.core.common.result.AppError
import com.reader.core.common.result.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.charset.StandardCharsets

class CommonTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testStandardDispatchersProvider() {
        val provider = StandardDispatchersProvider()
        assertNotNull(provider.io)
        assertNotNull(provider.default)
        assertNotNull(provider.unconfined)
    }

    @Test
    fun testAppResultSuccessFlow() {
        val result: AppResult<String> = AppResult.Success("Hello Reader")
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertEquals("Hello Reader", result.getOrNull())
        assertEquals("Hello Reader", result.getOrDefault("Fallback"))

        val mapped = result.map { it.length }
        assertTrue(mapped.isSuccess)
        assertEquals(12, mapped.getOrNull())
    }

    @Test
    fun testAppResultErrorFlow() {
        val errorResult: AppResult<String> = AppResult.Error(AppError.FileNotFound("/fake/path"))
        assertTrue(errorResult.isError)
        assertFalse(errorResult.isSuccess)
        assertNull(errorResult.getOrNull())
        assertEquals("Default", errorResult.getOrDefault("Default"))

        var errorCaught = false
        errorResult.onError { error, _ ->
            errorCaught = true
            assertTrue(error is AppError.FileNotFound)
        }
        assertTrue(errorCaught)
    }

    @Test
    fun testAppResultRunCatching() {
        val success = AppResult.runCatchingAppResult { 42 }
        assertTrue(success.isSuccess)
        assertEquals(42, success.getOrNull())

        val failure = AppResult.runCatchingAppResult {
            throw java.io.FileNotFoundException("Test file missing")
        }
        assertTrue(failure.isError)
        val err = (failure as AppResult.Error).error
        assertTrue(err is AppError.FileNotFound)
    }

    @Test
    fun testNioFileReaderAndChunking() {
        val sampleText = "第一章 宴桃园豪杰三结义\n滚滚长江东逝水，浪花淘尽英雄。是非成败转头空。"
        val testFile = tempFolder.newFile("test_novel.txt")
        testFile.writeText(sampleText, StandardCharsets.UTF_8)

        testFile.useNioReader { reader ->
            assertEquals(testFile.length(), reader.fileSize)

            // Test sample read
            val sample = reader.readSampleBytes(32)
            assertTrue(sample.isNotEmpty())

            // Test offset text range read
            val readAll = reader.readTextRange(0L, reader.fileSize.toInt(), StandardCharsets.UTF_8)
            assertEquals(sampleText, readAll)

            // Test chunk iteration
            var totalBytesIterated = 0
            reader.forEachChunk(chunkSize = 16) { _, buffer, count ->
                totalBytesIterated += count
                true
            }
            assertEquals(reader.fileSize.toInt(), totalBytesIterated)

            // Test memory-mapped buffer
            val mapped = reader.mapReadOnly(0L, reader.fileSize)
            assertEquals(reader.fileSize.toInt(), mapped.remaining())
        }
    }
}
