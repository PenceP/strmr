package com.strmr.ai.ui.components.common.focus

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class DpadFocusManagerTest {
    
    private lateinit var focusManager: DpadFocusManager
    
    @Before
    fun setup() {
        focusManager = DpadFocusManager()
    }
    
    @Test
    fun `updateFocus - given valid parameters - should update current focused item`() = runTest {
        // Given
        val rowId = "test-row"
        val itemIndex = 3
        val itemId = 123
        
        // When
        focusManager.updateFocus(rowId, itemIndex, itemId)
        
        // Then
        val currentFocus = focusManager.currentFocusedItem.value
        assertNotNull("Current focused item should not be null", currentFocus)
        assertEquals(rowId, currentFocus?.rowId)
        assertEquals(itemIndex, currentFocus?.itemIndex)
        assertEquals(itemId, currentFocus?.itemId)
    }
    
    @Test
    fun `updateFocus - should store item in focus history`() {
        // Given
        val rowId = "test-row"
        val itemIndex = 5
        val itemId = 456
        
        // When
        focusManager.updateFocus(rowId, itemIndex, itemId)
        
        // Then
        val lastFocused = focusManager.getLastFocusedItem(rowId)
        assertNotNull("Last focused item should not be null", lastFocused)
        assertEquals(rowId, lastFocused?.rowId)
        assertEquals(itemIndex, lastFocused?.itemIndex)
        assertEquals(itemId, lastFocused?.itemId)
    }
    
    @Test
    fun `getLastFocusedItem - given non-existent row - should return null`() {
        // Given
        val nonExistentRowId = "non-existent-row"
        
        // When
        val result = focusManager.getLastFocusedItem(nonExistentRowId)
        
        // Then
        assertNull("Should return null for non-existent row", result)
    }
    
    @Test
    fun `getLastFocusedIndex - given existing row - should return correct index`() {
        // Given
        val rowId = "test-row"
        val itemIndex = 7
        val itemId = 789
        focusManager.updateFocus(rowId, itemIndex, itemId)
        
        // When
        val result = focusManager.getLastFocusedIndex(rowId)
        
        // Then
        assertEquals(itemIndex, result)
    }
    
    @Test
    fun `getLastFocusedIndex - given non-existent row - should return zero`() {
        // Given
        val nonExistentRowId = "non-existent-row"
        
        // When
        val result = focusManager.getLastFocusedIndex(nonExistentRowId)
        
        // Then
        assertEquals(0, result)
    }
    
    @Test
    fun `clearRowFocus - should remove row from history`() {
        // Given
        val rowId = "test-row"
        focusManager.updateFocus(rowId, 2, 222)
        assertTrue("Row should exist in history", focusManager.hasRowFocusHistory(rowId))
        
        // When
        focusManager.clearRowFocus(rowId)
        
        // Then
        assertFalse("Row should be removed from history", focusManager.hasRowFocusHistory(rowId))
        assertNull("Last focused item should be null", focusManager.getLastFocusedItem(rowId))
    }
    
    @Test
    fun `clearRowFocus - given currently focused row - should clear current focus`() = runTest {
        // Given
        val rowId = "test-row"
        focusManager.updateFocus(rowId, 1, 111)
        assertNotNull("Current focus should be set", focusManager.currentFocusedItem.value)
        
        // When
        focusManager.clearRowFocus(rowId)
        
        // Then
        assertNull("Current focus should be cleared", focusManager.currentFocusedItem.value)
    }
    
    @Test
    fun `clearAllFocus - should remove all focus history and current focus`() = runTest {
        // Given
        focusManager.updateFocus("row1", 1, 111)
        focusManager.updateFocus("row2", 2, 222)
        assertEquals(2, focusManager.getFocusHistorySize())
        assertNotNull("Current focus should be set", focusManager.currentFocusedItem.value)
        
        // When
        focusManager.clearAllFocus()
        
        // Then
        assertEquals(0, focusManager.getFocusHistorySize())
        assertNull("Current focus should be cleared", focusManager.currentFocusedItem.value)
    }
    
    @Test
    fun `getRowsWithFocusHistory - should return all rows with history`() {
        // Given
        focusManager.updateFocus("row1", 1, 111)
        focusManager.updateFocus("row2", 2, 222)
        focusManager.updateFocus("row3", 3, 333)
        
        // When
        val rowsWithHistory = focusManager.getRowsWithFocusHistory()
        
        // Then
        assertEquals(3, rowsWithHistory.size)
        assertTrue("Should contain row1", rowsWithHistory.contains("row1"))
        assertTrue("Should contain row2", rowsWithHistory.contains("row2"))
        assertTrue("Should contain row3", rowsWithHistory.contains("row3"))
    }
    
    @Test
    fun `hasRowFocusHistory - given existing row - should return true`() {
        // Given
        val rowId = "test-row"
        focusManager.updateFocus(rowId, 1, 111)
        
        // When
        val result = focusManager.hasRowFocusHistory(rowId)
        
        // Then
        assertTrue("Should return true for existing row", result)
    }
    
    @Test
    fun `hasRowFocusHistory - given non-existent row - should return false`() {
        // Given
        val nonExistentRowId = "non-existent-row"
        
        // When
        val result = focusManager.hasRowFocusHistory(nonExistentRowId)
        
        // Then
        assertFalse("Should return false for non-existent row", result)
    }
    
    @Test
    fun `restoreFocusToRow - given existing row - should restore and update current focus`() = runTest {
        // Given
        val rowId = "test-row"
        val originalItemIndex = 5
        val originalItemId = 555
        focusManager.updateFocus(rowId, originalItemIndex, originalItemId)
        
        // Clear current focus
        focusManager.clearAllFocus()
        focusManager.updateFocus(rowId, originalItemIndex, originalItemId) // Restore history
        
        // When
        val restoredItem = focusManager.restoreFocusToRow(rowId)
        
        // Then
        assertNotNull("Restored item should not be null", restoredItem)
        assertEquals(rowId, restoredItem?.rowId)
        assertEquals(originalItemIndex, restoredItem?.itemIndex)
        assertEquals(originalItemId, restoredItem?.itemId)
        
        val currentFocus = focusManager.currentFocusedItem.value
        assertNotNull("Current focus should be restored", currentFocus)
        assertEquals(rowId, currentFocus?.rowId)
    }
    
    @Test
    fun `restoreFocusToRow - given non-existent row - should return null`() = runTest {
        // Given
        val nonExistentRowId = "non-existent-row"
        
        // When
        val result = focusManager.restoreFocusToRow(nonExistentRowId)
        
        // Then
        assertNull("Should return null for non-existent row", result)
        assertNull("Current focus should remain null", focusManager.currentFocusedItem.value)
    }
    
    @Test
    fun `createFocusSnapshot - should capture current state`() = runTest {
        // Given
        focusManager.updateFocus("row1", 1, 111)
        focusManager.updateFocus("row2", 2, 222)
        
        // When
        val snapshot = focusManager.createFocusSnapshot()
        
        // Then
        assertNotNull("Snapshot should not be null", snapshot)
        assertNotNull("Current focus should be captured", snapshot.currentFocus)
        assertEquals(2, snapshot.focusHistory.size)
        assertTrue("Should contain row1", snapshot.focusHistory.containsKey("row1"))
        assertTrue("Should contain row2", snapshot.focusHistory.containsKey("row2"))
        assertTrue("Timestamp should be set", snapshot.timestamp > 0)
    }
    
    @Test
    fun `restoreFromSnapshot - should restore focus state`() = runTest {
        // Given
        val originalFocusManager = DpadFocusManager()
        originalFocusManager.updateFocus("row1", 1, 111)
        originalFocusManager.updateFocus("row2", 2, 222)
        val snapshot = originalFocusManager.createFocusSnapshot()
        
        // Clear the current focus manager
        focusManager.clearAllFocus()
        assertEquals(0, focusManager.getFocusHistorySize())
        
        // When
        focusManager.restoreFromSnapshot(snapshot)
        
        // Then
        assertEquals(2, focusManager.getFocusHistorySize())
        assertTrue("Should have row1 history", focusManager.hasRowFocusHistory("row1"))
        assertTrue("Should have row2 history", focusManager.hasRowFocusHistory("row2"))
        
        val currentFocus = focusManager.currentFocusedItem.value
        assertNotNull("Current focus should be restored", currentFocus)
        assertEquals("row2", currentFocus?.rowId) // Last updated row
    }
    
    @Test
    fun `FocusedItem - should create with correct timestamp`() {
        // Given
        val startTime = System.currentTimeMillis()
        
        // When
        val focusedItem = FocusedItem("test-row", 1, 111)
        
        // Then
        assertTrue("Timestamp should be recent", focusedItem.timestamp >= startTime)
        assertEquals("test-row", focusedItem.rowId)
        assertEquals(1, focusedItem.itemIndex)
        assertEquals(111, focusedItem.itemId)
    }
    
    @Test
    fun `ItemFocusState - should track focus changes`() {
        // Given
        var focusChangeCount = 0
        val itemFocusState = ItemFocusState { focused ->
            focusChangeCount++
        }
        
        // When
        itemFocusState.updateFocus(true)
        
        // Then
        assertTrue("Should be focused", itemFocusState.isFocused)
        assertEquals(1, focusChangeCount)
        
        // When - same focus state
        itemFocusState.updateFocus(true)
        
        // Then - should not trigger callback
        assertEquals(1, focusChangeCount)
        
        // When - different focus state
        itemFocusState.updateFocus(false)
        
        // Then
        assertFalse("Should not be focused", itemFocusState.isFocused)
        assertEquals(2, focusChangeCount)
    }
    
    @Test
    fun `RowFocusCoordinator - should manage multiple row focus states`() {
        // Given
        val coordinator = RowFocusCoordinator(focusManager)
        
        // When
        val rowState1 = coordinator.getRowFocusState("row1")
        val rowState2 = coordinator.getRowFocusState("row2")
        val rowState1Again = coordinator.getRowFocusState("row1")
        
        // Then
        assertNotNull("Row state 1 should not be null", rowState1)
        assertNotNull("Row state 2 should not be null", rowState2)
        assertSame("Should return same instance for same row", rowState1, rowState1Again)
        assertNotSame("Should return different instances for different rows", rowState1, rowState2)
    }
    
    @Test
    fun `RowFocusCoordinator - clearRowFocusState - should remove row state`() {
        // Given
        val coordinator = RowFocusCoordinator(focusManager)
        val rowState = coordinator.getRowFocusState("test-row")
        focusManager.updateFocus("test-row", 1, 111)
        
        // When
        coordinator.clearRowFocusState("test-row")
        
        // Then
        assertFalse("Focus manager should not have row history", focusManager.hasRowFocusHistory("test-row"))
        
        // Getting the same row state again should create a new instance
        val newRowState = coordinator.getRowFocusState("test-row")
        assertNotSame("Should create new instance after clearing", rowState, newRowState)
    }
}