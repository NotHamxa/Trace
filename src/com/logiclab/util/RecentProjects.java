package com.logiclab.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Persists a short MRU list of project files using java.util.prefs so we
 * don't have to manage a config directory or file format ourselves.
 */
public final class RecentProjects {
    private static final String KEY_PREFIX = "recent_";
    private static final int MAX = 10;
    private static final Preferences PREFS = Preferences.userNodeForPackage(RecentProjects.class);

    private RecentProjects() {}

    public static List<File> load() {
        List<File> list = new ArrayList<>();
        for (int i = 0; i < MAX; i++) {
            String path = PREFS.get(KEY_PREFIX + i, null);
            if (path == null) break;
            File f = new File(path);
            if (f.exists()) list.add(f);
        }
        return list;
    }

    /** Adds `file` to the front of the MRU list (dedupes and trims to MAX). */
    public static void add(File file) {
        if (file == null) return;
        List<File> list = load();
        list.removeIf(f -> f.getAbsolutePath().equals(file.getAbsolutePath()));
        list.add(0, file);
        while (list.size() > MAX) list.remove(list.size() - 1);
        save(list);
    }

    public static void remove(File file) {
        if (file == null) return;
        List<File> list = load();
        list.removeIf(f -> f.getAbsolutePath().equals(file.getAbsolutePath()));
        save(list);
    }

    public static void clear() {
        for (int i = 0; i < MAX; i++) PREFS.remove(KEY_PREFIX + i);
    }

    private static void save(List<File> list) {
        for (int i = 0; i < MAX; i++) {
            if (i < list.size()) PREFS.put(KEY_PREFIX + i, list.get(i).getAbsolutePath());
            else PREFS.remove(KEY_PREFIX + i);
        }
    }
}
