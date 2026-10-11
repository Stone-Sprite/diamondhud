package dev.dsh.diamondhud;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Every key diamondhud binds, plus the edge detection that turns a press into a change.
 *
 * <p>Lives here rather than in the entrypoint because only *registering* a KeyMapping is
 * loader-specific: the objects, their defaults and the tick logic are the same on both,
 * and keeping one copy is what stops the two builds from drifting apart.
 *
 * <p>Edge detection on {@code isDown()} rather than {@code consumeClick()}: clickCount is
 * a counter, so if anything calls {@code KeyMapping.click()} twice for one physical press
 * a consume loop flips a toggle off and straight back on with no way to tell that from two
 * real presses. isDown() is a state, so one press is one edge.
 */
final class DiamondHudKeys {

    /** On/off for the arrow. */
    static final KeyMapping TOGGLE = new KeyMapping(
            "key.diamondhud.toggle",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            KeyMapping.Category.MISC);

    /** Opens the settings screen; right-clicking with a compass does the same thing. */
    static final KeyMapping SETTINGS = new KeyMapping(
            "key.diamondhud.settings",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_J,
            KeyMapping.Category.MISC);

    /**
     * One per ore group, deliberately UNBOUND by default.
     *
     * <p>Eleven more default keys would collide with something in any real modpack, and
     * the toggle plus the settings screen already cover the common cases. Leaving them
     * unbound means the user opts in per ore from the vanilla Controls screen.
     */
    private static final Map<String, KeyMapping> ORE = new LinkedHashMap<>();
    /** Everything the loader has to register, in a stable order. */
    static final List<KeyMapping> ALL;

    static {
        int unbound = InputConstants.UNKNOWN.getValue();
        for (OreTargets.Ore ore : OreTargets.ALL) {
            ORE.put(ore.key, new KeyMapping("key.diamondhud.ore." + ore.key,
                    InputConstants.Type.KEYSYM, unbound, KeyMapping.Category.MISC));
        }
        List<KeyMapping> all = new ArrayList<>();
        all.add(TOGGLE);
        all.add(SETTINGS);
        all.addAll(ORE.values());
        ALL = List.copyOf(all);
    }

    /** Last observed down-state per binding, for the edge detection. */
    private static final Map<KeyMapping, Boolean> WAS_DOWN = new LinkedHashMap<>();

    private DiamondHudKeys() {
    }

    /** Call once per client tick. */
    static void tick(Minecraft mc) {
        for (KeyMapping km : ALL) {
            boolean down = km.isDown();
            boolean was = WAS_DOWN.getOrDefault(km, false);
            WAS_DOWN.put(km, down);
            if (down && !was) {
                pressed(mc, km);
            }
        }
    }

    private static void pressed(Minecraft mc, KeyMapping km) {
        if (km == TOGGLE) {
            DiamondHudConfig.toggleEnabled();
            say(mc, "指示箭头已" + (DiamondHudConfig.enabled() ? "开启" : "关闭"));
            return;
        }
        if (km == SETTINGS) {
            if (mc.screen == null) {
                mc.setScreen(new DiamondHudSettingsScreen());
            }
            return;
        }
        for (Map.Entry<String, KeyMapping> e : ORE.entrySet()) {
            if (e.getValue() == km) {
                String ore = e.getKey();
                boolean wasSelected = DiamondHudConfig.isOre(ore);
                DiamondHudConfig.selectOre(ore);
                say(mc, wasSelected
                        ? OreTargets.label(ore) + " 已经是当前目标"
                        : "改为找：" + OreTargets.label(ore));
                return;
            }
        }
    }

    private static void say(Minecraft mc, String text) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§b[钻石HUD] §f" + text), true);
        }
    }

    /** What a binding currently sits on, for the settings screens to show. */
    static String keyName(KeyMapping km) {
        if (km == null || km.isUnbound()) {
            return "未设置";
        }
        return km.getTranslatedKeyMessage().getString();
    }

    static KeyMapping oreKey(String oreKey) {
        return ORE.get(oreKey);
    }
}
