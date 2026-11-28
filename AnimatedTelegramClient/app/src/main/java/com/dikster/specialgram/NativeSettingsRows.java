package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

/** Native Java settings rows and privacy selectors. */
public final class NativeSettingsRows {
    private NativeSettingsRows() { }

    public static LinearLayout navigation(Context c, String title, String subtitle,
                                          int textColor, int mutedColor, Runnable click) {
        LinearLayout root = vertical(c);
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(c, 15), 0, dp(c, 15));
        TextView heading = text(c, title, textColor, 15, true);
        TextView detail = text(c, subtitle, mutedColor, 12, false);
        LinearLayout labels = vertical(c);
        labels.addView(heading);
        labels.addView(detail);
        row.addView(labels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(text(c, "›", mutedColor, 25, false));
        row.setOnClickListener(v -> { if (click != null) click.run(); });
        root.addView(row);
        root.addView(divider(c, mutedColor));
        return root;
    }

    public static LinearLayout coverChoice(Context c, String title, String subtitle,
                                           boolean selected, int textColor, int mutedColor,
                                           Runnable click) {
        LinearLayout root = vertical(c);
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(c, 16), 0, dp(c, 16));
        LinearLayout labels = vertical(c);
        labels.addView(text(c, title, textColor, 16, true));
        labels.addView(text(c, subtitle, mutedColor, 12, false));
        row.addView(labels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        RadioButton radio = new RadioButton(c);
        radio.setButtonTintList(android.content.res.ColorStateList.valueOf(textColor));
        radio.setChecked(selected);
        row.addView(radio);
        row.setOnClickListener(v -> { if (click != null) click.run(); });
        radio.setOnClickListener(v -> { if (click != null) click.run(); });
        root.addView(row);
        root.addView(divider(c, mutedColor));
        return root;
    }

    public static LinearLayout setting(Context c, String title, String subtitle,
                                       int textColor, int mutedColor) {
        LinearLayout root = vertical(c);
        root.setPadding(0, dp(c, 13), 0, dp(c, 13));
        root.addView(text(c, title, textColor, 15, false));
        TextView detail = text(c, subtitle, mutedColor, 12, false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(c, 3);
        root.addView(detail, params);
        return root;
    }

    public static LinearLayout privacy(Context c, String title, String current,
                                       int textColor, int mutedColor, ChoiceListener listener) {
        LinearLayout root = vertical(c);
        TextView heading = text(c, title, textColor, 16, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hp.bottomMargin = dp(c, 5);
        root.addView(heading, hp);
        String[] choices = {"Все", "Мои контакты", "Никто"};
        for (String choice : choices) {
            LinearLayout row = new LinearLayout(c);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(c, 9), 0, dp(c, 9));
            row.addView(text(c, choice, textColor, 14, false),
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            RadioButton radio = new RadioButton(c);
            radio.setButtonTintList(android.content.res.ColorStateList.valueOf(textColor));
            radio.setChecked(choice.equals(current));
            row.addView(radio);
            row.setOnClickListener(v -> { if (listener != null) listener.onSelect(choice); });
            radio.setOnClickListener(v -> { if (listener != null) listener.onSelect(choice); });
            root.addView(row);
        }
        root.addView(text(c, "Текущее значение: " + current, mutedColor, 11, false));
        return root;
    }

    public static void updateNavigation(LinearLayout root, Context c, String title,
                                        String subtitle, int textColor, int mutedColor,
                                        Runnable click) {
        replaceContents(root, navigation(c, title, subtitle, textColor, mutedColor, click));
    }

    public static void updateCoverChoice(LinearLayout root, Context c, String title,
                                         String subtitle, boolean selected, int textColor,
                                         int mutedColor, Runnable click) {
        replaceContents(root, coverChoice(c, title, subtitle, selected, textColor, mutedColor, click));
    }

    public static void updateSetting(LinearLayout root, Context c, String title, String subtitle,
                                     int textColor, int mutedColor) {
        replaceContents(root, setting(c, title, subtitle, textColor, mutedColor));
    }

    public static void updatePrivacy(LinearLayout root, Context c, String title, String current,
                                     int textColor, int mutedColor, ChoiceListener listener) {
        replaceContents(root, privacy(c, title, current, textColor, mutedColor, listener));
    }

    private static void replaceContents(LinearLayout target, LinearLayout source) {
        target.removeAllViews();
        while (source.getChildCount() > 0) {
            View child = source.getChildAt(0);
            source.removeViewAt(0);
            target.addView(child);
        }
    }

    public interface ChoiceListener { void onSelect(String value); }

    private static LinearLayout vertical(Context c) {
        LinearLayout view = new LinearLayout(c);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return view;
    }

    private static TextView text(Context c, String value, int color, int size, boolean bold) {
        TextView view = new TextView(c);
        view.setText(value == null ? "" : value);
        view.setTextColor(color);
        view.setTextSize(size);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private static View divider(Context c, int mutedColor) {
        View view = new View(c);
        view.setBackgroundColor((mutedColor & 0x00FFFFFF) | 0x29000000);
        view.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(c, 1)));
        return view;
    }

    private static int dp(Context c, int value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }
}
