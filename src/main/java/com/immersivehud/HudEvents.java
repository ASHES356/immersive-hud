package com.immersivehud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** All vanilla-HUD hiding and the single draw hook. */
public final class HudEvents {

    private HudEvents() {
    }

    @SubscribeEvent
    public static void onLayerPre(RenderGuiLayerEvent.Pre event) {
        HudConfig.Values v = HudConfig.VALUES;
        if (!v.enabled.get()) {
            return;
        }
        ResourceLocation name = event.getName();
        boolean hide =
                (v.hideHealth.get() && name.equals(VanillaGuiLayers.PLAYER_HEALTH))
             || (v.hideHunger.get() && name.equals(VanillaGuiLayers.FOOD_LEVEL))
             || (v.hideArmor.get() && name.equals(VanillaGuiLayers.ARMOR_LEVEL))
             || (v.hideXpBar.get() && name.equals(VanillaGuiLayers.EXPERIENCE_BAR))
             || (!v.showVanillaXpLevel.get() && name.equals(VanillaGuiLayers.EXPERIENCE_LEVEL))
             || (v.hideHotbar.get() && name.equals(VanillaGuiLayers.HOTBAR))
             || (v.hideAir.get() && name.equals(VanillaGuiLayers.AIR_LEVEL))
             // AppleSkin draws its drumsticks / saturation overlay onto the vanilla hunger
             // position; cancel its layers by namespace so nothing is left floating there.
             // No dependency: the namespace simply never matches when it is not installed.
             || (v.hideAppleSkin.get() && v.hideHunger.get() && "appleskin".equals(name.getNamespace()));
        if (hide) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (ImmersiveHud.OPEN_CONFIG.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new HudConfigScreen(null));
            }
        }
    }

    @SubscribeEvent
    public static void onGuiPost(RenderGuiEvent.Post event) {
        HudConfig.Values v = HudConfig.VALUES;
        if (!v.enabled.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        // this hook runs after screens are drawn, so anything painted here would sit on top
        // of an open menu (including our own config screen)
        if (mc.screen != null) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        HudRenderer.draw(graphics, mc, w, h);
    }
}
