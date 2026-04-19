package com.trace.ui;

/**
 * Central palette constants — matches the IntelliJ IDEA "New UI" dark theme
 * so the entire app reads as one coherent surface.
 */
public final class Theme {
    private Theme() {}

    // Surfaces
    public static final String BG_EDITOR   = "#1e1f22"; // canvas / editor body
    public static final String BG_CHROME   = "#17181a"; // toolbar / sidebar / status bar / title bar
    public static final String BG_ELEVATED = "#17181a"; // panels / popovers
    public static final String BG_INPUT    = "#2b2d30"; // text fields
    public static final String BG_HOVER    = "#2b2d30";
    public static final String BG_SELECT   = "#2e436e";

    // Borders
    public static final String BORDER_STRONG = "#0f1012";
    public static final String BORDER_SOFT   = "#2b2d30";

    // Text
    public static final String TEXT_PRIMARY   = "#dfe1e5";
    public static final String TEXT_SECONDARY = "#868a91";
    public static final String TEXT_MUTED     = "#5f6267";

    // Accents
    public static final String ACCENT_BLUE      = "#3574f0";
    public static final String ACCENT_BLUE_HOVR = "#4481f5";
    public static final String ACCENT_GREEN     = "#6aab73";
    public static final String ACCENT_YELLOW    = "#e8a33d";
    public static final String ACCENT_RED       = "#db5c5c";

    // Neutral buttons
    public static final String BTN_BG    = "#2e3033";
    public static final String BTN_HOVER = "#3c3f41";
}
