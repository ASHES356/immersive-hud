package com.immersivehud;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Immersive HUD - a screen-edge health / hunger ring.
 *
 * Pure client mod. The only integration point it needs from any other mod is
 * nothing at all: the config is a plain NeoForge ModConfigSpec and the config screen is
 * registered through NeoForge's own IConfigScreenFactory, so no config library is required.
 *
 * AppleSkin is handled defensively too: its HUD layers are cancelled by namespace, which
 * needs no dependency and simply does nothing when AppleSkin is absent.
 */
@Mod(value = ImmersiveHud.MOD_ID, dist = Dist.CLIENT)
public final class ImmersiveHud {

    public static final String MOD_ID = "immersivehud";

    /** Unbound by default - the player assigns it in Options -> Controls. */
    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.immersivehud.config",
            InputConstants.UNKNOWN.getValue(),
            "key.categories.immersivehud");

    public ImmersiveHud(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, HudConfig.SPEC);

        // own paged config screen, so the Mods list has a real "Config" button even when
        // no config screen mod is installed
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) (modContainer, parent) -> new HudConfigScreen(parent));

        modBus.addListener(ImmersiveHud::onRegisterKeys);
        NeoForge.EVENT_BUS.register(HudEvents.class);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG);
    }
}
