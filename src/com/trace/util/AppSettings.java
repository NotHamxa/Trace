package com.trace.util;

public final class AppSettings {
    private AppSettings() {}

    private static boolean autoBendWires = false;

    public static boolean isAutoBendWires() { return autoBendWires; }
    public static void setAutoBendWires(boolean v) { autoBendWires = v; }
}
