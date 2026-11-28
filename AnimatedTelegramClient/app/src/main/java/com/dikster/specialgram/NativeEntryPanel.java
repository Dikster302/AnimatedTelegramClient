package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.function.Consumer;

/** Telegram-inspired native authorization screen with explicit submit and error feedback. */
public final class NativeEntryPanel extends LinearLayout {
    private final TextView titleView;
    private final TextView subtitleView;
    private final TextView errorView;
    private final EditText input;
    private final Button submit;
    private final TextView footer;
    private Consumer<String> valueListener;
    private Runnable submitListener;
    private boolean changingText;
    private boolean submitting;
    private final String modeTitle;

    public NativeEntryPanel(Context context, String title, String subtitle, String value,
                            String buttonText, boolean secret, int backgroundColor,
                            int textColor, int mutedColor, int accentColor,
                            Consumer<String> onValue, Runnable onSubmit, String error) {
        super(context);
        modeTitle = title == null ? "" : title;
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setBackgroundColor(backgroundColor);
        setPadding(dp(26), dp(18), dp(26), dp(22));
        setFocusableInTouchMode(true);

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        addView(scroll, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(0, dp(18), 0, dp(18));
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.MATCH_PARENT));

        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.ic_launcher_foreground);
        logo.setContentDescription("Specialgram");
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LayoutParams logoParams = new LayoutParams(dp(76), dp(76));
        logoParams.bottomMargin = dp(20);
        content.addView(logo, logoParams);

        TextView brand = new TextView(context);
        brand.setText("Specialgram");
        brand.setTextColor(textColor);
        brand.setTextSize(17);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.setGravity(Gravity.CENTER);
        LayoutParams brandParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        brandParams.bottomMargin = dp(24);
        content.addView(brand, brandParams);

        titleView = new TextView(context);
        titleView.setTextSize(23);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleView.setGravity(Gravity.CENTER);
        content.addView(titleView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        subtitleView = new TextView(context);
        subtitleView.setTextSize(14);
        subtitleView.setGravity(Gravity.CENTER);
        subtitleView.setLineSpacing(dp(3), 1f);
        LayoutParams subtitleParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(10);
        subtitleParams.bottomMargin = dp(26);
        content.addView(subtitleView, subtitleParams);

        input = new EditText(context);
        input.setSingleLine(true);
        input.setTextSize(17);
        input.setTextColor(textColor);
        input.setHintTextColor(mutedColor);
        input.setPadding(dp(16), 0, dp(16), 0);
        input.setSelectAllOnFocus(false);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setInputType(resolveInputType(title, secret));
        input.setHint(resolveHint(title, secret));
        input.setBackground(fieldBackground(mutedColor));
        LayoutParams inputParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(56));
        content.addView(input, inputParams);

        errorView = new TextView(context);
        errorView.setTextColor(0xFFFF6B6B);
        errorView.setTextSize(13);
        errorView.setGravity(Gravity.CENTER);
        errorView.setVisibility(GONE);
        LayoutParams errorParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        errorParams.topMargin = dp(10);
        content.addView(errorView, errorParams);

        submit = new Button(context);
        submit.setAllCaps(false);
        submit.setTextSize(15);
        submit.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        submit.setTextColor(Color.WHITE);
        submit.setPadding(dp(12), 0, dp(12), 0);
        submit.setBackground(buttonBackground(accentColor));
        LayoutParams buttonParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(50));
        buttonParams.topMargin = dp(22);
        content.addView(submit, buttonParams);

        footer = new TextView(context);
        footer.setText("Продолжая, вы подтверждаете, что номер телефона принадлежит вам.");
        footer.setTextColor(mutedColor);
        footer.setTextSize(12);
        footer.setGravity(Gravity.CENTER);
        footer.setLineSpacing(dp(2), 1f);
        LayoutParams footerParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        footerParams.topMargin = dp(20);
        content.addView(footer, footerParams);

        valueListener = onValue;
        submitListener = onSubmit;
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!changingText && valueListener != null) valueListener.accept(s.toString());
                if (submitting) submitting = false;
                updateSubmitState();
            }
            @Override public void afterTextChanged(Editable editable) { }
        });

        submit.setOnClickListener(view -> submit());
        input.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_DONE || enter) {
                submit();
                return true;
            }
            return false;
        });

        update(title, subtitle, value, buttonText, secret, backgroundColor,
                textColor, mutedColor, accentColor, onValue, onSubmit, error);
    }

    public void update(String title, String subtitle, String value, String buttonText,
                       boolean secret, int backgroundColor, int textColor, int mutedColor,
                       int accentColor, Consumer<String> onValue, Runnable onSubmit, String error) {
        valueListener = onValue;
        submitListener = onSubmit;
        setBackgroundColor(backgroundColor);
        titleView.setText(title);
        titleView.setTextColor(textColor);
        subtitleView.setText(subtitle);
        subtitleView.setTextColor(mutedColor);
        input.setTextColor(textColor);
        input.setHintTextColor(mutedColor);
        input.setInputType(resolveInputType(title, secret));
        input.setHint(resolveHint(title, secret));
        input.setBackground(fieldBackground(mutedColor));
        submit.setBackground(buttonBackground(accentColor));
        submit.setText(buttonText);
        footer.setTextColor(mutedColor);

        String current = input.getText().toString();
        if (!current.equals(value == null ? "" : value)) {
            changingText = true;
            input.setText(value == null ? "" : value);
            input.setSelection(input.getText().length());
            changingText = false;
        }

        if (error != null && !error.trim().isEmpty()) {
            errorView.setText(error);
            errorView.setVisibility(VISIBLE);
        } else {
            errorView.setText("");
            errorView.setVisibility(GONE);
        }
        submitting = false;
        updateSubmitState();
    }

    private void submit() {
        String value = input.getText() == null ? "" : input.getText().toString().trim();
        if (value.isEmpty() || submitting || submitListener == null) return;
        submitting = true;
        updateSubmitState();
        InputMethodManager imm = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        submitListener.run();
    }

    private void updateSubmitState() {
        boolean valid = input.getText() != null && input.getText().toString().trim().length() > 0;
        submit.setEnabled(valid && !submitting);
        submit.setAlpha(valid && !submitting ? 1f : .55f);
        if (submitting) submit.setText("Отправляем…");
    }

    private int resolveInputType(String title, boolean secret) {
        if (secret) return InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
        if (title != null && title.toLowerCase().contains("номер"))
            return InputType.TYPE_CLASS_PHONE;
        if (title != null && title.toLowerCase().contains("код"))
            return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
        return InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
    }

    private String resolveHint(String title, boolean secret) {
        if (secret) return "Пароль";
        if (title != null && title.toLowerCase().contains("номер")) return "+7 700 000 00 00";
        if (title != null && title.toLowerCase().contains("код")) return "Код подтверждения";
        return "Введите данные";
    }

    private GradientDrawable fieldBackground(int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(0xFF17212B);
        drawable.setCornerRadius(dp(12));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private GradientDrawable buttonBackground(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(12));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public static View loading(Context context, String title, String subtitle,
                               int backgroundColor, int textColor, int mutedColor, int accentColor) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(28, 28, 28, 28);
        root.setBackgroundColor(backgroundColor);
        TextView heading = new TextView(context);
        heading.setText(title);
        heading.setTextColor(textColor);
        heading.setTextSize(19);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setGravity(Gravity.CENTER);
        root.addView(heading, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        TextView detail = new TextView(context);
        detail.setText(subtitle);
        detail.setTextColor(mutedColor);
        detail.setTextSize(13);
        detail.setGravity(Gravity.CENTER);
        LayoutParams detailParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        detailParams.topMargin = 10;
        root.addView(detail, detailParams);
        return root;
    }
}
