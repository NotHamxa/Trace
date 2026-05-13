package com.trace.util;

import com.trace.model.Circuit;
import com.trace.model.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayDeque;
import java.util.Deque;

public class UndoManager {
    private static final int MAX_DEPTH = 50;
    private final Deque<byte[]> undoStack = new ArrayDeque<>();
    private final Deque<byte[]> redoStack = new ArrayDeque<>();

    public void capture(Circuit c) {
        byte[] snap = serialize(c);
        if (snap == null) return;
        undoStack.push(snap);
        while (undoStack.size() > MAX_DEPTH) undoStack.pollLast();
        redoStack.clear();
    }

    public Circuit undo(Circuit current) {
        if (undoStack.isEmpty()) return null;
        byte[] curSnap = serialize(current);
        if (curSnap == null) return null;
        redoStack.push(curSnap);
        return deserialize(undoStack.pop());
    }

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
            for (Component comp : c.getComponents()) comp.restorePinOwnership();
            return c;
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
}
