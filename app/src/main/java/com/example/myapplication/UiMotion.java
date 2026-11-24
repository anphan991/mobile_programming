package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public final class UiMotion {

    private static final DecelerateInterpolator DECELERATE = new DecelerateInterpolator(1.6f);
    private static final OvershootInterpolator OVERSHOOT = new OvershootInterpolator(1.5f);

    private UiMotion() {}

    public static String currentUser(Context ctx) {
        if (ctx == null) return "admin";
        String u = ctx.getSharedPreferences("UserSession", Context.MODE_PRIVATE).getString("CURRENT_USER", "admin");
        return (u == null || u.trim().isEmpty()) ? "admin" : u.trim();
    }

    public static void logout(AppCompatActivity act) {
        FirebaseAuth.getInstance().signOut();
        CartManager.onLogout();
        act.getSharedPreferences("UserSession", Context.MODE_PRIVATE).edit().clear().apply();
        Toast.makeText(act, "Đã đăng xuất tài khoản!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(act, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        act.startActivity(intent);
        act.finish();
    }

    public static void bindStatusBadge(Context ctx, TextView badge, int step) {
        if (ctx == null || badge == null) return;
        boolean cancelled = step == Order.STEP_CANCELLED, delivered = step >= Order.STEP_DELIVERED;
        badge.setText(Order.getStepTitle(step));
        badge.setBackgroundResource(cancelled ? R.drawable.bg_badge_danger : (delivered ? R.drawable.bg_badge_success : R.drawable.bg_badge_primary));
        badge.setTextColor(ContextCompat.getColor(ctx, cancelled ? R.color.danger : (delivered ? R.color.success : R.color.primary_dark)));
    }

    public static void bindStockBadge(Context ctx, TextView badge, boolean inStock) {
        if (ctx == null || badge == null) return;
        badge.setText(inStock ? "Còn hàng" : "Hết hàng");
        badge.setBackgroundResource(inStock ? R.drawable.bg_badge_success : R.drawable.bg_badge_danger);
        badge.setTextColor(ContextCompat.getColor(ctx, inStock ? R.color.success : R.color.danger));
    }

    public static void styleTabButton(Context ctx, MaterialButton btn, boolean active) {
        if (ctx == null || btn == null) return;
        btn.setBackgroundTintList(ContextCompat.getColorStateList(ctx, active ? R.color.primary : R.color.surface));
        btn.setTextColor(ContextCompat.getColor(ctx, active ? R.color.white : R.color.ink));
        btn.setStrokeColor(ContextCompat.getColorStateList(ctx, R.color.border));
        btn.setStrokeWidth(active ? 0 : Math.round(ctx.getResources().getDisplayMetrics().density));
    }

    public static void applyInsets(View root) {
        if (root == null) return;
        int l = root.getPaddingLeft(), t = root.getPaddingTop(), r = root.getPaddingRight(), b = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(l + bars.left, t + bars.top, r + bars.right, b + bars.bottom);
            return insets;
        });
    }

    public static void applyAuthInsets(View root, View header) {
        if (root == null || header == null) return;
        int ht = header.getPaddingTop(), l = root.getPaddingLeft(), r = root.getPaddingRight(), b = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            header.setPadding(header.getPaddingLeft(), ht + bars.top, header.getPaddingRight(), header.getPaddingBottom());
            v.setPadding(l + bars.left, 0, r + bars.right, b + bars.bottom);
            return insets;
        });
    }

    public static boolean isReducedMotion(Context context) {
        if (context == null) return false;
        try {
            return Settings.Global.getFloat(context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void bindTouchScale(View... views) {
        if (views == null) return;
        for (View view : views) {
            if (view == null) continue;
            view.setOnTouchListener((v, e) -> {
                if (!v.isEnabled() || isReducedMotion(v.getContext())) return false;
                int a = e.getActionMasked();
                if (a == MotionEvent.ACTION_DOWN) {
                    v.animate().cancel();
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(110L).setInterpolator(DECELERATE).start();
                } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                    v.animate().cancel();
                    v.animate().scaleX(1f).scaleY(1f).setDuration(220L).setInterpolator(OVERSHOOT).start();
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
                v.setAlpha(1f);
                v.setTranslationY(0f);
                continue;
            }
            v.animate().cancel();
            v.setAlpha(0f);
            v.setTranslationY(14f * v.getResources().getDisplayMetrics().density);
            v.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(230L).setInterpolator(DECELERATE).start();
            delay += 40L;
        }
    }

    public static void animateContentSwitch(View view) {
        if (view == null || isReducedMotion(view.getContext())) return;
        view.animate().cancel();
        view.setAlpha(0.35f);
        view.setTranslationY(8f * view.getResources().getDisplayMetrics().density);
        view.animate().alpha(1f).translationY(0f).setDuration(180L).setInterpolator(DECELERATE).start();
    }

    public static void animateBadgePop(View badgeView) {
        if (badgeView == null) return;
        badgeView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        if (isReducedMotion(badgeView.getContext())) return;
        badgeView.animate().cancel();
        badgeView.setScaleX(0.7f);
        badgeView.setScaleY(0.7f);
        badgeView.animate().scaleX(1f).scaleY(1f).setDuration(240L).setInterpolator(OVERSHOOT).start();
    }

    public static void togglePanel(View panel, boolean show) {
        if (panel == null) return;
        panel.animate().cancel();
        if (isReducedMotion(panel.getContext())) {
            panel.setVisibility(show ? View.VISIBLE : View.GONE);
            return;
        }
        float offset = -10f * panel.getResources().getDisplayMetrics().density;
        if (show) {
            panel.setAlpha(0f);
            panel.setTranslationY(offset);
            panel.setVisibility(View.VISIBLE);
            panel.animate().alpha(1f).translationY(0f).setDuration(180L).setInterpolator(DECELERATE).start();
        } else {
            panel.animate().alpha(0f).translationY(offset).setDuration(140L).setInterpolator(DECELERATE)
                    .withEndAction(() -> {
                        panel.setVisibility(View.GONE);
                        panel.setAlpha(1f);
                        panel.setTranslationY(0f);
                    }).start();
        }
    }
}
