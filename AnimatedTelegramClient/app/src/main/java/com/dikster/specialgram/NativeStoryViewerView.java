package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Native Java story viewer content hosted inside the platform dialog. */
public final class NativeStoryViewerView extends LinearLayout {
    public interface Listener {
        void onDismiss();
        void onPrevious();
        void onNext();
        void onReact();
        void onReply(String text);
    }

    private final TextView owner;
    private final TextView position;
    private final Button close;
    private final LinearLayout progressBars;
    private final FrameLayout mediaFrame;
    private final TextView caption;
    private final Button previous;
    private final Button react;
    private final Button next;
    private final EditText reply;
    private final Button send;
    private final ProgressBar loading;
    private final ImageView image;
    private final VideoViewHolder videoHolder;
    private TgStory story;
    private int storyPosition;
    private int storyCount;
    private Listener listener;
    private String mediaKey = "";
    private Bitmap bitmap;

    private static final class VideoViewHolder {
        final android.widget.VideoView view;
        VideoViewHolder(Context context) { view = new android.widget.VideoView(context); }
    }

    public NativeStoryViewerView(Context context, TgStory story, int storyPosition,
                                 int storyCount, Listener listener) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(dp(14), dp(8), dp(14), dp(10));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(9, 11, 18));
        background.setCornerRadius(dp(20));
        setBackground(background);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        addView(header, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        owner = new TextView(context);
        owner.setTextColor(Color.WHITE);
        owner.setTextSize(15);
        owner.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(owner, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        position = new TextView(context);
        position.setTextColor(0xBFFFFFFF);
        position.setTextSize(11);
        header.addView(position);
        close = button(context, "Закрыть");
        header.addView(close);
        close.setOnClickListener(view -> { if (this.listener != null) this.listener.onDismiss(); });

        progressBars = new LinearLayout(context);
        progressBars.setOrientation(HORIZONTAL);
        LayoutParams barsParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(3));
        barsParams.topMargin = dp(7);
        addView(progressBars, barsParams);

        mediaFrame = new FrameLayout(context);
        LayoutParams mediaParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        mediaParams.topMargin = dp(8);
        mediaParams.bottomMargin = dp(4);
        addView(mediaFrame, mediaParams);
        image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mediaFrame.addView(image, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        videoHolder = new VideoViewHolder(context);
        loading = new ProgressBar(context);
        FrameLayout.LayoutParams loadingParams = new FrameLayout.LayoutParams(dp(42), dp(42), Gravity.CENTER);
        mediaFrame.addView(loading, loadingParams);

        TextView unsupported = new TextView(context);
        unsupported.setText("Этот тип истории пока не поддерживается");
        unsupported.setTextColor(Color.WHITE);
        unsupported.setGravity(Gravity.CENTER);
        unsupported.setTag("unsupported");
        FrameLayout.LayoutParams unsupportedParams = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mediaFrame.addView(unsupported, unsupportedParams);
        unsupported.setVisibility(GONE);

        mediaFrame.setOnTouchListener((view, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP && this.listener != null) {
                if (event.getX() < view.getWidth() / 2f) this.listener.onPrevious();
                else this.listener.onNext();
            }
            return true;
        });

        caption = new TextView(context);
        caption.setTextColor(Color.WHITE);
        caption.setTextSize(14);
        caption.setPadding(dp(2), dp(5), dp(2), dp(5));
        addView(caption, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout navigation = new LinearLayout(context);
        navigation.setGravity(Gravity.CENTER_VERTICAL);
        addView(navigation, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        previous = button(context, "Назад");
        navigation.addView(previous);
        react = button(context, "❤️");
        react.setTextSize(21);
        navigation.addView(react);
        View spacer = new View(context);
        navigation.addView(spacer, new LayoutParams(0, 1, 1f));
        next = button(context, "Далее");
        navigation.addView(next);
        previous.setOnClickListener(view -> { if (this.listener != null) this.listener.onPrevious(); });
        react.setOnClickListener(view -> { if (this.listener != null) this.listener.onReact(); });
        next.setOnClickListener(view -> { if (this.listener != null) this.listener.onNext(); });

        LinearLayout replyRow = new LinearLayout(context);
        replyRow.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams replyParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        replyParams.topMargin = dp(5);
        addView(replyRow, replyParams);
        reply = new EditText(context);
        reply.setSingleLine(true);
        reply.setTextSize(14);
        reply.setTextColor(Color.WHITE);
        reply.setHintTextColor(0xFFB0B0B0);
        reply.setHint("Ответить на историю…");
        reply.setImeOptions(EditorInfo.IME_ACTION_SEND);
        GradientDrawable inputBackground = new GradientDrawable();
        inputBackground.setColor(Color.TRANSPARENT);
        inputBackground.setCornerRadius(dp(24));
        inputBackground.setStroke(dp(1), 0x59FFFFFF);
        reply.setBackground(inputBackground);
        reply.setPadding(dp(14), dp(8), dp(14), dp(8));
        replyRow.addView(reply, new LayoutParams(0, dp(46), 1f));
        send = button(context, "Отпр.");
        send.setTextColor(0xFF42C7FF);
        replyRow.addView(send);
        send.setOnClickListener(view -> submitReply());
        reply.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) {
                send.setEnabled(value != null && value.toString().trim().length() > 0);
            }
            @Override public void afterTextChanged(android.text.Editable value) { }
        });
        reply.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                submitReply();
                return true;
            }
            return false;
        });
        update(story, storyPosition, storyCount, listener);
    }

    public void update(TgStory value, int positionValue, int countValue, Listener listener) {
        boolean newStory = story == null || story.getChatId() != value.getChatId()
                || story.getStoryId() != value.getStoryId();
        this.story = value;
        this.storyPosition = positionValue;
        this.storyCount = Math.max(1, countValue);
        this.listener = listener;
        if (newStory) reply.setText("");
        owner.setText(value.getOwnerName());
        position.setText(positionValue + "/" + Math.max(1, countValue));
        previous.setEnabled(positionValue > 1);
        next.setEnabled(positionValue < countValue);
        caption.setText(value.getCaption());
        caption.setVisibility(value.getCaption().isEmpty() ? GONE : VISIBLE);
        updateProgressBars();
        updateMedia(value);
        send.setEnabled(!reply.getText().toString().trim().isEmpty());
        reply.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                submitReply();
                return true;
            }
            return false;
        });
    }

    private void updateProgressBars() {
        progressBars.removeAllViews();
        int count = Math.max(1, storyCount);
        for (int i = 0; i < count; i++) {
            View bar = new View(getContext());
            bar.setBackgroundColor(i < storyPosition ? Color.WHITE : 0x38FFFFFF);
            LayoutParams params = new LayoutParams(0, dp(3), 1f);
            if (i > 0) params.leftMargin = dp(3);
            progressBars.addView(bar, params);
        }
    }

    private void updateMedia(TgStory value) {
        String key = value.getChatId() + ":" + value.getStoryId() + ":" + value.getPath() + ":" + value.isVideo();
        if (key.equals(mediaKey)) return;
        mediaKey = key;
        stopVideo();
        image.setVisibility(GONE);
        loading.setVisibility(GONE);
        View unsupported = mediaFrame.findViewWithTag("unsupported");
        if (unsupported != null) unsupported.setVisibility(GONE);
        image.setImageDrawable(null);
        if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        bitmap = null;
        if (value.getPath().isEmpty()) {
            loading.setVisibility(VISIBLE);
        } else if ("unsupported".equals(value.getPath())) {
            if (unsupported != null) unsupported.setVisibility(VISIBLE);
        } else if (value.isVideo()) {
            videoHolder.view.setVideoPath(value.getPath());
            videoHolder.view.setOnPreparedListener(player -> {
                player.setLooping(true);
                videoHolder.view.start();
            });
            videoHolder.view.setVisibility(VISIBLE);
            mediaFrame.addView(videoHolder.view, 0, new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        } else {
            try { bitmap = BitmapFactory.decodeFile(value.getPath()); }
            catch (RuntimeException ignored) { bitmap = null; }
            if (bitmap != null) {
                image.setImageBitmap(bitmap);
                image.setVisibility(VISIBLE);
            } else {
                loading.setVisibility(VISIBLE);
            }
        }
    }

    private void submitReply() {
        String value = reply.getText().toString().trim();
        if (value.isEmpty() || listener == null) return;
        listener.onReply(value);
        reply.setText("");
    }

    private void stopVideo() {
        try { videoHolder.view.stopPlayback(); } catch (RuntimeException ignored) { }
        videoHolder.view.setVisibility(GONE);
        mediaFrame.removeView(videoHolder.view);
    }

    private Button button(Context context, String text) {
        Button button = new Button(context);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(13);
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (story != null) updateMedia(story);
    }

    @Override protected void onDetachedFromWindow() {
        stopVideo();
        image.setImageDrawable(null);
        if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        bitmap = null;
        mediaKey = "";
        super.onDetachedFromWindow();
    }
}
