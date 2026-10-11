package dev.dsh.diamondhud;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = DiamondHudMod.MODID, dist = Dist.CLIENT)
public final class DiamondHudMod {

    public static final String MODID = "diamondhud";

    public DiamondHudMod(IEventBus modBus) {
        modBus.addListener(DiamondHudMod::onRegisterLayers);
        modBus.addListener(DiamondHudMod::onRegisterKeys);
        NeoForge.EVENT_BUS.register(DiamondHudMod.class);
    }

    private static void onRegisterLayers(RegisterGuiLayersEvent event) {
        // Above the experience level so the arrow is drawn after the bars and is
        // never overpainted by them.
        event.registerAbove(
                VanillaGuiLayers.EXPERIENCE_LEVEL,
                Identifier.fromNamespaceAndPath(MODID, "diamond_arrow"),
                new DiamondHudLayer());
    }

    /** The bindings live in DiamondHudKeys so both loaders behave identically. */
    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        for (KeyMapping km : DiamondHudKeys.ALL) {
            event.register(km);
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        DiamondHudKeys.tick(Minecraft.getInstance());
    }

    /**
     * Right-click while holding a compass opens the settings.
     *
     * <p>NeoForge can cancel the interaction outright, so unlike the Fabric build the click
     * never reaches the world at all. Both builds end up in the same place; this one just
     * leaves no trace behind.
     */
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        if (!GogglesPanel.holdingCompass(mc.player)) {
            return;
        }
        event.setCanceled(true);
        mc.setScreen(new DiamondHudSettingsScreen());
    }
}
