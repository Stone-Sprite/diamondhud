package dev.dsh.diamondhud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

/**
 * Diamond HUD, Fabric entrypoint.
 *
 * <p>Ported from the NeoForge build. The only loading-specific piece is the HUD
 * registration: NeoForge registers on {@code RegisterGuiLayersEvent} above
 * {@code VanillaGuiLayers.EXPERIENCE_LEVEL}, and Fabric has no such event, so this
 * appends through {@code HudElementRegistry.addLast} instead. That lands in the same
 * place — drawn after the bars, so the arrow is never overpainted by them.
 *
 * <p>The layer itself is unchanged: {@code GuiLayer.render} and
 * {@code HudElement.render} have the same signature here.
 */
public final class DiamondHudMod implements ClientModInitializer {

    public static final String MODID = "diamondhud";

    @Override
    public void onInitializeClient() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MODID, "diamond_arrow"),
                new DiamondHudLayer());
    }
}