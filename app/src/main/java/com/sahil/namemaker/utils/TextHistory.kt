package com.sahil.namemaker.utils

class TextHistory(private val maxEntries: Int = 60) {
    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()
    private var current: String = ""

    fun reset(initial: String) {
        undoStack.clear()
        redoStack.clear()
        current = initial
    }

    fun push(newValue: String) {
        if (newValue == current) return
        undoStack.addLast(current)
        if (undoStack.size > maxEntries) {
            undoStack.removeFirst()
        }
        current = newValue
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo(): String? {
        if (undoStack.isEmpty()) return null
        redoStack.addLast(current)
        current = undoStack.removeLast()
        return current
    }

    fun redo(): String? {
        if (redoStack.isEmpty()) return null
        undoStack.addLast(current)
        current = redoStack.removeLast()
        return current
    }
}
