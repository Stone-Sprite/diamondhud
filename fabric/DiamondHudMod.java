package dev.dsh.diamondhud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Diamond HUD, Fabric entrypoint.
 *
 * <p>All this class does is register things. The bindings themselves and the logic behind
 * them live in {@link DiamondHudKeys}, which is loader-independent, so the Fabric and
 * NeoForge builds cannot drift apart in behaviour.
 *
 * <p>The compass right-click is polled on the client tick with edge detection rather than
 * hooked through a mixin. NeoForge can cancel the interaction event; Fabric cannot, so the
 * click also reaches the world -- harmless with a compass in hand, since a compass has no
 * use action of its own.
 */
public final class DiamondHudMod implements ClientModInitializer {

    public static final String MODID = "diamondhud";

    /** Set while the use key is held, so one press opens the screen exactly once. */
    private static boolean useWasDown;

    @Override
    public void onInitializeClient() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MODID, "diamond_arrow"),
                new DiamondHudLayer());
        for (KeyMapping km : DiamondHudKeys.ALL) {
            KeyBindingHelper.registerKeyBinding(km);
        }
        ClientTickEvents.END_CLIENT_TICK.register(DiamondHudMod::onEndTick);
    }

    private static void onEndTick(Minecraft mc) {
        DiamondHudKeys.tick(mc);

        if (mc.player == null || mc.screen != null) {
            useWasDown = false;
            return;
        }
        boolean down = mc.options.keyUse.isDown();
        if (down && !useWasDown && GogglesPanel.holdingCompass(mc.player)) {
            mc.setScreen(new DiamondHudSettingsScreen());
        }
        useWasDown = down;
    }
}
