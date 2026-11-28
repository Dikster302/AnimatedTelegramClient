package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Native Java rendering for a single chat message bubble. */
public final class NativeMessageBubbleView extends LinearLayout {
    public interface Listener {
        void onLongPress();
        void onReaction(String emoji);
    }

    private TgMessage message;
    private Listener listener;
    private int textColor, mutedColor, accentColor, incomingColor, outgoingColor, panelColor;
    private float radius;
    private Bitmap bitmap;
    private VideoView video;

    public NativeMessageBubbleView(Context context, TgMessage message, int textColor,
                                   int mutedColor, int accentColor, int incomingColor,
                                   int outgoingColor, int panelColor, float radius,
                                   boolean groupedPrevious, boolean groupedNext, Listener listener) {
        super(context);
        update(message, textColor, mutedColor, accentColor, incomingColor, outgoingColor,
                panelColor, radius, groupedPrevious, groupedNext, listener);
    }

    public void update(TgMessage value, int text, int muted, int accent, int incoming,
                       int outgoing, int panel, float bubbleRadius, boolean groupedPrevious,
                       boolean groupedNext, Listener callback) {
        this.listener = callback;
        boolean changed = message == null || !message.equals(value) || textColor != text
                || mutedColor != muted || accentColor != accent || incomingColor != incoming
                || outgoingColor != outgoing || panelColor != panel || radius != bubbleRadius
                || groupedPrevious != previousGrouped || groupedNext != nextGrouped;
        this.textColor = text; this.mutedColor = muted; this.accentColor = accent;
        this.incomingColor = incoming; this.outgoingColor = outgoing;
        this.panelColor = panel; this.radius = bubbleRadius;
        this.previousGrouped = groupedPrevious; this.nextGrouped = groupedNext;
        if (changed) {
            this.message = value;
            render();
        }
    }

    private boolean previousGrouped, nextGrouped;

    private void render() {
        stopMedia();
        removeAllViews();
        setOrientation(HORIZONTAL);
        setGravity(message.getOutgoing() ? Gravity.END : Gravity.START);
        LinearLayout bubble = new LinearLayout(getContext());
        bubble.setOrientation(VERTICAL);
        bubble.setPadding(dp(11), dp(7), dp(11), dp(7));
        float r = dp(radius);
        float small = dp(4);
        float topLeft = !message.getOutgoing() && previousGrouped ? small : r;
        float topRight = message.getOutgoing() && previousGrouped ? small : r;
        float bottomLeft = !message.getOutgoing() && nextGrouped ? small : r;
        float bottomRight = message.getOutgoing() && nextGrouped ? small : r;
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(message.getOutgoing() ? outgoingColor : incomingColor);
        shape.setCornerRadii(new float[]{topLeft,topLeft,topRight,topRight,
                bottomRight,bottomRight,bottomLeft,bottomLeft});
        bubble.setBackground(shape);
        bubble.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongPress();
            return true;
        });

        if (message.getReplyToMessageId() != 0L) {
            TextView reply = text("↪ " + (empty(message.getReplyToText()) ? "Сообщение" : message.getReplyToText()),
                    accentColor, 12, false);
            reply.setMaxLines(1);
            reply.setEllipsize(android.text.TextUtils.TruncateAt.END);
            bubble.addView(reply);
        }

        String kind = safe(message.getMediaKind());
        if (!safe(message.getPhotoPath()).isEmpty() && "photo".equals(kind)) {
            addImage(bubble, message.getPhotoPath(), 240, 300, true);
            if (!empty(message.getText()) && !"📷 Фото".equals(message.getText()))
                bubble.addView(text(message.getText(), textColor, 15, false));
        } else if ("sticker".equals(kind)) {
            if (!empty(message.getMediaPath())) addImage(bubble, message.getMediaPath(), 156, 156, false);
            else bubble.addView(text("Стикер загружается…", mutedColor, 13, false));
        } else if ("document".equals(kind)) {
            addDocument(bubble);
            if (!empty(message.getText()) && !message.getText().startsWith("📎"))
                bubble.addView(text(message.getText(), textColor, 14, false));
        } else if ("audio".equals(kind) || "voice".equals(kind)) {
            NativeAudioMessagePlayerView player = new NativeAudioMessagePlayerView(getContext(),
                    message.getMediaPath(), empty(message.getMediaName()) ? message.getText() : message.getMediaName(),
                    message.getMediaDuration(), textColor, mutedColor, accentColor);
            bubble.addView(player, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        } else if ("animation".equals(kind) && !empty(message.getMediaPath())) {
            addVideo(bubble, message.getMediaPath(), false);
            if (!empty(message.getText()) && !"GIF".equals(message.getText()))
                bubble.addView(text(message.getText(), textColor, 14, false));
        } else if (!empty(message.getVideoPath())) {
            addVideo(bubble, message.getVideoPath(), true);
            bubble.addView(text(message.getText(), textColor, 12, false));
        } else {
            TextView body = text(message.getText(), textColor, 16, false);
            body.setLineSpacing(dp(2), 1f);
            bubble.addView(body);
        }

        LinearLayout footer = new LinearLayout(getContext());
        footer.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        TextView time = text(formatTime(message.getDate()), mutedColor, 11, false);
        footer.addView(time);
        if (message.getOutgoing())
            footer.addView(text(message.isRead() ? "  ✓✓" : "  ✓",
                    message.isRead() ? accentColor : mutedColor, 11, false));
        if (message.isPinned())
            footer.addView(text("📌 ", mutedColor, 10, false), 0);
        LayoutParams footerParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        footerParams.topMargin = dp(2);
        bubble.addView(footer, footerParams);

        if (message.getReactions() != null && !message.getReactions().isEmpty()) {
            LinearLayout reactions = new LinearLayout(getContext());
            reactions.setOrientation(HORIZONTAL);
            reactions.setPadding(0, dp(5), 0, 0);
            for (TgReaction reaction : message.getReactions()) {
                TextView chip = text(reaction.getEmoji() + " " + reaction.getCount(),
                        reaction.getChosen() ? accentColor : textColor, 12, true);
                chip.setPadding(dp(8), dp(4), dp(8), dp(4));
                GradientDrawable chipBg = new GradientDrawable();
                chipBg.setColor(reaction.getChosen() ? withAlpha(accentColor, 0.28f)
                        : withAlpha(panelColor, 0.80f));
                chipBg.setCornerRadius(dp(30));
                chip.setBackground(chipBg);
                chip.setOnClickListener(v -> { if (listener != null) listener.onReaction(reaction.getEmoji()); });
                LayoutParams cp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                cp.rightMargin = dp(5);
                reactions.addView(chip, cp);
            }
            bubble.addView(reactions);
        }
        addView(bubble, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    private void addDocument(LinearLayout parent) {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(6), dp(8), dp(6));
        row.setOnClickListener(v -> MediaPreparation.openDownloadedMedia(getContext(),
                message.getMediaPath(), message.getMediaMimeType()));
        TextView icon = text("📎", textColor, 27, false);
        row.addView(icon);
        LinearLayout labels = new LinearLayout(getContext());
        labels.setOrientation(VERTICAL);
        LayoutParams labelParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        labelParams.leftMargin = dp(10);
        labels.addView(text(empty(message.getMediaName()) ? "Файл" : message.getMediaName(), textColor, 14, false));
        String state = empty(message.getMediaPath()) ? "Загрузка…" : "Открыть файл";
        labels.addView(text(MediaPreparation.formatFileSize(message.getMediaSize()) + " · " + state,
                mutedColor, 11, false));
        row.addView(labels, labelParams);
        parent.addView(row);
    }

    private void addImage(LinearLayout parent, String path, int width, int maxHeight, boolean open) {
        try { bitmap = BitmapFactory.decodeFile(path); } catch (RuntimeException ignored) { bitmap = null; }
        if (bitmap == null) {
            parent.addView(text("Медиафайл загружается…", mutedColor, 13, false));
            return;
        }
        ImageView image = new ImageView(getContext());
        image.setImageBitmap(bitmap);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setAdjustViewBounds(true);
        image.setMaxHeight(dp(maxHeight));
        image.setMaxWidth(dp(width));
        image.setBackground(roundBackground(0x00000000, 12));
        image.setClipToOutline(true);
        if (open) image.setOnClickListener(v -> MediaPreparation.openDownloadedMedia(getContext(),
                empty(message.getMediaPath()) ? path : message.getMediaPath(), "image/*"));
        parent.addView(image, new LayoutParams(dp(width), LayoutParams.WRAP_CONTENT));
    }

    private void addVideo(LinearLayout parent, String path, boolean customShape) {
        video = new VideoView(getContext());
        video.setVideoPath(path);
        video.setOnPreparedListener(player -> { player.setLooping(true); video.start(); });
        if (customShape && "circle".equals(message.getVideoShape())) {
            video.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
            video.setClipToOutline(true);
        } else if (customShape && "rounded".equals(message.getVideoShape())) {
            video.setBackground(roundBackground(Color.TRANSPARENT, 22));
            video.setClipToOutline(true);
        }
        parent.addView(video, new LayoutParams(dp(224), dp(224)));
    }

    private void stopMedia() {
        if (video != null) {
            try { video.stopPlayback(); } catch (RuntimeException ignored) { }
            video = null;
        }
        if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        bitmap = null;
    }

    private GradientDrawable roundBackground(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private TextView text(String value, int color, int size, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(value == null ? "" : value);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setMaxWidth(dp(290));
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private String formatTime(int timestamp) {
        try { return new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date(timestamp * 1000L)); }
        catch (RuntimeException ignored) { return ""; }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static int withAlpha(int color, float alpha) {
        return (color & 0x00FFFFFF) | (Math.round(Color.alpha(color) * alpha) << 24);
    }
    private static String safe(String value) { return value == null ? "" : value; }
    private static boolean empty(String value) { return value == null || value.isEmpty(); }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (message != null) render();
    }

    @Override protected void onDetachedFromWindow() {
        stopMedia();
        super.onDetachedFromWindow();
    }
}
