package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.function.Consumer;

/** Native Android Views for the authentication and connection entry panels. */
public final class NativeEntryPanel extends LinearLayout {
    private final ImageView logo;
    private final TextView titleView;
    private final TextView subtitleView;
    private final EditText input;
    private final Button submit;
    private Consumer<String> valueListener;
    private Runnable submitListener;
    private boolean changingText;
    private Boolean secretMode;

    public NativeEntryPanel(Context context, String title, String subtitle, String value,
                            String buttonText, boolean secret, int backgroundColor,
                            int textColor, int mutedColor, int accentColor,
                            Consumer<String> onValue, Runnable onSubmit) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        int horizontal = dp(context, 26);
        setPadding(horizontal, dp(context, 24), horizontal, dp(context, 24));
        setBackgroundColor(backgroundColor);

        logo = new ImageView(context);
        logo.setImageResource(R.drawable.ic_launcher_foreground);
        logo.setContentDescription("Specialgram");
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LayoutParams logoParams = new LayoutParams(dp(context, 92), dp(context, 92));
        addView(logo, logoParams);

        titleView = new TextView(context);
        titleView.setTextSize(24);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleView.setGravity(Gravity.CENTER);
        LayoutParams titleParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(context, 25);
        addView(titleView, titleParams);

        subtitleView = new TextView(context);
        subtitleView.setTextSize(14);
        subtitleView.setGravity(Gravity.CENTER);
        LayoutParams subtitleParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(context, 8);
        addView(subtitleView, subtitleParams);

        input = new EditText(context);
        input.setSingleLine(true);
        input.setTextSize(16);
        input.setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12));
        LayoutParams inputParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 54));
        inputParams.topMargin = dp(context, 24);
        addView(input, inputParams);

        submit = new Button(context);
        submit.setAllCaps(false);
        submit.setTextSize(15);
        LayoutParams buttonParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 50));
        buttonParams.topMargin = dp(context, 17);
        addView(submit, buttonParams);

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!changingText && valueListener != null) valueListener.accept(s.toString());
                submit.setEnabled(s != null && s.toString().trim().length() > 0);
            }
            @Override public void afterTextChanged(Editable editable) { }
        });
        submit.setOnClickListener(view -> {
            if (submitListener != null && input.getText().toString().trim().length() > 0)
                submitListener.run();
        });
        update(title, subtitle, value, buttonText, secret, backgroundColor,
                textColor, mutedColor, accentColor, onValue, onSubmit);
    }

    public void update(String title, String subtitle, String value, String buttonText,
                       boolean secret, int backgroundColor, int textColor, int mutedColor,
                       int accentColor, Consumer<String> onValue, Runnable onSubmit) {
        valueListener = onValue;
        submitListener = onSubmit;
        setBackgroundColor(backgroundColor);
        titleView.setText(title);
        titleView.setTextColor(textColor);
        subtitleView.setText(subtitle);
        subtitleView.setTextColor(mutedColor);
        input.setTextColor(textColor);
        input.setHintTextColor(mutedColor);
        input.setHint(" ");
        if (secretMode == null || secretMode.booleanValue() != secret) {
            input.setInputType(secret
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            secretMode = secret;
        }
        GradientDrawable fieldBackground = new GradientDrawable();
        fieldBackground.setColor(Color.TRANSPARENT);
        fieldBackground.setCornerRadius(dp(getContext(), 12));
        fieldBackground.setStroke(dp(getContext(), 1), mutedColor);
        input.setBackground(fieldBackground);
        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setColor(accentColor);
        buttonBackground.setCornerRadius(dp(getContext(), 12));
        submit.setBackground(buttonBackground);
        submit.setTextColor(Color.WHITE);
        submit.setText(buttonText);
        String current = input.getText().toString();
        if (!current.equals(value == null ? "" : value)) {
            changingText = true;
            input.setText(value == null ? "" : value);
            input.setSelection(input.getText().length());
            changingText = false;
        }
        submit.setEnabled(input.getText().toString().trim().length() > 0);
    }

    public static View loading(Context context, String title, String subtitle,
                               int backgroundColor, int textColor, int mutedColor, int accentColor) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(context, 28), dp(context, 28), dp(context, 28), dp(context, 28));
        root.setBackgroundColor(backgroundColor);
        ProgressBar progress = new ProgressBar(context);
        progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(accentColor));
        root.addView(progress, new LayoutParams(dp(context, 42), dp(context, 42)));
        TextView heading = new TextView(context);
        heading.setText(title);
        heading.setTextColor(textColor);
        heading.setTextSize(19);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setGravity(Gravity.CENTER);
        LayoutParams headingParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        headingParams.topMargin = dp(context, 22);
        root.addView(heading, headingParams);
        TextView detail = new TextView(context);
        detail.setText(subtitle);
        detail.setTextColor(mutedColor);
        detail.setTextSize(13);
        detail.setGravity(Gravity.CENTER);
        LayoutParams detailParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        detailParams.topMargin = dp(context, 8);
        root.addView(detail, detailParams);
        return root;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
