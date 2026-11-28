package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Small reusable native rows used by profile and message history surfaces. */
public final class NativeTextRows {
    private NativeTextRows() { }

    public static LinearLayout profileInfo(Context context, String label, String value,
                                           int mutedColor, int textColor) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView left = new TextView(context);
        left.setText(label);
        left.setTextColor(mutedColor);
        left.setTextSize(12);
        row.addView(left, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView right = new TextView(context);
        right.setText(value);
        right.setTextColor(textColor);
        right.setTextSize(12);
        right.setGravity(Gravity.END);
        row.addView(right, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    public static FrameLayout messageDateDivider(Context context, int timestamp, int panelColor) {
        FrameLayout outer = new FrameLayout(context);
        outer.setPadding(0, dp(context, 9), 0, dp(context, 9));
        TextView label = new TextView(context);
        label.setText(formatMessageDate(timestamp));
        label.setTextColor(Color.WHITE);
        label.setTextSize(12);
        label.setPadding(dp(context, 12), dp(context, 5), dp(context, 12), dp(context, 5));
        GradientDrawable background = new GradientDrawable();
        background.setColor(adjustAlpha(panelColor, 0.88f));
        background.setCornerRadius(dp(context, 20));
        label.setBackground(background);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        outer.addView(label, params);
        return outer;
    }

    public static boolean isSameMessageDay(int first, int second) {
        Calendar a = Calendar.getInstance();
        Calendar b = Calendar.getInstance();
        a.setTimeInMillis(first * 1000L);
        b.setTimeInMillis(second * 1000L);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private static String formatMessageDate(int timestamp) {
        Date date = new Date(timestamp * 1000L);
        Calendar today = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTime(date);
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (today.get(Calendar.YEAR) == target.get(Calendar.YEAR)
                && today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR))
            return "Сегодня";
        if (yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR)
                && yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR))
            return "Вчера";
        return new SimpleDateFormat("d MMMM yyyy", new Locale("ru")).format(date);
    }

    private static int adjustAlpha(int color, float alpha) {
        return (color & 0x00FFFFFF) | (Math.round(Color.alpha(color) * alpha) << 24);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
