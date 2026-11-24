package com.example.data.maintenance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TireSetDeletionTest {

    @Test
    fun referencedSetIsBlockedWithAnExplanation() {
        val result = tireSetDeleteResult(referenceCount = 2, name = "Summer")

        assertTrue(result is TireSetDeleteResult.Blocked)
        val message = (result as TireSetDeleteResult.Blocked).message
        assertTrue(message.contains("Summer"))
        assertTrue(message.contains("swap"))
        assertTrue(message.contains("rotation"))
        assertTrue(message.contains("Rename"))
    }

    @Test
    fun unreferencedSetCanBeDeleted() {
        assertEquals(TireSetDeleteResult.Deleted, tireSetDeleteResult(referenceCount = 0, name = "Spare"))
    }
}
