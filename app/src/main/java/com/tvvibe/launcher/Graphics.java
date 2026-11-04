package com.tvvibe.launcher;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

/** Small drawing helpers: tile colours and the soft blurred backdrop. */
final class Graphics {

    private Graphics() {}

    /** Copies a drawable so drawing it here never disturbs the one shown on screen. */
    static Drawable copy(Drawable d) {
        Drawable.ConstantState cs = d.getConstantState();
        return cs != null ? cs.newDrawable().mutate() : d;
    }

    static Bitmap render(Drawable d, int w, int h, int backgroundColor) {
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.drawColor(backgroundColor);
        Drawable dd = copy(d);
        dd.setBounds(0, 0, w, h);
        dd.draw(c);
        return b;
    }

    /** Draws an icon centred on a coloured field (used when an app has no TV banner). */
    static Bitmap renderIconOnColor(Drawable icon, int w, int h, int color) {
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.drawColor(color);
        Drawable dd = copy(icon);
        int s = h / 2;
        int left = (w - s) / 2;
        int top = (h - s) / 2;
        dd.setBounds(left, top, left + s, top + s);
        dd.draw(c);
        return b;
    }

    /** Average colour of the visible, non-grey pixels of an icon. */
    static int dominantColor(Drawable icon) {
        try {
            Bitmap b = render(icon, 24, 24, Color.TRANSPARENT);
            long r = 0, g = 0, bl = 0, n = 0;
            long ra = 0, ga = 0, ba = 0, na = 0;
            float[] hsv = new float[3];
            for (int y = 0; y < 24; y++) {
                for (int x = 0; x < 24; x++) {
                    int p = b.getPixel(x, y);
                    if (Color.alpha(p) < 128) continue;
                    ra += Color.red(p); ga += Color.green(p); ba += Color.blue(p); na++;
                    Color.colorToHSV(p, hsv);
                    if (hsv[1] < 0.2f || hsv[2] < 0.15f) continue; // skip greys
                    r += Color.red(p); g += Color.green(p); bl += Color.blue(p); n++;
                }
            }
            b.recycle();
            if (n > 0) return Color.rgb((int) (r / n), (int) (g / n), (int) (bl / n));
            if (na > 0) return Color.rgb((int) (ra / na), (int) (ga / na), (int) (ba / na));
        } catch (RuntimeException ignored) {
        }
        return Color.rgb(60, 60, 70);
    }

    /** Same hue, set to a given saturation floor and brightness. */
    static int tone(int color, float minSat, float value) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.max(hsv[1], minSat);
        hsv[2] = value;
        return Color.HSVToColor(hsv);
    }

    /** Cheap, smooth blur: shrink to a few pixels, then let bilinear scaling smear it. */
    static Bitmap blur(Bitmap src) {
        Bitmap tiny = Bitmap.createScaledBitmap(src, 16, 9, true);
        Bitmap soft = Bitmap.createScaledBitmap(tiny, 96, 54, true);
        tiny.recycle();
        return soft;
    }
}
