package com.trace.io;

import com.trace.model.subcircuit.SubCircuitDefinition;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User-local library of sub-circuit definitions. Lives at
 * {@code ~/.trace/subcircuits/<id>.trs}.
 *
 * Definitions are cached by id after first load; call {@link #reload()} to
 * rescan the directory (e.g. after saving a new sub-circuit).
 */
public final class SubCircuitLibrary {
    private static final Map<String, SubCircuitDefinition> CACHE = new HashMap<>();
    private static boolean scanned = false;

    private SubCircuitLibrary() {}

    public static File directory() {
        File dir = new File(System.getProperty("user.home"), ".trace/subcircuits");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /** Returns the canonical on-disk path for an id. */
    public static File fileFor(String id) {
        return new File(directory(), sanitize(id) + ".trs");
    }

    private static String sanitize(String id) {
        return id.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /** All loaded definitions. Triggers a scan if none have been loaded yet. */
    public static synchronized List<SubCircuitDefinition> all() {
        if (!scanned) reload();
        List<SubCircuitDefinition> list = new ArrayList<>(CACHE.values());
        list.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return list;
    }

    /** Looks up a definition by id, loading on demand. Returns null if not found. */
    public static synchronized SubCircuitDefinition get(String id) {
        if (id == null) return null;
        SubCircuitDefinition cached = CACHE.get(id);
        if (cached != null) return cached;
        File f = fileFor(id);
        if (!f.exists()) return null;
        try {
            SubCircuitDefinition def = SubCircuitIO.read(f);
            CACHE.put(def.getId(), def);
            return def;
        } catch (IOException e) {
            return null;
        }
    }

    /** Writes a definition to disk and refreshes the cache. */
    public static synchronized void save(SubCircuitDefinition def, String author) throws IOException {
        File f = fileFor(def.getId());
        SubCircuitIO.write(def, author, f);
        CACHE.put(def.getId(), def);
    }

    /** Rescans the on-disk directory and rebuilds the cache. */
    public static synchronized void reload() {
        CACHE.clear();
        scanned = true;
        File dir = directory();
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".trs"));
        if (files == null) return;
        for (File f : files) {
            try {
                SubCircuitDefinition def = SubCircuitIO.read(f);
                CACHE.put(def.getId(), def);
            } catch (IOException ignore) {
                // Skip unreadable files rather than failing the scan.
            }
        }
    }

    /** Unmodifiable view of the cache, primarily for diagnostics. */
    public static synchronized Map<String, SubCircuitDefinition> cache() {
        return Collections.unmodifiableMap(CACHE);
    }
}
