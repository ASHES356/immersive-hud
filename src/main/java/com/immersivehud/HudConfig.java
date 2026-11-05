package com.immersivehud;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Client config.
 *
 * NOTE: every value lives at the TOP LEVEL on purpose, no sections. Configured 2.6.3
 * merges a save by doing {@code loaded.putAll(changedOnly)}, and nightconfig's putAll
 * replaces a whole sub-table instead of deep-merging it - so a change to one value in
 * {@code [ring]} would wipe every other value in {@code [ring]} back to its default.
 * Flat keys sidestep that entirely.
 */
public final class HudConfig {

    public enum HungerStyle {
        /** hunger and health share one band; hunger is painted in front (semi-transparent) */
        OVERLAY,
        /** a second, thinner ring just inside the health ring */
        INNER
    }

    /** How the health metre is drawn. */
    public enum DisplayMode {
        /** a frame around the whole screen edge */
        FRAME,
        /** a straight bar along one edge */
        BAR
    }

    /** Which edge the bar sits on. */
    public enum BarSide {
        TOP, BOTTOM, LEFT, RIGHT
    }

    /** Where the experience level number is printed. */
    public enum XpCorner {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    public static final ModConfigSpec SPEC;
    public static final Values VALUES;

    static {
        Pair<Values, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Values::new);
        VALUES = pair.getLeft();
        SPEC = pair.getRight();
    }

    private HudConfig() {
    }

    public static final class Values {

        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.BooleanValue hideInCreative;

        // ---- ring ----
        public final ModConfigSpec.EnumValue<DisplayMode> displayMode;
        public final ModConfigSpec.EnumValue<BarSide> barSide;
        public final ModConfigSpec.DoubleValue barLength;
        public final ModConfigSpec.IntValue barInset;
        public final ModConfigSpec.IntValue thickness;
        public final ModConfigSpec.ConfigValue<String> trackColor;
        public final ModConfigSpec.IntValue trackAlpha;
        public final ModConfigSpec.ConfigValue<String> healthColor;
        public final ModConfigSpec.ConfigValue<String> healthLowColor;
        public final ModConfigSpec.IntValue healthAlpha;
        public final ModConfigSpec.ConfigValue<String> absorptionColor;
        public final ModConfigSpec.IntValue absorptionAlpha;
        public final ModConfigSpec.DoubleValue fullHealthMinShare;

        // ---- hunger ----
        public final ModConfigSpec.EnumValue<HungerStyle> hungerStyle;
        public final ModConfigSpec.ConfigValue<String> hungerColor;
        public final ModConfigSpec.IntValue hungerAlpha;
        public final ModConfigSpec.BooleanValue showSaturation;
        public final ModConfigSpec.ConfigValue<String> saturationColor;
        public final ModConfigSpec.IntValue saturationAlpha;
        public final ModConfigSpec.BooleanValue hungerWarning;

        // ---- experience ----
        public final ModConfigSpec.BooleanValue xpReplaceHunger;
        public final ModConfigSpec.IntValue xpShowDuration;
        public final ModConfigSpec.ConfigValue<String> xpColor;
        public final ModConfigSpec.IntValue xpAlpha;
        public final ModConfigSpec.EnumValue<XpCorner> xpLevelCorner;
        public final ModConfigSpec.IntValue xpLevelMargin;
        public final ModConfigSpec.ConfigValue<String> xpLevelColor;
        public final ModConfigSpec.IntValue xpLevelIdleAlpha;

        // ---- air ----
        public final ModConfigSpec.ConfigValue<String> airColor;
        public final ModConfigSpec.IntValue airAlpha;
        public final ModConfigSpec.IntValue hungerInnerThickness;
        public final ModConfigSpec.IntValue hungerInnerGap;

        // ---- what to hide ----
        public final ModConfigSpec.BooleanValue hideHealth;
        public final ModConfigSpec.BooleanValue hideHunger;
        public final ModConfigSpec.BooleanValue hideArmor;
        public final ModConfigSpec.BooleanValue hideXpBar;
        public final ModConfigSpec.BooleanValue showVanillaXpLevel;
        public final ModConfigSpec.BooleanValue hideHotbar;
        public final ModConfigSpec.BooleanValue hideAir;
        public final ModConfigSpec.BooleanValue hideAppleSkin;

        // ---- hotbar ----
        public final ModConfigSpec.BooleanValue hotbarEnabled;
        public final ModConfigSpec.IntValue hotbarAlpha;
        public final ModConfigSpec.DoubleValue hotbarSelectedScale;
        public final ModConfigSpec.IntValue hotbarSelectedLift;
        public final ModConfigSpec.IntValue hotbarYOffset;
        public final ModConfigSpec.DoubleValue hotbarScale;
        public final ModConfigSpec.BooleanValue hotbarOffhand;
        public final ModConfigSpec.BooleanValue hotbarSelectionOutline;

        // ---- feedback ----
        public final ModConfigSpec.BooleanValue lowHealthPulse;
        public final ModConfigSpec.DoubleValue lowHealthThreshold;
        public final ModConfigSpec.BooleanValue damageFlash;

        Values(ModConfigSpec.Builder b) {
            b.comment("Master switch. When false the mod does nothing at all.").translation("immersivehud.enabled");
            enabled = b.define("enabled", true);

            b.comment("Hide the ring and the level number in creative mode. The hotbar stays,",
                            "since you still use it there.")
                    .translation("immersivehud.hideInCreative");
            hideInCreative = b.define("hideInCreative", true);

            // ------------------------------------------------------------ ring
            b.comment("FRAME = a ring around the whole screen edge. BAR = a straight bar along one edge.")
                    .translation("immersivehud.ring.displayMode");
            displayMode = b.defineEnum("displayMode", DisplayMode.BAR);

            b.comment("Which screen edge the bar sits on, BAR mode only.")
                    .translation("immersivehud.ring.barSide");
            barSide = b.defineEnum("barSide", BarSide.BOTTOM);

            b.comment("Length of the bar as a fraction of the screen edge, BAR mode only.")
                    .translation("immersivehud.ring.barLength");
            barLength = b.defineInRange("barLength", 1.0D, 0.1D, 1.0D);

            b.comment("Distance between the bar and the screen edge, BAR mode only.")
                    .translation("immersivehud.ring.barInset");
            barInset = b.defineInRange("barInset", 0, 0, 96);

            b.comment("Thickness of the ring in GUI-scaled pixels.").translation("immersivehud.ring.thickness");
            thickness = b.defineInRange("ringThickness", 2, 1, 16);

            b.comment("Colour of the part of the ring that is not lit (#RRGGBB).").translation("immersivehud.ring.trackColor");
            trackColor = b.define("ringTrackColor", "#0E1A12");

            b.comment("Opacity of that unlit track, 0-255.").translation("immersivehud.ring.trackAlpha");
            trackAlpha = b.defineInRange("ringTrackAlpha", 70, 0, 255);

            b.comment("Health colour while healthy (#RRGGBB).").translation("immersivehud.ring.healthColor");
            healthColor = b.define("ringHealthColor", "#4EE870");

            b.comment("Health colour it fades towards when low (#RRGGBB).").translation("immersivehud.ring.healthLowColor");
            healthLowColor = b.define("ringHealthLowColor", "#E8503C");

            b.comment("Health opacity, 0-255.").translation("immersivehud.ring.healthAlpha");
            healthAlpha = b.defineInRange("ringHealthAlpha", 235, 0, 255);

            b.comment("Absorption (golden hearts) colour (#RRGGBB).").translation("immersivehud.ring.absorptionColor");
            absorptionColor = b.define("ringAbsorptionColor", "#F2CE5C");

            b.comment("Absorption opacity, 0-255.").translation("immersivehud.ring.absorptionAlpha");
            absorptionAlpha = b.defineInRange("ringAbsorptionAlpha", 235, 0, 255);

            b.comment("At full health, the smallest share of the ring health keeps for itself,",
                            "even when absorption is worth more than the health. 0.5 = never less than half.")
                    .translation("immersivehud.ring.fullHealthMinShare");
            fullHealthMinShare = b.defineInRange("ringFullHealthMinShare", 0.5D, 0.0D, 1.0D);

            // ---------------------------------------------------------- hunger
            b.comment("Hunger ring style. OVERLAY = same band as health, painted in front.",
                            "INNER = a second thinner ring just inside the health ring.")
                    .translation("immersivehud.hunger.style");
            hungerStyle = b.defineEnum("hungerStyle", HungerStyle.INNER);

            b.comment("Hunger colour (#RRGGBB).").translation("immersivehud.hunger.color");
            hungerColor = b.define("hungerColor", "#FF963C");

            b.comment("Hunger opacity, 0-255. Only meaningful in OVERLAY mode.").translation("immersivehud.hunger.alpha");
            hungerAlpha = b.defineInRange("hungerAlpha", 110, 0, 255);

            b.comment("Draw saturation on top of the hunger band, the way AppleSkin does.",
                            "Saturation is vanilla data (FoodData#getSaturationLevel), no AppleSkin needed.")
                    .translation("immersivehud.hunger.showSaturation");
            showSaturation = b.define("showSaturation", true);

            b.comment("Saturation colour (#RRGGBB).").translation("immersivehud.hunger.saturationColor");
            saturationColor = b.define("saturationColor", "#FFF200");

            b.comment("Saturation opacity, 0-255. Default matches the hunger opacity: saturation is",
                            "told apart by its brighter colour rather than by being more transparent.")
                    .translation("immersivehud.hunger.saturationAlpha");
            saturationAlpha = b.defineInRange("saturationAlpha", 110, 0, 255);

            b.comment("Pulse the hunger band while the Hunger effect is active (AppleSkin warns the same way).")
                    .translation("immersivehud.hunger.hungerWarning");
            hungerWarning = b.define("hungerWarning", true);

            // -------------------------------------------------------- experience
            b.comment("When experience is picked up, show it in place of hunger for a moment,",
                            "then fade back to hunger.")
                    .translation("immersivehud.xp.replaceHunger");
            xpReplaceHunger = b.define("xpReplaceHunger", true);

            b.comment("How long the experience stays on screen, in milliseconds.")
                    .translation("immersivehud.xp.showDuration");
            xpShowDuration = b.defineInRange("xpShowDuration", 2500, 300, 8000);

            b.comment("Experience colour (#RRGGBB).").translation("immersivehud.xp.color");
            xpColor = b.define("xpColor", "#7CFF4D");

            b.comment("Experience opacity, 0-255.").translation("immersivehud.xp.alpha");
            xpAlpha = b.defineInRange("xpAlpha", 235, 0, 255);

            b.comment("Corner where the experience level number is printed. NONE hides it.")
                    .translation("immersivehud.xp.levelCorner");
            xpLevelCorner = b.defineEnum("xpLevelCorner", XpCorner.BOTTOM_RIGHT);

            b.comment("Distance from that corner, in GUI-scaled pixels. Keep it above the ring thickness.")
                    .translation("immersivehud.xp.levelMargin");
            xpLevelMargin = b.defineInRange("xpLevelMargin", 10, 0, 64);

            b.comment("Experience level colour (#RRGGBB).").translation("immersivehud.xp.levelColor");
            xpLevelColor = b.define("xpLevelColor", "#80FF20");

            b.comment("Opacity of the level number when nothing is happening, 0-255.",
                            "0 = completely hidden until experience is picked up, a low value leaves a faint watermark.")
                    .translation("immersivehud.xp.levelIdleAlpha");
            xpLevelIdleAlpha = b.defineInRange("xpLevelIdleAlpha", 50, 0, 255);

            // ---------------------------------------------------------------- air
            b.comment("Air colour (#RRGGBB). Air takes over the hunger band while you are underwater.")
                    .translation("immersivehud.air.color");
            airColor = b.define("airColor", "#4FC3F7");

            b.comment("Air opacity, 0-255.").translation("immersivehud.air.alpha");
            airAlpha = b.defineInRange("airAlpha", 220, 0, 255);

            b.comment("Thickness of the inner hunger ring, INNER mode only.")
                    .translation("immersivehud.hunger.innerThickness");
            hungerInnerThickness = b.defineInRange("hungerInnerThickness", 2, 1, 8);

            b.comment("Gap between the health ring and the inner hunger ring, INNER mode only.",
                            "0 = the hunger ring sits flush against the health ring.")
                    .translation("immersivehud.hunger.innerGap");
            hungerInnerGap = b.defineInRange("hungerInnerGap", 0, 0, 16);

            // ---------------------------------------------------------- hiding
            b.comment("Hide the vanilla hearts.").translation("immersivehud.hide.health");
            hideHealth = b.define("hideHealth", true);

            b.comment("Hide the vanilla hunger drumsticks.").translation("immersivehud.hide.hunger");
            hideHunger = b.define("hideHunger", true);

            b.comment("Hide the vanilla armour icons.").translation("immersivehud.hide.armor");
            hideArmor = b.define("hideArmor", true);

            b.comment("Hide the vanilla experience bar.").translation("immersivehud.hide.xpBar");
            hideXpBar = b.define("hideXpBar", true);

            // inverted on purpose: the old "hideXpLevel" key defaulted to false and stayed
            // false forever in existing config files, so the vanilla number never went away.
            // A new key gets the new default instead of a stale stored value.
            b.comment("Keep showing the vanilla experience level number above the hotbar.",
                            "Off by default: the corner number replaces it.")
                    .translation("immersivehud.hide.showXpLevel");
            showVanillaXpLevel = b.define("showVanillaXpLevel", false);

            b.comment("Hide the vanilla hotbar (this mod draws its own).").translation("immersivehud.hide.hotbar");
            hideHotbar = b.define("hideHotbar", true);

            b.comment("Hide the vanilla air bubbles; the ring shows air instead while you are underwater.")
                    .translation("immersivehud.hide.air");
            hideAir = b.define("hideAir", true);

            b.comment("Also cancel AppleSkin's HUD layers while the vanilla hunger bar is hidden.",
                            "Its drumsticks and saturation overlay are drawn on the vanilla hunger position,",
                            "so they would be left floating around the new ring.")
                    .translation("immersivehud.hide.appleSkin");
            hideAppleSkin = b.define("hideAppleSkin", true);

            // ---------------------------------------------------------- hotbar
            b.comment("Draw the replacement hotbar at all.").translation("immersivehud.hotbar.enabled");
            hotbarEnabled = b.define("hotbarEnabled", true);

            b.comment("Opacity of the hotbar contents, 0-255.").translation("immersivehud.hotbar.alpha");
            hotbarAlpha = b.defineInRange("hotbarAlpha", 150, 0, 255);

            b.comment("Scale of the selected slot's item.").translation("immersivehud.hotbar.selectedScale");
            hotbarSelectedScale = b.defineInRange("hotbarSelectedScale", 1.5D, 1.0D, 2.0D);

            b.comment("How many pixels the selected slot's item floats up.")
                    .translation("immersivehud.hotbar.selectedLift");
            hotbarSelectedLift = b.defineInRange("hotbarSelectedLift", 2, 0, 8);

            b.comment("Vertical offset of the whole hotbar.").translation("immersivehud.hotbar.yOffset");
            hotbarYOffset = b.defineInRange("hotbarYOffset", -6, -60, 60);

            b.comment("Scale of the whole hotbar, anchored at the bottom centre of the screen.")
                    .translation("immersivehud.hotbar.scale");
            hotbarScale = b.defineInRange("hotbarScale", 0.6D, 0.5D, 2.0D);

            b.comment("Also draw the offhand item next to the hotbar.").translation("immersivehud.hotbar.offhand");
            hotbarOffhand = b.define("hotbarOffhand", true);

            b.comment("Draw a faint outline around the selected slot.")
                    .translation("immersivehud.hotbar.selectionOutline");
            hotbarSelectionOutline = b.define("hotbarSelectionOutline", true);

            // -------------------------------------------------------- feedback
            b.comment("Make the ring breathe when health is low.").translation("immersivehud.fx.lowHealthPulse");
            lowHealthPulse = b.define("fxLowHealthPulse", true);

            b.comment("Health fraction below which the ring turns red and pulses.")
                    .translation("immersivehud.fx.lowHealthThreshold");
            lowHealthThreshold = b.defineInRange("fxLowHealthThreshold", 0.3D, 0.0D, 1.0D);

            b.comment("Flash the ring white when health drops.").translation("immersivehud.fx.damageFlash");
            damageFlash = b.define("fxDamageFlash", true);
        }
    }
}
