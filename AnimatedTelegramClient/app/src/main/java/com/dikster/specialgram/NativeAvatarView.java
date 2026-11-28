package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** Canvas-rendered avatar with Telegram-style story ring and online indicator. */
public final class NativeAvatarView extends View {
    private static final int[] STORY_COLORS = {
            Color.rgb(66, 199, 255), Color.rgb(101, 117, 255),
            Color.rgb(179, 77, 255), Color.rgb(101, 117, 255),
            Color.rgb(66, 199, 255)
    };
    private static final int[] AVATAR_COLORS = {
            Color.rgb(91, 141, 239), Color.rgb(142, 106, 200),
            Color.rgb(38, 166, 154), Color.rgb(226, 139, 85),
            Color.rgb(204, 102, 136)
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path clipPath = new Path();
    private final Rect source = new Rect();
    private final RectF destination = new RectF();
    private String photoPath = "";
    private String fallback = "";
    private long identity;
    private int requestedSize;
    private int panelColor;
    private boolean online;
    private boolean hasStory;
    private boolean storyViewed;
    private Bitmap bitmap;

    public NativeAvatarView(Context context, String path, String fallback, long id, int size,
                            boolean online, boolean hasStory, boolean storyViewed, int panelColor) {
        super(context);
        update(path, fallback, id, size, online, hasStory, storyViewed, panelColor);
    }

    public void update(String path, String fallback, long id, int size,
                       boolean online, boolean hasStory, boolean storyViewed, int panelColor) {
        String safePath = path == null ? "" : path;
        if (!safePath.equals(photoPath)) {
            photoPath = safePath;
            if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
            bitmap = null;
            if (!photoPath.isEmpty()) {
                try { bitmap = BitmapFactory.decodeFile(photoPath); }
                catch (RuntimeException ignored) { bitmap = null; }
            }
        }
        this.fallback = fallback == null ? "" : fallback;
        this.identity = id;
        this.requestedSize = Math.max(1, size);
        this.online = online;
        this.hasStory = hasStory;
        this.storyViewed = storyViewed;
        this.panelColor = panelColor;
        int pixels = dp(this.requestedSize);
        if (getLayoutParams() == null) {
            setLayoutParams(new android.view.ViewGroup.LayoutParams(pixels, pixels));
        } else if (getLayoutParams().width != pixels || getLayoutParams().height != pixels) {
            getLayoutParams().width = pixels;
            getLayoutParams().height = pixels;
            requestLayout();
        }
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float size = Math.min(width, height);
        float centerX = width / 2f;
        float centerY = height / 2f;
        float ringWidth = dp(2.8f);
        float inset = hasStory ? dp(3.5f) : 0f;
        float radius = size / 2f - (hasStory ? ringWidth + inset : 0f);
        if (radius <= 0f) return;

        canvas.save();
        clipPath.reset();
        clipPath.addCircle(centerX, centerY, radius, Path.Direction.CW);
        canvas.clipPath(clipPath);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(AVATAR_COLORS[(int) Math.abs(identity % AVATAR_COLORS.length)]);
        canvas.drawCircle(centerX, centerY, radius, paint);

        if (bitmap != null && !bitmap.isRecycled()) {
            source.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
            destination.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
            paint.setColor(Color.WHITE);
            canvas.drawBitmap(bitmap, source, destination, paint);
        } else if (!fallback.isEmpty()) {
            paint.setShader(null);
            paint.setColor(Color.WHITE);
            paint.setTypeface(android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dp(requestedSize / 3f));
            Paint.FontMetrics metrics = paint.getFontMetrics();
            canvas.drawText(fallback, centerX,
                    centerY - (metrics.ascent + metrics.descent) / 2f, paint);
        }
        canvas.restore();

        if (hasStory) {
            paint.setShader(ShaderSweep(centerX, centerY,
                    storyViewed ? new int[]{Color.rgb(120, 130, 141), Color.rgb(120, 130, 141)}
                            : STORY_COLORS));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(ringWidth);
            canvas.drawCircle(centerX, centerY, size / 2f - ringWidth / 2f, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
        }

        if (online) {
            float outer = dp(requestedSize / 8f);
            float x = centerX + size / 2f - outer;
            float y = centerY + size / 2f - outer;
            paint.setColor(Color.rgb(53, 199, 89));
            canvas.drawCircle(x, y, outer, paint);
            paint.setColor(panelColor);
            canvas.drawCircle(x, y, Math.max(0f, outer - dp(2f)), paint);
            paint.setColor(Color.rgb(53, 199, 89));
            canvas.drawCircle(x, y, Math.max(0f, outer - dp(4f)), paint);
        }
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
    }

    private static Shader ShaderSweep(float x, float y, int[] colors) {
        return new android.graphics.SweepGradient(x, y, colors, null);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

}
