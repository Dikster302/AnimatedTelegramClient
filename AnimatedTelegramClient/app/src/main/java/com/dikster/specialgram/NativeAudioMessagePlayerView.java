package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Native Java audio player used inside message bubbles. */
public final class NativeAudioMessagePlayerView extends LinearLayout {
    private final TextView playButton;
    private final TextView titleView;
    private final TextView detailView;
    private String path = "";
    private String title = "";
    private int duration;
    private int textColor = Color.WHITE;
    private int mutedColor = Color.LTGRAY;
    private int accentColor = Color.CYAN;
    private MediaPlayer player;
    private boolean prepared;
    private boolean playing;
    private boolean pendingPlay;
    private boolean failed;

    public NativeAudioMessagePlayerView(Context context, String path, String title, int duration,
                                         int textColor, int mutedColor, int accentColor) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setMinimumWidth(dp(210));
        setPadding(0, dp(3), 0, dp(3));

        playButton = new TextView(context);
        playButton.setTextSize(22);
        playButton.setGravity(Gravity.CENTER);
        playButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        playButton.setMinWidth(dp(42));
        playButton.setMinHeight(dp(42));
        addView(playButton, new LayoutParams(dp(42), dp(42)));

        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(VERTICAL);
        LayoutParams labelsParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        labelsParams.leftMargin = dp(6);
        addView(labels, labelsParams);

        titleView = new TextView(context);
        titleView.setTextSize(13);
        titleView.setSingleLine(true);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labels.addView(titleView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        detailView = new TextView(context);
        detailView.setTextSize(11);
        LayoutParams detailParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        detailParams.topMargin = dp(2);
        labels.addView(detailView, detailParams);

        playButton.setOnClickListener(view -> togglePlayback());
        update(path, title, duration, textColor, mutedColor, accentColor);
    }

    public void update(String newPath, String newTitle, int newDuration,
                       int newTextColor, int newMutedColor, int newAccentColor) {
        String safePath = newPath == null ? "" : newPath;
        boolean pathChanged = !safePath.equals(path);
        path = safePath;
        title = newTitle == null ? "" : newTitle;
        duration = newDuration;
        textColor = newTextColor;
        mutedColor = newMutedColor;
        accentColor = newAccentColor;
        titleView.setText(title.isEmpty() ? "Аудиосообщение" : title);
        titleView.setTextColor(textColor);
        if (pathChanged) { releasePlayer(); failed = false; }
        if (isAttachedToWindow() && !path.isEmpty() && player == null && !failed) preparePlayer();
        renderState();
    }

    private void togglePlayback() {
        if (path.isEmpty()) return;
        if (!prepared) {
            pendingPlay = true;
            renderState();
            return;
        }
        try {
            if (playing) {
                player.pause();
                playing = false;
            } else {
                player.start();
                playing = true;
            }
        } catch (IllegalStateException ignored) {
            playing = false;
        }
        renderState();
    }

    private void preparePlayer() {
        if (path.isEmpty() || player != null || failed) return;
        MediaPlayer media = new MediaPlayer();
        player = media;
        try {
            media.setDataSource(path);
            media.setOnPreparedListener(preparedPlayer -> {
                if (player != preparedPlayer) return;
                prepared = true;
                if (pendingPlay) {
                    pendingPlay = false;
                    try {
                        preparedPlayer.start();
                        playing = true;
                    } catch (IllegalStateException ignored) { playing = false; }
                }
                renderState();
            });
            media.setOnCompletionListener(completed -> {
                if (player != completed) return;
                playing = false;
                try { completed.seekTo(0); } catch (IllegalStateException ignored) { }
                renderState();
            });
            media.setOnErrorListener((failedPlayer, what, extra) -> {
                if (player == failedPlayer) {
                    releasePlayer();
                    failed = true;
                    renderState();
                }
                return true;
            });
            media.prepareAsync();
        } catch (Exception error) {
            releasePlayer();
            failed = true;
        }
        renderState();
    }

    private void renderState() {
        playButton.setText(playing ? "Ⅱ" : "▶");
        playButton.setTextColor(accentColor);
        playButton.setEnabled(!path.isEmpty());
        String detail;
        if (path.isEmpty()) detail = "Загрузка…";
        else if (prepared) detail = MediaPreparation.formatDuration(duration);
        else if (failed) detail = "Не удалось открыть аудио";
        else detail = "Подготовка аудио…";
        detailView.setText(detail);
        detailView.setTextColor(mutedColor);
    }

    private void releasePlayer() {
        pendingPlay = false;
        prepared = false;
        playing = false;
        if (player != null) {
            try { player.setOnPreparedListener(null); } catch (RuntimeException ignored) { }
            try { player.setOnCompletionListener(null); } catch (RuntimeException ignored) { }
            try { player.release(); } catch (RuntimeException ignored) { }
            player = null;
        }
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!path.isEmpty() && player == null && !failed) preparePlayer();
    }

    @Override protected void onDetachedFromWindow() {
        releasePlayer();
        super.onDetachedFromWindow();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
