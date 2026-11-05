package com.immersivehud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Paged config screen, registered through NeoForge's IConfigScreenFactory so the Mods list
 * shows a normal "Config" button.
 *
 * Layout is computed from the actual GUI size: rows shrink, then split into two columns,
 * so nothing ever overlaps on a small window. The backdrop is a plain fill instead of the
 * blurred world, which keeps the text crisp.
 */
public class HudConfigScreen extends Screen {

    private static final String[] PAGE_KEYS = {
            "immersivehud.page.display",
            "immersivehud.page.colors",
            "immersivehud.page.hide",
            "immersivehud.page.hotbar",
            "immersivehud.page.misc"
    };

    private static final int MARGIN = 20;
    private static final int TAB_H = 20;
    private static final int ROW_STEP_MAX = 22;
    private static final int ROW_STEP_MIN = 12;

    private final Screen parent;
    private int page;

    private final List<Label> labels = new ArrayList<>();

    public HudConfigScreen(Screen parent) {
        super(Component.translatable("immersivehud.config.title"));
        this.parent = parent;
    }

    private static final class Label {
        final Component text;
        final int x;
        final int y;
        final boolean right;

        Label(Component text, int x, int y, boolean right) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.right = right;
        }
    }

    /** One config row; laid out later so the layout code can adapt to the screen size. */
    private interface Row {
        void build(int x, int y, int w, int h);
    }

    // ------------------------------------------------------------------------ layout

    @Override
    protected void init() {
        this.labels.clear();

        // ---- page tabs
        int margin = Math.max(24, Math.round(this.width * 0.12F));
        int tabCount = PAGE_KEYS.length;
        int tabSpan = Math.min(this.width - margin * 2, 620);
        int tabW = Math.max(40, Math.min(150, (tabSpan - (tabCount - 1) * 4) / tabCount));
        int tabTotal = tabW * tabCount + (tabCount - 1) * 4;
        int tabX = (this.width - tabTotal) / 2;
        for (int i = 0; i < tabCount; i++) {
            final int index = i;
            Button tab = Button.builder(Component.translatable(PAGE_KEYS[i]), b -> {
                if (this.page != index) {
                    this.page = index;
                    this.rebuildWidgets();
                }
            }).bounds(tabX + i * (tabW + 4), 26, tabW, TAB_H).build();
            tab.active = this.page != index;
            addRenderableWidget(tab);
        }

        // ---- rows, laid out to fit whatever space is left
        List<Row> rows = rowsFor(this.page);
        int top = 26 + TAB_H + 8;
        int bottom = this.height - 32;
        int available = Math.max(36, bottom - top);

        int columns = 1;
        int perColumn = Math.max(1, rows.size());
        int step = Math.min(ROW_STEP_MAX, available / perColumn);
        if (step < ROW_STEP_MIN + 2 && this.width >= 340 && rows.size() > 3) {
            columns = 2;
            perColumn = (rows.size() + 1) / 2;
            step = Math.min(ROW_STEP_MAX, available / perColumn);
        }
        step = Math.max(ROW_STEP_MIN, step);

        int rowH = Math.max(9, step - 2);
        int blockW = Math.min(this.width - margin * 2, columns == 2 ? 620 : 340);
        int blockX = (this.width - blockW) / 2;
        int colW = (blockW - (columns - 1) * 10) / columns;
        for (int i = 0; i < rows.size(); i++) {
            int column = columns == 1 ? 0 : i / perColumn;
            int rowIndex = columns == 1 ? i : i % perColumn;
            int x = blockX + column * (colW + 10);
            int y = top + rowIndex * step;
            rows.get(i).build(x, y, colW, rowH);
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(this.width / 2 - 60, this.height - 26, 120, 20)
                .build());
    }

    // -------------------------------------------------------------------- row table

    private List<Row> rowsFor(int page) {
        HudConfig.Values v = HudConfig.VALUES;
        List<Row> rows = new ArrayList<>();
        switch (page) {
            case 0 -> {
                rows.add(enumRow("immersivehud.ring.displayMode", v.displayMode, HudConfig.DisplayMode.values()));
                rows.add(enumRow("immersivehud.ring.barSide", v.barSide, HudConfig.BarSide.values()));
                rows.add(doubleRow("immersivehud.ring.barLength", v.barLength, 0.1D, 1.0D, 2));
                rows.add(intRow("immersivehud.ring.barInset", v.barInset, 0, 96));
                rows.add(intRow("immersivehud.ring.thickness", v.thickness, 1, 16));
                rows.add(enumRow("immersivehud.hunger.style", v.hungerStyle, HudConfig.HungerStyle.values()));
                rows.add(intRow("immersivehud.hunger.innerGap", v.hungerInnerGap, 0, 16));
                rows.add(intRow("immersivehud.hunger.innerThickness", v.hungerInnerThickness, 1, 8));
                rows.add(boolRow("immersivehud.hunger.showSaturation", v.showSaturation));
                rows.add(boolRow("immersivehud.hunger.hungerWarning", v.hungerWarning));
                rows.add(boolRow("immersivehud.xp.replaceHunger", v.xpReplaceHunger));
                rows.add(intRow("immersivehud.xp.showDuration", v.xpShowDuration, 300, 8000));
                rows.add(enumRow("immersivehud.xp.levelCorner", v.xpLevelCorner, HudConfig.XpCorner.values()));
                rows.add(intRow("immersivehud.xp.levelMargin", v.xpLevelMargin, 0, 64));
            }
            case 1 -> {
                rows.add(colorRow("immersivehud.ring.healthColor", v.healthColor));
                rows.add(intRow("immersivehud.ring.healthAlpha", v.healthAlpha, 0, 255));
                rows.add(colorRow("immersivehud.ring.healthLowColor", v.healthLowColor));
                rows.add(colorRow("immersivehud.ring.absorptionColor", v.absorptionColor));
                rows.add(intRow("immersivehud.ring.absorptionAlpha", v.absorptionAlpha, 0, 255));
                rows.add(colorRow("immersivehud.hunger.color", v.hungerColor));
                rows.add(intRow("immersivehud.hunger.alpha", v.hungerAlpha, 0, 255));
                rows.add(colorRow("immersivehud.hunger.saturationColor", v.saturationColor));
                rows.add(intRow("immersivehud.hunger.saturationAlpha", v.saturationAlpha, 0, 255));
                rows.add(colorRow("immersivehud.xp.color", v.xpColor));
                rows.add(intRow("immersivehud.xp.alpha", v.xpAlpha, 0, 255));
                rows.add(colorRow("immersivehud.xp.levelColor", v.xpLevelColor));
                rows.add(intRow("immersivehud.xp.levelIdleAlpha", v.xpLevelIdleAlpha, 0, 255));
                rows.add(colorRow("immersivehud.air.color", v.airColor));
                rows.add(intRow("immersivehud.air.alpha", v.airAlpha, 0, 255));
                rows.add(colorRow("immersivehud.ring.trackColor", v.trackColor));
                rows.add(intRow("immersivehud.ring.trackAlpha", v.trackAlpha, 0, 255));
            }
            case 2 -> {
                rows.add(boolRow("immersivehud.hide.health", v.hideHealth));
                rows.add(boolRow("immersivehud.hide.hunger", v.hideHunger));
                rows.add(boolRow("immersivehud.hide.armor", v.hideArmor));
                rows.add(boolRow("immersivehud.hide.xpBar", v.hideXpBar));
                rows.add(boolRow("immersivehud.hide.showXpLevel", v.showVanillaXpLevel));
                rows.add(boolRow("immersivehud.hide.hotbar", v.hideHotbar));
                rows.add(boolRow("immersivehud.hide.appleSkin", v.hideAppleSkin));
                rows.add(boolRow("immersivehud.hide.air", v.hideAir));
            }
            case 3 -> {
                rows.add(boolRow("immersivehud.hotbar.enabled", v.hotbarEnabled));
                rows.add(intRow("immersivehud.hotbar.alpha", v.hotbarAlpha, 0, 255));
                rows.add(doubleRow("immersivehud.hotbar.scale", v.hotbarScale, 0.5D, 2.0D, 2));
                rows.add(doubleRow("immersivehud.hotbar.selectedScale", v.hotbarSelectedScale, 1.0D, 2.0D, 2));
                rows.add(intRow("immersivehud.hotbar.selectedLift", v.hotbarSelectedLift, 0, 8));
                rows.add(intRow("immersivehud.hotbar.yOffset", v.hotbarYOffset, -60, 60));
                rows.add(boolRow("immersivehud.hotbar.offhand", v.hotbarOffhand));
                rows.add(boolRow("immersivehud.hotbar.selectionOutline", v.hotbarSelectionOutline));
            }
            default -> {
                rows.add(boolRow("immersivehud.enabled", v.enabled));
                rows.add(boolRow("immersivehud.hideInCreative", v.hideInCreative));
                rows.add(boolRow("immersivehud.fx.lowHealthPulse", v.lowHealthPulse));
                rows.add(doubleRow("immersivehud.fx.lowHealthThreshold", v.lowHealthThreshold, 0.0D, 1.0D, 2));
                rows.add(boolRow("immersivehud.fx.damageFlash", v.damageFlash));
                rows.add(doubleRow("immersivehud.ring.fullHealthMinShare", v.fullHealthMinShare, 0.0D, 1.0D, 2));
            }
        }
        return rows;
    }

    // ---------------------------------------------------------------- row factories

    private Row boolRow(String nameKey, ModConfigSpec.BooleanValue cfg) {
        return (x, y, w, h) -> addRenderableWidget(CycleButton.onOffBuilder(cfg.get())
                .create(x, y, w, h, Component.translatable(nameKey), (button, value) -> cfg.set(value)));
    }

    private <E extends Enum<E>> Row enumRow(String nameKey, ModConfigSpec.EnumValue<E> cfg, E[] values) {
        return (x, y, w, h) -> addRenderableWidget(CycleButton.<E>builder(e -> Component.literal(humanize(e.name())))
                .withValues(values)
                .withInitialValue(cfg.get())
                .create(x, y, w, h, Component.translatable(nameKey), (button, value) -> cfg.set(value)));
    }

    private Row intRow(String nameKey, ModConfigSpec.IntValue cfg, int min, int max) {
        return (x, y, w, h) -> addRenderableWidget(new IntSlider(nameKey, cfg, min, max, x, y, w, h));
    }

    private Row doubleRow(String nameKey, ModConfigSpec.DoubleValue cfg, double min, double max, int decimals) {
        return (x, y, w, h) -> addRenderableWidget(
                new DoubleSlider(nameKey, cfg, min, max, decimals, x, y, w, h));
    }

    private Row colorRow(String nameKey, ModConfigSpec.ConfigValue<String> cfg) {
        return (x, y, w, h) -> {
            int boxW = Math.min(110, Math.max(64, w / 2));
            this.labels.add(new Label(Component.translatable(nameKey), x, y + (h - 8) / 2, false));
            EditBox box = new EditBox(this.font, x + w - boxW, y, boxW, h, Component.translatable(nameKey));
            box.setMaxLength(9);
            box.setValue(cfg.get());
            box.setResponder(text -> {
                String value = text.trim();
                if (value.matches("#?[0-9a-fA-F]{6}")) {
                    cfg.set(value.startsWith("#") ? value : "#" + value);
                }
            });
            addRenderableWidget(box);
        };
    }

    // -------------------------------------------------------------------- rendering

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // fully opaque backdrop: the blurred world must not bleed through, which is what
        // makes a screen look soft even when its own pixels are sharp
        graphics.fill(0, 0, this.width, this.height, 0xFF0E1014);

        // widgets first, text last: a widget drawn afterwards would cover the labels and
        // make them look washed out
        super.render(graphics, mouseX, mouseY, partialTick);

        // push the text well above the widget layer
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        for (Label label : this.labels) {
            // shadow on purpose: without it the labels look washed out next to the vanilla
            // widgets, whose messages always carry a shadow
            graphics.drawString(this.font, label.text, label.x, label.y, 0xFFFFFF, true);
        }
        graphics.pose().popPose();
    }

    @Override
    public void onClose() {
        try {
            ModConfigSpec spec = HudConfig.SPEC;
            if (spec != null && spec.isLoaded()) {
                spec.save();
            }
        } catch (Exception ignored) {
            // never let saving take the game down
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private static String humanize(String enumName) {
        String lower = enumName.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return upperFirst(lower);
    }

    private static String upperFirst(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // --------------------------------------------------------------------- sliders

    private static final class IntSlider extends AbstractSliderButton {

        private final String nameKey;
        private final ModConfigSpec.IntValue cfg;
        private final int min;
        private final int max;

        IntSlider(String nameKey, ModConfigSpec.IntValue cfg, int min, int max, int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty(), fraction(cfg.get(), min, max));
            this.nameKey = nameKey;
            this.cfg = cfg;
            this.min = min;
            this.max = max;
            updateMessage();
        }

        static double fraction(double value, double min, double max) {
            if (max <= min) {
                return 0.0;
            }
            return Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
        }

        private int current() {
            return (int) Math.round(this.min + (this.max - this.min) * this.value);
        }

        @Override
        protected void updateMessage() {
            if (this.cfg == null) {
                return;
            }
            setMessage(Component.translatable(this.nameKey).append(": " + current()));
        }

        @Override
        protected void applyValue() {
            this.cfg.set(current());
        }
    }

    private static final class DoubleSlider extends AbstractSliderButton {

        private final String nameKey;
        private final ModConfigSpec.DoubleValue cfg;
        private final double min;
        private final double max;
        private final int decimals;

        DoubleSlider(String nameKey, ModConfigSpec.DoubleValue cfg, double min, double max, int decimals,
                     int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty(), IntSlider.fraction(cfg.get(), min, max));
            this.nameKey = nameKey;
            this.cfg = cfg;
            this.min = min;
            this.max = max;
            this.decimals = decimals;
            updateMessage();
        }

        private double current() {
            double raw = this.min + (this.max - this.min) * this.value;
            double factor = Math.pow(10.0, this.decimals);
            return Math.round(raw * factor) / factor;
        }

        @Override
        protected void updateMessage() {
            if (this.cfg == null) {
                return;
            }
            setMessage(Component.translatable(this.nameKey)
                    .append(": " + String.format(java.util.Locale.ROOT, "%." + this.decimals + "f", current())));
        }

        @Override
        protected void applyValue() {
            this.cfg.set(current());
        }
    }
}
