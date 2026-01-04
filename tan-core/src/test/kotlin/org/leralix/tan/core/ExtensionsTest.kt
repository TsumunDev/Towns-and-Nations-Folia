package org.leralix.tan.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExtensionsTest {
    
    @Test
    fun `translateColorCodes should convert ampersand to section`() {
        val input = "&aHello &bWorld"
        val expected = "§aHello §bWorld"
        
        assertEquals(expected, input.translateColorCodes())
    }
    
    @Test
    fun `stripColorCodes should remove all color codes`() {
        val input = "§aHello §bWorld"
        val expected = "Hello World"
        
        assertEquals(expected, input.stripColorCodes())
    }
    
    @Test
    fun `isValidTerritoryName should validate name length`() {
        assertFalse("AB".isValidTerritoryName()) // Too short
        assertTrue("ABC".isValidTerritoryName()) // Minimum length
        assertTrue("A".repeat(24).isValidTerritoryName()) // Maximum length
        assertFalse("A".repeat(25).isValidTerritoryName()) // Too long
    }
    
    @Test
    fun `isValidTerritoryName should allow alphanumeric and spaces`() {
        assertTrue("My Town 123".isValidTerritoryName())
        assertTrue("Town_Name".isValidTerritoryName())
        assertFalse("Town@Name".isValidTerritoryName()) // Special char
        assertFalse("Town#Name".isValidTerritoryName()) // Special char
    }
    
    @Test
    fun `titleCase should capitalize first letter of each word`() {
        assertEquals("Hello World", "hello world".titleCase())
        assertEquals("My Cool Town", "my cool town".titleCase())
        assertEquals("Test", "TEST".titleCase())
    }
    
    @Test
    fun `ifNotNull should execute block when value is not null`() {
        val value: String? = "Hello"
        var executed = false
        
        value.ifNotNull { 
            executed = true
            it.length
        }
        
        assertTrue(executed)
    }
    
    @Test
    fun `ifNotNull should not execute block when value is null`() {
        val value: String? = null
        var executed = false
        
        value.ifNotNull { 
            executed = true
            it.length
        }
        
        assertFalse(executed)
    }
    
    @Test
    fun `ifNotBlank should execute block for non-blank string`() {
        val value: String? = "Hello"
        val result = value.ifNotBlank { it.uppercase() }
        
        assertEquals("HELLO", result)
    }
    
    @Test
    fun `ifNotBlank should not execute block for blank string`() {
        val blank: String? = "   "
        val empty: String? = ""
        val nullStr: String? = null
        
        assertNull(blank.ifNotBlank { it.uppercase() })
        assertNull(empty.ifNotBlank { it.uppercase() })
        assertNull(nullStr.ifNotBlank { it.uppercase() })
    }
    
    @Test
    fun `orThrow should return value when not null`() {
        val value: String? = "Hello"
        assertEquals("Hello", value.orThrow { "Should not throw" })
    }
    
    @Test
    fun `orThrow should throw when null`() {
        val value: String? = null
        assertThrows(IllegalStateException::class.java) {
            value.orThrow { "Value was null" }
        }
    }
    
    @Test
    fun `batch should partition list into chunks`() {
        val list = listOf(1, 2, 3, 4, 5, 6, 7)
        val batches = list.batch(3)
        
        assertEquals(3, batches.size)
        assertEquals(listOf(1, 2, 3), batches[0])
        assertEquals(listOf(4, 5, 6), batches[1])
        assertEquals(listOf(7), batches[2])
    }
    
    @Test
    fun `findIndexed should return index and element`() {
        val list = listOf("a", "b", "c", "d")
        val result = list.findIndexed { index, element -> element == "c" }
        
        assertNotNull(result)
        assertEquals(2 to "c", result)
    }
    
    @Test
    fun `findIndexed should return null when not found`() {
        val list = listOf("a", "b", "c")
        val result = list.findIndexed { _, element -> element == "z" }
        
        assertNull(result)
    }
    
    @Test
    fun `safeCall should wrap successful result`() {
        val result = safeCall { 42 }
        
        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }
    
    @Test
    fun `safeCall should wrap exception in failure`() {
        val result = safeCall<Int> { throw RuntimeException("Error") }
        
        assertTrue(result.isFailure)
        assertEquals("Error", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `formatWithSeparators should format numbers`() {
        assertEquals("1,000", 1000.formatWithSeparators())
        assertEquals("1,000,000", 1000000.formatWithSeparators())
    }
    
    @Test
    fun `double format should limit decimal places`() {
        assertEquals("3.14", 3.14159.format(2))
        assertEquals("3.1", 3.14159.format(1))
    }
    
    @Test
    fun `clamp should limit values`() {
        assertEquals(5, 3.clamp(5, 10))
        assertEquals(10, 15.clamp(5, 10))
        assertEquals(7, 7.clamp(5, 10))
    }
    
    @Test
    fun `double clamp should limit values`() {
        assertEquals(5.0, 3.0.clamp(5.0, 10.0), 0.001)
        assertEquals(10.0, 15.0.clamp(5.0, 10.0), 0.001)
        assertEquals(7.5, 7.5.clamp(5.0, 10.0), 0.001)
    }
}
