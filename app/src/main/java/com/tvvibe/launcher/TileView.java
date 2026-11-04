package com.tvvibe.launcher;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A rounded 16:9 card in the Apple TV style: it lifts and grows when focused,
 * and its name fades in underneath.
 */
final class TileView extends LinearLayout {

    private static final float FOCUS_SCALE = 1.12f;

    final FrameLayout card;
    final ImageView image;
    final TextView label;
    private final float density;
    private final int cardHeight;
    /** Colour used for the blurred backdrop when this tile has no banner. */
    int accentColor = Color.rgb(60, 60, 70);

    TileView(Context c, int width, int height) {
        super(c);
        density = getResources().getDisplayMetrics().density;
        cardHeight = height;
        setOrientation(VERTICAL);
        setFocusable(true);
        setClipChildren(false);
        setClipToPadding(false);
        if (Build.VERSION.SDK_INT >= 26) {
            setDefaultFocusHighlightEnabled(false); // we draw our own focus effect
        }

        card = new FrameLayout(c);
        final float radius = 14 * density;
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
            }
        });
        card.setClipToOutline(true);
        card.setElevation(3 * density);

        image = new ImageView(c);
        card.addView(image, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        addView(card, new LayoutParams(width, height));

        label = new TextView(c);
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER);
        label.setAlpha(0f);
        LayoutParams lp = new LayoutParams(width, LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (12 * density);
        addView(label, lp);
    }

    void bind(AppEntry e) {
        label.setText(e.label);
        setContentDescription(e.label);
        accentColor = Graphics.dominantColor(e.icon);
        if (e.banner != null) {
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setPadding(0, 0, 0, 0);
            image.setImageDrawable(e.banner);
            card.setBackgroundColor(Color.BLACK);
        } else {
            showIcon(e.icon, accentColor);
        }
    }

    /** For built-in tiles such as Settings. */
    void bindSpecial(String text, Drawable icon, int color) {
        label.setText(text);
        setContentDescription(text);
        accentColor = color;
        showIcon(icon, color);
    }

    private void showIcon(Drawable icon, int color) {
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Graphics.tone(color, 0.35f, 0.62f), Graphics.tone(color, 0.45f, 0.30f)});
        card.setBackground(bg);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int p = (int) (cardHeight * 0.24f);
        image.setPadding(p, p, p, p);
        image.setImageDrawable(icon);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        float s = gainFocus ? FOCUS_SCALE : 1f;
        DecelerateInterpolator ease = new DecelerateInterpolator(2f);
        card.animate().scaleX(s).scaleY(s)
                .translationZ(gainFocus ? 22 * density : 0)
                .setDuration(200).setInterpolator(ease).start();
        // Push the label down so the enlarged card doesn't cover it.
        float drop = gainFocus ? cardHeight * (FOCUS_SCALE - 1f) / 2f : 0f;
        label.animate().alpha(gainFocus ? 1f : 0f).translationY(drop)
                .setDuration(200).setInterpolator(ease).start();
    }
}
