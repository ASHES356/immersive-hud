package com.immersivehud;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Geometry for the screen-edge ring.
 *
 * The ring is drawn as ONE frame made of four rectangles that never overlap:
 *
 *   +----------------------------------+   top strip    (corners belong here)
 *   |  +----------------------------+  |
 *   |  |   left/right strips only   |  |   sides exclude the corners
 *   |  +----------------------------+  |
 *   +----------------------------------+   bottom strip (corners belong here)
 *
 * Only the sides are shortened, so no pixel is ever painted twice. That matters because
 * the colours are semi-transparent: painting the same pixel twice shows up as a brighter
 * patch in every corner.
 *
 * The health/hunger value is laid out along a single 1-D path starting at the middle of
 * the bottom strip, running to both corners, climbing both side strips together and then
 * running along the top strip from both corners towards the middle, where it closes.
 * Every segment boundary is rounded to whole pixels once, so consecutive segments share
 * exactly one boundary and cannot leave a seam or overlap by a pixel.
 */
public final class RingRenderer {

    private RingRenderer() {
    }

    /**
     * Draws the slice of the ring between {@code from} and {@code to} (both 0..1 of the path).
     *
     * @param thickness strip thickness in GUI-scaled pixels
     * @param inset     distance from the screen edge (0 = flush against the edge)
     * @param argb      colour, alpha included
     */
    public static void ring(GuiGraphics g, int w, int h, int thickness, int inset,
                            double from, double to, int argb) {
        if (thickness <= 0 || argb == 0) {
            return;
        }
        int limit = Math.max(1, Math.min(w, h) / 2 - inset);
        int t = Math.min(thickness, limit);

        int x0 = inset;
        int x1 = w - inset;
        int y0 = inset;
        int y1 = h - inset;

        int innerW = x1 - x0;
        if ((innerW & 1) == 1) {
            innerW--;          // keep the two halves exactly the same width
            x1--;
        }
        int sideLen = (y1 - y0) - 2 * t;
        if (innerW < 2 || sideLen < 0) {
            return;
        }

        int halfW = innerW / 2;
        int bottomLen = halfW;
        int topLen = halfW;
        int halfTotal = bottomLen + sideLen + topLen;
        if (halfTotal <= 0) {
            return;
        }

        int u0 = (int) Math.round(clamp01(from) * halfTotal);
        int u1 = (int) Math.round(clamp01(to) * halfTotal);
        if (u1 <= u0) {
            return;
        }
        int cx = x0 + halfW;

        // ---- bottom strip: from the centre outwards to both corners
        int a = Math.max(u0, 0);
        int b = Math.min(u1, bottomLen);
        if (b > a) {
            rect(g, cx - b, y1 - t, cx - a, y1, argb);
            rect(g, cx + a, y1 - t, cx + b, y1, argb);
        }

        // ---- side strips: climbing both sides together (corners excluded)
        a = Math.max(u0, bottomLen);
        b = Math.min(u1, bottomLen + sideLen);
        if (b > a) {
            int da = a - bottomLen;
            int db = b - bottomLen;
            int top = y1 - t - db;
            int bottom = y1 - t - da;
            rect(g, x0, top, x0 + t, bottom, argb);
            rect(g, x1 - t, top, x1, bottom, argb);
        }

        // ---- top strip: from both corners towards the centre, closing in the middle
        a = Math.max(u0, bottomLen + sideLen);
        b = Math.min(u1, halfTotal);
        if (b > a) {
            int da = a - (bottomLen + sideLen);
            int db = b - (bottomLen + sideLen);
            rect(g, x0 + da, y0, x0 + db, y0 + t, argb);
            rect(g, x1 - db, y0, x1 - da, y0 + t, argb);
        }
    }

    private static void rect(GuiGraphics g, int x0, int y0, int x1, int y1, int argb) {
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        g.fill(x0, y0, x1, y1, argb);
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
    }

    // ------------------------------------------------------------------ colours

    /** Parses "#RRGGBB" (the leading # is optional) and applies {@code alpha}. */
    public static int parseColor(String hex, int alpha) {
        int rgb = parseRgb(hex);
        return (clamp255(alpha) << 24) | rgb;
    }

    public static int parseRgb(String hex) {
        if (hex == null) {
            return 0xFFFFFF;
        }
        String s = hex.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() == 8) {
            s = s.substring(2); // tolerate #AARRGGBB, alpha comes from the config value
        }
        if (s.length() != 6) {
            return 0xFFFFFF;
        }
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

    public static int withAlpha(int argb, int alpha) {
        return (clamp255(alpha) << 24) | (argb & 0xFFFFFF);
    }

    public static int lerpArgb(int from, int to, double t) {
        double k = t < 0 ? 0 : (t > 1 ? 1 : t);
        int a = (int) Math.round(((from >>> 24) & 255) * (1 - k) + ((to >>> 24) & 255) * k);
        int r = (int) Math.round(((from >> 16) & 255) * (1 - k) + ((to >> 16) & 255) * k);
        int gg = (int) Math.round(((from >> 8) & 255) * (1 - k) + ((to >> 8) & 255) * k);
        int b = (int) Math.round((from & 255) * (1 - k) + (to & 255) * k);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    private static int clamp255(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
