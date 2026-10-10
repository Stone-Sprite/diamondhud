package dev.dsh.diamondhud;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@Mod(value = DiamondHudMod.MODID, dist = Dist.CLIENT)
public final class DiamondHudMod {

    public static final String MODID = "diamondhud";

    public DiamondHudMod(IEventBus modBus) {
        modBus.addListener(DiamondHudMod::onRegisterLayers);
    }

    private static void onRegisterLayers(RegisterGuiLayersEvent event) {
        // Above the experience level so the arrow is drawn after the bars and is
        // never overpainted by them.
        event.registerAbove(
                VanillaGuiLayers.EXPERIENCE_LEVEL,
                Identifier.fromNamespaceAndPath(MODID, "diamond_arrow"),
                new DiamondHudLayer());
    }
}
