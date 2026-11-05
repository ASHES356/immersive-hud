package com.immersivehud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * The vanilla hotbar is cancelled and redrawn here: no background texture, icons faded,
 * the selected slot floats up and grows a little.
 *
 * Fading items is not officially supported by GuiGraphics - the shader colour is the only
 * lever available without an offscreen render target. Some item render types ignore the
 * alpha channel; if the icons turn out fully opaque in game, this pass simply looks normal
 * and nothing breaks.
 */
public final class HotbarRenderer {

    private static final int SLOT = 20;
    private static final int HOTBAR_WIDTH = 182;
    private static final int HOTBAR_HEIGHT = 22;

    private HotbarRenderer() {
    }

    public static void draw(GuiGraphics g, Minecraft mc, int w, int h) {
        HudConfig.Values v = HudConfig.VALUES;
        if (!v.hotbarEnabled.get() || mc.player == null) {
            return;
        }

        float alpha = v.hotbarAlpha.get() / 255.0F;
        int baseX = (w - HOTBAR_WIDTH) / 2;
        int baseY = h - HOTBAR_HEIGHT + v.hotbarYOffset.get();
        var inventory = mc.player.getInventory();

        // whole-hotbar scale, anchored at the bottom centre of the screen
        float hotbarScale = v.hotbarScale.get().floatValue();
        g.pose().pushPose();
        if (hotbarScale != 1.0F) {
            g.pose().translate(w / 2.0F, h, 0.0F);
            g.pose().scale(hotbarScale, hotbarScale, 1.0F);
            g.pose().translate(-w / 2.0F, -h, 0.0F);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        try {
            if (v.hotbarOffhand.get()) {
                ItemStack offhand = inventory.offhand.get(0);
                if (!offhand.isEmpty()) {
                    int ox = baseX - 29 + 3;
                    int oy = baseY + 3;
                    g.renderItem(offhand, ox, oy);
                    g.renderItemDecorations(mc.font, offhand, ox, oy);
                }
            }

            double scale = v.hotbarSelectedScale.get();
            int lift = v.hotbarSelectedLift.get();

            for (int i = 0; i < 9; i++) {
                ItemStack stack = inventory.items.get(i);
                if (stack.isEmpty()) {
                    continue;
                }
                int x = baseX + 3 + i * SLOT;
                int y = baseY + 3;
                boolean selected = i == inventory.selected;

                if (selected && (scale > 1.0 || lift > 0)) {
                    int cx = x + 8;
                    int cy = y + 8;
                    g.pose().pushPose();
                    g.pose().translate(0.0F, (float) -lift, 0.0F);
                    g.pose().translate(cx, cy, 0.0F);
                    g.pose().scale((float) scale, (float) scale, 1.0F);
                    g.pose().translate(-cx, -cy, 0.0F);
                    g.renderItem(stack, x, y);
                    g.renderItemDecorations(mc.font, stack, x, y);
                    g.pose().popPose();
                } else {
                    g.renderItem(stack, x, y);
                    g.renderItemDecorations(mc.font, stack, x, y);
                }
            }
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        if (v.hotbarSelectionOutline.get()) {
            // an empty selected slot keeps its outline at rest: floating it up would look
            // like there is something there
            ItemStack selectedStack = inventory.items.get(inventory.selected);
            int lift = selectedStack.isEmpty() ? 0 : v.hotbarSelectedLift.get();
            int x = baseX + 3 + inventory.selected * SLOT;
            int y = baseY + 3 - lift;
            int line = (int) Math.round(150.0 * alpha) << 24 | 0xFFFFFF;
            g.fill(x - 2, y - 2, x + 18, y - 1, line);
            g.fill(x - 2, y + 16, x + 18, y + 17, line);
            g.fill(x - 2, y - 1, x - 1, y + 16, line);
            g.fill(x + 17, y - 1, x + 18, y + 16, line);
        }

        g.pose().popPose();
    }
}
