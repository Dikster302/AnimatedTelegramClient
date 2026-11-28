package com.dikster.specialgram;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.view.View;
import java.io.InputStream;

/** Canvas-rendered native header wallpaper, including the built-in cosmic cover. */
public final class NativeHeaderWallpaperView extends View {
    private static final float[][] STARS = {
            {.08f,.22f},{.17f,.71f},{.29f,.36f},{.38f,.82f},{.49f,.18f},{.57f,.56f},
            {.68f,.28f},{.76f,.76f},{.88f,.39f},{.94f,.15f},{.23f,.12f},{.83f,.90f}
    };
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Rect source = new Rect();
    private final RectF destination = new RectF();
    private String uri = "";
    private int panelColor = Color.BLACK;
    private Bitmap bitmap;

    public NativeHeaderWallpaperView(Context context, String uri, int panelColor) {
        super(context);
        update(uri, panelColor);
        setContentDescription("Фон верхней панели Specialgram");
    }

    public void update(String value, int color) {
        String next = value == null ? "" : value;
        boolean changed = !next.equals(uri);
        uri = next;
        panelColor = color;
        if (changed) {
            if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
            bitmap = null;
            if (!uri.isEmpty() && !"preset:cosmic".equals(uri)) {
                try (InputStream input = getContext().getContentResolver().openInputStream(Uri.parse(uri))) {
                    if (input != null) bitmap = BitmapFactory.decodeStream(input);
                } catch (Exception ignored) { bitmap = null; }
            }
        }
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth(), height = getHeight();
        if ("preset:cosmic".equals(uri)) {
            paint.setShader(new LinearGradient(0, 0, width, height,
                    new int[]{0xFF080B19,0xFF29204D,0xFF10152C}, null, Shader.TileMode.CLAMP));
            canvas.drawRect(0,0,width,height,paint);
            paint.setShader(null);
            for (int i=0;i<STARS.length;i++) {
                paint.setColor(i%3==0 ? 0xF2B9A8FF : 0xB8FFFFFF);
                float radius = (i%4==0 ? 2.2f : 1.1f) * getResources().getDisplayMetrics().density;
                canvas.drawCircle(width*STARS[i][0],height*STARS[i][1],radius,paint);
            }
            paint.setColor(0x33000000);
            canvas.drawRect(0,0,width,height,paint);
            return;
        }
        if (bitmap != null && !bitmap.isRecycled() && bitmap.getWidth()>0 && bitmap.getHeight()>0) {
            float scale=Math.max(width/bitmap.getWidth(),height/bitmap.getHeight());
            float drawWidth=bitmap.getWidth()*scale, drawHeight=bitmap.getHeight()*scale;
            float left=(width-drawWidth)/2f, top=(height-drawHeight)/2f;
            source.set(0,0,bitmap.getWidth(),bitmap.getHeight());
            destination.set(left,top,left+drawWidth,top+drawHeight);
            paint.setColor(Color.WHITE);
            canvas.drawBitmap(bitmap,source,destination,paint);
            paint.setColor(0x99000000);
            canvas.drawRect(0,0,width,height,paint);
        } else {
            paint.setColor(panelColor);
            canvas.drawRect(0,0,width,height,paint);
        }
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (bitmap == null && !uri.isEmpty() && !"preset:cosmic".equals(uri)) {
            String current = uri;
            uri = "";
            update(current, panelColor);
        }
    }

    @Override protected void onDetachedFromWindow() {
        if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        bitmap = null;
        super.onDetachedFromWindow();
    }
}
