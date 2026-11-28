package com.dikster.specialgram.anim;

import android.view.View;
import android.view.animation.OvershootInterpolator;

/** Общие короткие анимации нативного интерфейса Specialgram. */
public final class SpecialgramAnimations {
    private static final long TAB_DURATION_MS = 220L;
    private static final long ITEM_DURATION_MS = 260L;

    private SpecialgramAnimations() { }

    /** Пружинное выделение активной вкладки нижней навигации. */
    public static void animateTabSelection(View view) {
        if (view == null) return;
        view.animate().cancel();
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(TAB_DURATION_MS / 2)
                .setInterpolator(new OvershootInterpolator(2.2f))
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(TAB_DURATION_MS / 2)
                        .setInterpolator(new OvershootInterpolator(1.5f))
                        .start())
                .start();
    }

    /** Появление нового пузыря снизу с мягким overshoot. */
    public static void animateMessageIn(View view) {
        if (view == null) return;
        view.animate().cancel();
        view.setAlpha(0f);
        view.setTranslationY(dp(view, 12f));
        view.setScaleX(0.98f);
        view.setScaleY(0.98f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(ITEM_DURATION_MS)
                .setInterpolator(new OvershootInterpolator(0.8f))
                .start();
    }

    /** Появление счётчика непрочитанных сообщений. */
    public static void animateCounterIn(View view) {
        if (view == null) return;
        view.animate().cancel();
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setScaleX(0f);
        view.setScaleY(0f);
        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(ITEM_DURATION_MS)
                .setInterpolator(new OvershootInterpolator(1.7f))
                .start();
    }

    private static float dp(View view, float value) {
        return value * view.getResources().getDisplayMetrics().density;
    }
}
