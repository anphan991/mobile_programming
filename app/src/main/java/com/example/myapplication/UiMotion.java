package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Hệ thống chuyển động & phản hồi chạm mượt mà theo chuẩn UI/UX Pro Max:
 * - Hiệu ứng nhấn đàn hồi (Tactile Press Scale 0.96 -> 1.0, 110ms/220ms)
 * - Chuyển cảnh & nạp thẻ Staggered Fade-Slide (180-240ms)
 * - Hiệu ứng nảy huy hiệu giỏ hàng + phản hồi xúc giác (Haptic Feedback) cho tác vụ chính
 * - Tự động tôn trọng cài đặt trợ năng tắt chuyển động (prefers-reduced-motion)
 */
public final class UiMotion {

    private static final float PRESS_SCALE = 0.96f;
    private static final long PRESS_DURATION_MS = 110L;
    private static final long RELEASE_DURATION_MS = 220L;
    private static final long ENTRY_DURATION_MS = 230L;
    private static final long SWITCH_DURATION_MS = 180L;

    private static final DecelerateInterpolator DECELERATE = new DecelerateInterpolator(1.6f);
    private static final OvershootInterpolator OVERSHOOT = new OvershootInterpolator(1.5f);

    private UiMotion() {}

    public static boolean isReducedMotion(Context context) {
        if (context == null) return false;
        try {
            float scale = Settings.Global.getFloat(
                    context.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1.0f
            );
            return scale == 0.0f;
        } catch (Exception ignored) {
            return false;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void bindTouchScale(View... views) {
        if (views == null) return;
        for (View view : views) {
            if (view == null) continue;
            view.setOnTouchListener((v, event) -> {
                if (!v.isEnabled() || isReducedMotion(v.getContext())) {
                    return false;
                }
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    v.animate()
                            .cancel();
                    v.animate()
                            .scaleX(PRESS_SCALE)
                            .scaleY(PRESS_SCALE)
                            .setDuration(PRESS_DURATION_MS)
                            .setInterpolator(DECELERATE)
                            .start();
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    v.animate()
                            .cancel();
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(RELEASE_DURATION_MS)
                            .setInterpolator(OVERSHOOT)
                            .start();
                }
                return false;
            });
        }
    }

    public static void animateEntry(View... views) {
        if (views == null) return;
        long delay = 0L;
        for (View v : views) {
            if (v == null || v.getVisibility() != View.VISIBLE) continue;
            if (isReducedMotion(v.getContext())) {
                v.setAlpha(1.0f);
                v.setTranslationY(0.0f);
                continue;
            }
            float offsetPx = 14f * v.getResources().getDisplayMetrics().density;
            v.animate().cancel();
            v.setAlpha(0.0f);
            v.setTranslationY(offsetPx);
            v.animate()
                    .alpha(1.0f)
                    .translationY(0.0f)
                    .setStartDelay(delay)
                    .setDuration(ENTRY_DURATION_MS)
                    .setInterpolator(DECELERATE)
                    .start();
            delay += 40L;
        }
    }

    public static void animateContentSwitch(View view) {
        if (view == null) return;
        if (isReducedMotion(view.getContext())) {
            view.setAlpha(1.0f);
            view.setTranslationY(0.0f);
            return;
        }
        float offsetPx = 8f * view.getResources().getDisplayMetrics().density;
        view.animate().cancel();
        view.setAlpha(0.35f);
        view.setTranslationY(offsetPx);
        view.animate()
                .alpha(1.0f)
                .translationY(0.0f)
                .setStartDelay(0L)
                .setDuration(SWITCH_DURATION_MS)
                .setInterpolator(DECELERATE)
                .start();
    }

    public static void animateBadgePop(View badgeView) {
        if (badgeView == null) return;
        badgeView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        if (isReducedMotion(badgeView.getContext())) {
            badgeView.setScaleX(1.0f);
            badgeView.setScaleY(1.0f);
            return;
        }
        badgeView.animate().cancel();
        badgeView.setScaleX(0.7f);
        badgeView.setScaleY(0.7f);
        badgeView.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(240L)
                .setInterpolator(OVERSHOOT)
                .start();
    }

    public static void togglePanel(View panel, boolean show) {
        if (panel == null) return;
        panel.animate().cancel();
        if (isReducedMotion(panel.getContext())) {
            panel.setAlpha(1.0f);
            panel.setTranslationY(0.0f);
            panel.setVisibility(show ? View.VISIBLE : View.GONE);
            return;
        }
        float offsetPx = -10f * panel.getResources().getDisplayMetrics().density;
        if (show) {
            panel.setAlpha(0.0f);
            panel.setTranslationY(offsetPx);
            panel.setVisibility(View.VISIBLE);
            panel.animate()
                    .alpha(1.0f)
                    .translationY(0.0f)
                    .setDuration(180L)
                    .setInterpolator(DECELERATE)
                    .start();
        } else {
            panel.animate()
                    .alpha(0.0f)
                    .translationY(offsetPx)
                    .setDuration(140L)
                    .setInterpolator(DECELERATE)
                    .withEndAction(() -> {
                        panel.setVisibility(View.GONE);
                        panel.setAlpha(1.0f);
                        panel.setTranslationY(0.0f);
                    })
                    .start();
        }
    }
}
