package com.logiclab.util;

import com.logiclab.model.Circuit;
import com.logiclab.model.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Snapshot-based undo/redo. Serializes the entire Circuit into an in-memory
 * byte array before each mutation, so any mutation path is recorded without
 * instrumenting individual operations. Circuit is already Serializable, so
 * the round-trip matches what save/load does to disk.
 */
public class UndoManager {
    private static final int MAX_DEPTH = 50;
    private final Deque<byte[]> undoStack = new ArrayDeque<>();
    private final Deque<byte[]> redoStack = new ArrayDeque<>();

    /** Snapshot the current circuit onto the undo stack. Clears redo. */
    public void capture(Circuit c) {
        byte[] snap = serialize(c);
        if (snap == null) return;
        undoStack.push(snap);
        while (undoStack.size() > MAX_DEPTH) undoStack.pollLast();
        redoStack.clear();
    }

    /** Pops the latest snapshot off the undo stack, pushing `current` onto redo. */
    public Circuit undo(Circuit current) {
        if (undoStack.isEmpty()) return null;
        byte[] curSnap = serialize(current);
        if (curSnap == null) return null;
        redoStack.push(curSnap);
        return deserialize(undoStack.pop());
    }

    /** Pops the latest snapshot off the redo stack, pushing `current` onto undo. */
    public Circuit redo(Circuit current) {
        if (redoStack.isEmpty()) return null;
        byte[] curSnap = serialize(current);
        if (curSnap == null) return null;
        undoStack.push(curSnap);
        return deserialize(redoStack.pop());
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    private byte[] serialize(Circuit c) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(c);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private Circuit deserialize(byte[] data) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            Circuit c = (Circuit) ois.readObject();
            // Pin.owner is transient — must rebuild the back-references.
            for (Component comp : c.getComponents()) comp.restorePinOwnership();
            return c;
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
}
