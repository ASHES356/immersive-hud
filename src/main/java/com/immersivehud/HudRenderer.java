package com.immersivehud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

/**
 * Per-frame composition: track -> health -> absorption -> hunger -> damage flash.
 *
 * Health and absorption share the ring/bar by ratio. The length itself is always
 * health/maxHealth, so it still shrinks when you are hurt; absorption is painted on top of
 * the health part and never past it.
 *
 * The same numbers can be drawn as a frame around the screen edge (FRAME) or as a straight
 * bar along one edge (BAR).
 */
public final class HudRenderer {

    /** previous frame's health, used to detect a drop */
    private static float lastHealth = -1.0F;
    private static long lastDamageAt = 0L;

    /** previous total experience, used to detect an XP pickup */
    private static int lastTotalExperience = -1;
    private static long lastXpGainAt = 0L;

    private static final long FLASH_MS = 200L;

    private HudRenderer() {
    }

    public static void draw(GuiGraphics g, Minecraft mc, int w, int h) {
        HudConfig.Values v = HudConfig.VALUES;
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        // spectators get no HUD at all, creative keeps the hotbar but loses the metre
        if (player.isSpectator()) {
            return;
        }

        // the replacement hotbar sits underneath the ring
        if (v.hideHotbar.get()) {
            HotbarRenderer.draw(g, mc, w, h);
        }

        if (v.hideInCreative.get() && player.isCreative()) {
            return;
        }

        // ---- values -------------------------------------------------------
        float maxHealth = Math.max(1.0F, player.getMaxHealth());
        float health = Math.max(0.0F, player.getHealth());
        float absorption = Math.max(0.0F, player.getAbsorptionAmount());

        double healthFraction = Math.min(1.0, health / maxHealth);
        double shareHealth = 1.0;
        double shareAbsorption = 0.0;
        if (absorption > 0.0F) {
            shareHealth = health / (health + absorption);
            if (health >= maxHealth) {
                // at full health the metre keeps at least `ringFullHealthMinShare` for
                // health, even when absorption is worth more
                shareHealth = Math.max(v.fullHealthMinShare.get(), shareHealth);
            }
            shareAbsorption = 1.0 - shareHealth;
        }

        double healthEnd = healthFraction * shareHealth;
        double absorptionEnd = healthFraction * (shareHealth + shareAbsorption);
        double hunger = Math.max(0.0, Math.min(1.0, player.getFoodData().getFoodLevel() / 20.0));
        // saturation is plain vanilla data, no AppleSkin required
        double saturation = Math.max(0.0, Math.min(1.0, player.getFoodData().getSaturationLevel() / 20.0));
        double saturationEnd = Math.min(saturation, hunger);
        boolean hungerEffect = player.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER);

        // ---- colours ------------------------------------------------------
        int t = Math.max(1, v.thickness.get());
        int trackColor = RingRenderer.parseColor(v.trackColor.get(), v.trackAlpha.get());
        int absorptionColor = RingRenderer.parseColor(v.absorptionColor.get(), v.absorptionAlpha.get());
        int hungerColor = RingRenderer.parseColor(v.hungerColor.get(), v.hungerAlpha.get());
        if (hungerEffect && v.hungerWarning.get()) {
            // same idea as AppleSkin's warning: the band breathes while Hunger is active
            double phase = (System.currentTimeMillis() % 900L) / 900.0;
            double pulse = 0.55 + 0.45 * (0.5 + 0.5 * Math.cos(phase * Math.PI * 2.0));
            hungerColor = RingRenderer.withAlpha(hungerColor,
                    (int) Math.round(((hungerColor >>> 24) & 0xFF) * pulse));
        }
        int saturationColor = RingRenderer.parseColor(v.saturationColor.get(), v.saturationAlpha.get());

        int healthColor = RingRenderer.parseColor(v.healthColor.get(), v.healthAlpha.get());
        double threshold = v.lowHealthThreshold.get();
        boolean low = threshold > 0.0 && healthFraction < threshold;
        if (low) {
            int lowColor = RingRenderer.parseColor(v.healthLowColor.get(), v.healthAlpha.get());
            healthColor = RingRenderer.lerpArgb(healthColor, lowColor, 1.0 - healthFraction / threshold);
            if (v.lowHealthPulse.get()) {
                // 0.55 .. 1.0 breathing, ~1.2 s period
                double phase = (System.currentTimeMillis() % 1200L) / 1200.0;
                double pulse = 0.55 + 0.45 * (0.5 + 0.5 * Math.cos(phase * Math.PI * 2.0));
                healthColor = RingRenderer.withAlpha(healthColor,
                        (int) Math.round(((healthColor >>> 24) & 0xFF) * pulse));
            }
        }

        // ---- experience: takes over the hunger band for a moment after picking XP up ----
        boolean xpActive = false;
        double xpShown = 0.0;
        int xpDrawColor = 0;
        int xpAlphaNow = 0;
        if (v.xpReplaceHunger.get()) {
            int totalXp = player.totalExperience;
            long nowMs = System.currentTimeMillis();
            if (lastTotalExperience >= 0 && totalXp > lastTotalExperience) {
                lastXpGainAt = nowMs;
            }
            lastTotalExperience = totalXp;
            int duration = v.xpShowDuration.get();
            long since = nowMs - lastXpGainAt;
            if (lastXpGainAt > 0 && since >= 0 && since < duration) {
                xpActive = true;
                xpShown = Math.max(0.0, Math.min(1.0, player.experienceProgress));
                // fade out over the last 600 ms
                double fade = since > duration - 600 ? Math.max(0.0, (duration - since) / 600.0) : 1.0;
                xpAlphaNow = (int) Math.round(v.xpAlpha.get() * fade);
                xpDrawColor = RingRenderer.parseColor(v.xpColor.get(), xpAlphaNow);
            }
        }

        // ---- air: while underwater it takes over the hunger band, like XP does ----
        int airMax = Math.max(1, player.getMaxAirSupply());
        double airFraction = Math.max(0.0, Math.min(1.0, player.getAirSupply() / (double) airMax));
        boolean airActive = !xpActive && v.hideAir.get() && airFraction < 0.999;
        int airDrawColor = airActive
                ? RingRenderer.parseColor(v.airColor.get(), v.airAlpha.get())
                : 0;

        // ---- geometry -----------------------------------------------------
        boolean barMode = v.displayMode.get() == HudConfig.DisplayMode.BAR;
        HudConfig.BarSide side = v.barSide.get();
        double barLength = v.barLength.get();
        int barInset = v.barInset.get();

        boolean hungerOverlay = v.hungerStyle.get() == HudConfig.HungerStyle.OVERLAY;
        int hungerT = Math.max(1, v.hungerInnerThickness.get());
        int hungerInset = barInset + t;

        if (barMode) {
            BarRenderer.draw(g, w, h, side, barLength, barInset, t, absorptionEnd, 1.0, trackColor);
            BarRenderer.draw(g, w, h, side, barLength, barInset, t, 0.0, healthEnd, healthColor);
            if (absorptionEnd > healthEnd) {
                BarRenderer.draw(g, w, h, side, barLength, barInset, t,
                        healthEnd, absorptionEnd, absorptionColor);
            }
            if (hunger > 0.0 && !xpActive && !airActive) {
                if (hungerOverlay) {
                    BarRenderer.draw(g, w, h, side, barLength, barInset, t, 0.0, hunger, hungerColor);
                } else {
                    BarRenderer.draw(g, w, h, side, barLength, hungerInset, hungerT, 0.0, hunger, hungerColor);
                }
            }
        } else {
            int hungerInsetRing = t + v.hungerInnerGap.get();

            RingRenderer.ring(g, w, h, t, 0, absorptionEnd, 1.0, trackColor);
            RingRenderer.ring(g, w, h, t, 0, 0.0, healthEnd, healthColor);
            if (absorptionEnd > healthEnd) {
                RingRenderer.ring(g, w, h, t, 0, healthEnd, absorptionEnd, absorptionColor);
            }
            if (hunger > 0.0 && !xpActive && !airActive) {
                if (hungerOverlay) {
                    RingRenderer.ring(g, w, h, t, 0, 0.0, hunger, hungerColor);
                } else {
                    RingRenderer.ring(g, w, h, hungerT, hungerInsetRing, 0.0, hunger, hungerColor);
                }
            }
        }

        // ---- experience: drawn on the hunger band while it is active ----
        if (xpActive) {
            if (barMode) {
                if (hungerOverlay) {
                    BarRenderer.draw(g, w, h, side, barLength, barInset, t, 0.0, xpShown, xpDrawColor);
                } else {
                    BarRenderer.draw(g, w, h, side, barLength, hungerInset, hungerT,
                            0.0, xpShown, xpDrawColor);
                }
            } else {
                if (hungerOverlay) {
                    RingRenderer.ring(g, w, h, t, 0, 0.0, xpShown, xpDrawColor);
                } else {
                    RingRenderer.ring(g, w, h, hungerT, t + v.hungerInnerGap.get(),
                            0.0, xpShown, xpDrawColor);
                }
            }
        }

        // ---- air: drawn on the hunger band while underwater ----
        if (airActive) {
            if (barMode) {
                if (hungerOverlay) {
                    BarRenderer.draw(g, w, h, side, barLength, barInset, t, 0.0, airFraction, airDrawColor);
                } else {
                    BarRenderer.draw(g, w, h, side, barLength, hungerInset, hungerT,
                            0.0, airFraction, airDrawColor);
                }
            } else {
                if (hungerOverlay) {
                    RingRenderer.ring(g, w, h, t, 0, 0.0, airFraction, airDrawColor);
                } else {
                    RingRenderer.ring(g, w, h, hungerT, t + v.hungerInnerGap.get(),
                            0.0, airFraction, airDrawColor);
                }
            }
        }

        // ---- saturation: painted on top of the hunger band, from the same origin ----
        if (!xpActive && !airActive && v.showSaturation.get() && saturationEnd > 0.0) {
            if (barMode) {
                if (hungerOverlay) {
                    BarRenderer.draw(g, w, h, side, barLength, barInset, t, 0.0, saturationEnd, saturationColor);
                } else {
                    BarRenderer.draw(g, w, h, side, barLength, hungerInset, hungerT,
                            0.0, saturationEnd, saturationColor);
                }
            } else {
                if (hungerOverlay) {
                    RingRenderer.ring(g, w, h, t, 0, 0.0, saturationEnd, saturationColor);
                } else {
                    RingRenderer.ring(g, w, h, hungerT, t + v.hungerInnerGap.get(),
                            0.0, saturationEnd, saturationColor);
                }
            }
        }

        // ---- damage flash --------------------------------------------------
        long now = System.currentTimeMillis();
        if (lastHealth >= 0.0F && health < lastHealth - 0.01F) {
            lastDamageAt = now;
        }
        lastHealth = health;
        if (v.damageFlash.get() && now - lastDamageAt < FLASH_MS) {
            int alpha = (int) Math.round(150.0 * (1.0 - (now - lastDamageAt) / (double) FLASH_MS));
            int white = (alpha << 24) | 0xFFFFFF;
            if (barMode) {
                BarRenderer.draw(g, w, h, side, barLength, barInset, t, 0.0, 1.0, white);
            } else {
                RingRenderer.ring(g, w, h, t, 0, 0.0, 1.0, white);
            }
        }

        // ---- experience level, tucked into a corner so it never touches the metre ----
        HudConfig.XpCorner corner = v.xpLevelCorner.get();
        // idle opacity: 0 hides the number completely until XP is picked up, a small value
        // leaves a faint watermark
        int levelAlpha = xpActive ? xpAlphaNow : v.xpLevelIdleAlpha.get();
        if (corner != HudConfig.XpCorner.NONE && player.experienceLevel > 0 && levelAlpha > 0) {
            String text = Integer.toString(player.experienceLevel);
            int margin = v.xpLevelMargin.get();
            int textW = mc.font.width(text);
            int textH = mc.font.lineHeight;
            int x;
            int y;
            switch (corner) {
                case TOP_LEFT -> {
                    x = margin;
                    y = margin;
                }
                case TOP_RIGHT -> {
                    x = w - margin - textW;
                    y = margin;
                }
                case BOTTOM_LEFT -> {
                    x = margin;
                    y = h - margin - textH;
                }
                default -> {
                    x = w - margin - textW;
                    y = h - margin - textH;
                }
            }
            g.drawString(mc.font, text, x, y,
                    RingRenderer.parseColor(v.xpLevelColor.get(), levelAlpha), true);
        }
    }
}
