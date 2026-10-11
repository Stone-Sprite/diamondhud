package dev.dsh.diamondhud;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Every ore the arrow can be pointed at, grouped the way a player thinks about them.
 *
 * <p>A group is the unit the settings screen toggles: "钻石" covers both
 * {@code diamond_ore} and {@code deepslate_diamond_ore}, because nobody mining diamond
 * wants to tick two boxes for the same drop.
 *
 * <p>The key is the only thing written to the config file, so relabelling an entry is
 * free and reordering the list cannot silently change what a saved config means.
 *
 * <p>{@code hintY} is the middle of that ore's generation band, used only when nothing
 * is in scan range: the arrow then points straight down at the depth worth digging to.
 * Ores that only generate in the Nether leave it null, because "dig down" is meaningless
 * advice there.
 */
final class OreTargets {

    private OreTargets() {
    }

    /** One thing worth digging for. */
    static final class Ore {
        final String key;
        final String label;
        final List<Block> blocks;
        final Integer hintY;

        private Ore(String key, String label, Integer hintY, Block... blocks) {
            this.key = key;
            this.label = label;
            this.hintY = hintY;
            this.blocks = List.of(blocks);
        }
    }

    /** Ordered the way a player runs into them. */
    static final List<Ore> ALL = List.of(
            new Ore("coal", "煤矿", 96, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE),
            new Ore("copper", "铜矿", 48, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE),
            new Ore("iron", "铁矿", 16, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE),
            new Ore("gold", "金矿", -16, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE),
            new Ore("redstone", "红石", -59, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE),
            new Ore("lapis", "青金石", 0, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE),
            new Ore("diamond", "钻石", -59, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE),
            new Ore("emerald", "绿宝石", 200, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE),
            new Ore("quartz", "下界石英", null, Blocks.NETHER_QUARTZ_ORE),
            new Ore("nether_gold", "下界金", null, Blocks.NETHER_GOLD_ORE),
            new Ore("debris", "远古残骸", null, Blocks.ANCIENT_DEBRIS));

    static final String DEFAULT_KEY = "diamond";

    private static final Map<String, Ore> BY_KEY = new LinkedHashMap<>();

    static {
        for (Ore o : ALL) {
            BY_KEY.put(o.key, o);
        }
    }

    static Ore byKey(String key) {
        return BY_KEY.get(key);
    }

    static boolean isKnown(String key) {
        return BY_KEY.containsKey(key);
    }

    static String label(String key) {
        Ore o = BY_KEY.get(key);
        return o == null ? key : o.label;
    }

    /**
     * The block set for a set of keys.
     *
     * <p>Falls back to diamond when nothing resolves: an empty set would make the arrow
     * point at nothing, which reads as a broken mod rather than as an empty selection.
     */
    static Set<Block> blocksOf(Collection<String> keys) {
        Set<Block> out = new HashSet<>();
        for (String k : keys) {
            Ore o = BY_KEY.get(k);
            if (o != null) {
                out.addAll(o.blocks);
            }
        }
        if (out.isEmpty()) {
            out.addAll(BY_KEY.get(DEFAULT_KEY).blocks);
        }
        return out;
    }
}
