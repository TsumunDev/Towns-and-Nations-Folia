package org.leralix.tan.coroutines

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class TanCoroutinesTest {
    
    @BeforeEach
    fun setUp() {
        TanCoroutines.initialize()
    }
    
    @AfterEach
    fun tearDown() {
        TanCoroutines.shutdown()
    }
    
    @Test
    fun `initialize should setup coroutine scope`() {
        // Already initialized in setUp
        assertNotNull(TanCoroutines.scope)
    }
    
    @Test
    fun `launch should execute coroutine`() = runTest {
        val counter = AtomicInteger(0)
        
        val job = TanCoroutines.launch {
            counter.incrementAndGet()
        }
        
        job.join()
        assertEquals(1, counter.get())
    }
    
    @Test
    fun `async should return deferred result`() = runTest {
        val deferred = TanCoroutines.async {
            42
        }
        
        assertEquals(42, deferred.await())
    }
    
    @Test
    fun `awaitTan should convert CompletableFuture to suspend`() = runTest {
        val future = CompletableFuture.supplyAsync { "Hello Kotlin" }
        
        val result = future.awaitTan()
        
        assertEquals("Hello Kotlin", result)
    }
    
    @Test
    fun `asFuture should convert suspend to CompletableFuture`() = runBlocking {
        val future = TanCoroutines.scope.asFuture {
            "Hello Java"
        }
        
        assertEquals("Hello Java", future.get())
    }
    
    @Test
    fun `multiple launches should work concurrently`() = runTest {
        val counter = AtomicInteger(0)
        
        val jobs = (1..10).map {
            TanCoroutines.launch {
                counter.incrementAndGet()
            }
        }
        
        jobs.forEach { it.join() }
        assertEquals(10, counter.get())
    }
    
    @Test
    fun `awaitTan should handle exceptions`() = runTest {
        val future = CompletableFuture<String>().apply {
            completeExceptionally(RuntimeException("Test error"))
        }
        
        val result = runCatching { future.awaitTan() }
        
        assertTrue(result.isFailure)
        assertEquals("Test error", result.exceptionOrNull()?.message)
    }
}
