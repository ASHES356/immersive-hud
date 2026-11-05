package com.immersivehud;

import net.minecraft.client.gui.GuiGraphics;

/**
 * BAR display mode: the same numbers, drawn as a straight bar along one screen edge
 * instead of a frame around the whole screen.
 *
 * Horizontal bars fill left to right, vertical bars fill bottom to top, and the bar is
 * always centred on its edge. {@code from}/{@code to} are fractions of the bar length and
 * are rounded to whole pixels, so the health / absorption seam can never overlap.
 */
public final class BarRenderer {

    private BarRenderer() {
    }

    public static void draw(GuiGraphics g, int w, int h, HudConfig.BarSide side, double lengthFraction,
                            int inset, int thickness, double from, double to, int argb) {
        if (thickness <= 0 || argb == 0) {
            return;
        }
        int in = Math.max(0, inset);
        if (in * 2 >= Math.min(w, h)) {
            return;
        }
        double f0 = clamp01(from);
        double f1 = clamp01(to);
        if (f1 - f0 < 1e-6) {
            return;
        }
        double len = Math.max(0.02, Math.min(1.0, lengthFraction));

        switch (side) {
            case TOP, BOTTOM -> {
                int barW = (int) Math.round((w - 2 * in) * len);
                if (barW < 2) {
                    return;
                }
                int x0 = (w - barW) / 2;
                int y = side == HudConfig.BarSide.TOP ? in : h - in - thickness;
                rect(g, x0 + (int) Math.round(barW * f0), y,
                        x0 + (int) Math.round(barW * f1), y + thickness, argb);
            }
            case LEFT, RIGHT -> {
                int barH = (int) Math.round((h - 2 * in) * len);
                if (barH < 2) {
                    return;
                }
                int y0 = (h - barH) / 2;
                int x = side == HudConfig.BarSide.LEFT ? in : w - in - thickness;
                int bottom = y0 + barH;
                rect(g, x, bottom - (int) Math.round(barH * f1),
                        x + thickness, bottom - (int) Math.round(barH * f0), argb);
            }
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
}
