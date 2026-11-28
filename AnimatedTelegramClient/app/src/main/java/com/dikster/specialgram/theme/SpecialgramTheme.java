package com.dikster.specialgram.theme;

import android.graphics.Color;

/**
 * Единая нативная палитра и размеры Specialgram.
 *
 * Все значения размеров заданы в dp/sp и должны преобразовываться
 * через ресурсы/DisplayMetrics в месте использования.
 */
public final class SpecialgramTheme {
    private SpecialgramTheme() { }

    public static final int BACKGROUND = Color.rgb(15, 17, 21);       // #0F1115
    public static final int SURFACE = Color.rgb(22, 24, 29);          // #16181D
    public static final int ACCENT = Color.rgb(51, 144, 236);         // #3390EC
    public static final int OUTGOING_BUBBLE = Color.rgb(43, 82, 120); // #2B5278
    public static final int INCOMING_BUBBLE = Color.rgb(44, 44, 53);  // #2C2C35
    public static final int TEXT_PRIMARY = Color.WHITE;
    public static final int TEXT_SECONDARY = Color.rgb(139, 141, 147); // #8B8D93
    public static final int STORY_RING_START = Color.rgb(124, 77, 255); // #7C4DFF
    public static final int STORY_RING_END = ACCENT;

    public static final float SCREEN_HORIZONTAL_PADDING_DP = 16f;
    public static final float CHAT_ROW_HEIGHT_DP = 76f;
    public static final float CHAT_AVATAR_SIZE_DP = 52f;
    public static final float MESSAGE_TEXT_SIZE_SP = 15f;
    public static final float HEADER_TITLE_SIZE_SP = 17f;
    public static final float BOTTOM_BAR_HEIGHT_DP = 56f;
    public static final float MESSAGE_BUBBLE_RADIUS_DP = 18f;
    public static final float MESSAGE_BUBBLE_JOIN_RADIUS_DP = 4f;
    public static final float STORY_RING_WIDTH_DP = 3f;
}
