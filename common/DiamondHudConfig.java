package dev.dsh.diamondhud;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The settings that have to survive a restart: is the arrow on, how far does it look, and
 * which single ore it points at.
 *
 * <p>One ore, not a set of them. With several selected, "the nearest ore" stops being a
 * well defined thing -- the arrow flips between targets as the player moves, the
 * remembered positions from one ore are used to judge another, and the depth hint has to
 * pick one of several bands to mean. One target keeps every one of those choices obvious.
 *
 * <p>Stored as plain text, one {@code key=value} per line. The whole file is a boolean, an
 * int and one short id, so a JSON parser would buy nothing but a dependency.
 *
 * <p>{@code config/diamondhud.txt} is resolved against the process working directory,
 * which is the game directory for both the vanilla launcher and PCL. Nothing here touches
 * loader APIs, so the same file compiles for Fabric and NeoForge.
 */
final class DiamondHudConfig {

    private static final Logger LOG = LoggerFactory.getLogger("diamondhud");

    /** Where the settings live, relative to the game directory. */
    static final Path FILE = Path.of("config", "diamondhud.txt");

    static final int MIN_RADIUS = 4;
    static final int MAX_RADIUS = 64;

    /**
     * Smoke-test escape hatch: a raw block-id list that overrides the ore choice.
     *
     * <p>Kept because pointing the arrow at {@code minecraft:stone} is the only way to see
     * it draw without hunting for ore, and that must not depend on the settings file
     * having the right contents.
     */
    private static final String TARGET_OVERRIDE = System.getProperty("diamondhud.target");
    private static final String RADIUS_OVERRIDE = System.getProperty("diamondhud.radius");

    private static boolean enabled = true;
    private static int radius = 20;
    private static String ore = OreTargets.DEFAULT_KEY;

    /** Set when the file still carries 0.6.0's multi-ore list, so it gets rewritten once. */
    private static boolean legacyList;

    /**
     * Bumped on every change.
     *
     * <p>The layer keeps a set of remembered ore positions, which is only meaningful for
     * the target it was collected under. A generation counter lets the layer notice a
     * change and drop that memory without either side holding a listener on the other.
     */
    private static int generation;

    private static Set<Block> cachedBlocks;
    private static int cachedGeneration = -1;

    static {
        load();
    }

    private DiamondHudConfig() {
    }

    // ------------------------------------------------------------------ load / save

    private static void load() {
        boolean hadFile = false;
        if (Files.isRegularFile(FILE)) {
            try {
                for (String raw : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eq = line.indexOf('=');
                    if (eq < 0) {
                        continue;
                    }
                    String key = line.substring(0, eq).trim();
                    if ("ores".equalsIgnoreCase(key)) {
                        legacyList = true;
                    }
                    apply(key, line.substring(eq + 1).trim());
                }
                hadFile = true;
            } catch (IOException e) {
                LOG.warn("could not read {}: {}", FILE, e.toString());
            }
        }
        if (!hadFile) {
            // First run. Seed from the old system property so launch scripts and smoke
            // tests written before the settings screen keep meaning the same thing.
            if (RADIUS_OVERRIDE != null) {
                radius = clampRadius(parseInt(RADIUS_OVERRIDE, radius));
            }
            save();
        } else if (legacyList) {
            // 0.6.0 wrote a list of ores. Rewrite the file in the single-ore form once, so
            // it stops carrying a key nothing reads any more.
            save();
        }
        generation++;
    }

    private static void apply(String key, String value) {
        switch (key.toLowerCase(Locale.ROOT)) {
            case "enabled" -> enabled = Boolean.parseBoolean(value);
            case "radius" -> radius = clampRadius(parseInt(value, radius));
            case "ore" -> {
                if (OreTargets.isKnown(value)) {
                    ore = value;
                }
            }
            // 0.6.0 wrote a list; take the first entry it names so an existing file keeps
            // a sensible meaning instead of silently falling back to diamond
            case "ores" -> {
                for (String token : value.split(",")) {
                    String k = token.trim();
                    if (OreTargets.isKnown(k)) {
                        ore = k;
                        break;
                    }
                }
            }
            default -> {
                // unknown keys are kept in the file but ignored, so a newer build writing
                // extra settings does not break an older one
            }
        }
    }

    static void save() {
        StringBuilder sb = new StringBuilder();
        sb.append("# DSH diamondhud - hand-editable; the in-game screen writes this file\n");
        sb.append("# 手持指南针右键或按 J 可以在游戏里改\n");
        sb.append("enabled=").append(enabled).append('\n');
        sb.append("radius=").append(radius).append('\n');
        sb.append("ore=").append(ore).append('\n');
        try {
            Path parent = FILE.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(FILE, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("could not write {}: {}", FILE, e.toString());
        }
    }

    // ------------------------------------------------------------------ reads

    static boolean enabled() {
        return enabled;
    }

    static int radius() {
        return radius;
    }

    /** The one ore being looked for. */
    static String ore() {
        return ore;
    }

    static boolean isOre(String key) {
        return ore.equals(key);
    }

    static int generation() {
        return generation;
    }

    /** The block set the arrow looks for, honouring the smoke-test override. */
    static Set<Block> targets() {
        if (TARGET_OVERRIDE != null) {
            return resolveIds(TARGET_OVERRIDE);
        }
        if (cachedGeneration != generation) {
            cachedBlocks = OreTargets.blocksOf(ore);
            cachedGeneration = generation;
        }
        return cachedBlocks;
    }

    /**
     * The depth worth digging down to, or null when this ore only generates in the Nether
     * and "dig down" would be meaningless advice.
     */
    static Integer hintY() {
        OreTargets.Ore o = OreTargets.byKey(ore);
        return o == null ? null : o.hintY;
    }

    // ------------------------------------------------------------------ writes

    static void toggleEnabled() {
        enabled = !enabled;
        generation++;
        save();
    }

    static void setEnabled(boolean value) {
        if (enabled != value) {
            enabled = value;
            generation++;
            save();
        }
    }

    static void setRadius(int value) {
        int clamped = clampRadius(value);
        if (clamped != radius) {
            radius = clamped;
            generation++;
            save();
        }
    }

    /** Select an ore. Selecting the one already selected is a no-op, not a deselection. */
    static void selectOre(String key) {
        if (!OreTargets.isKnown(key) || ore.equals(key)) {
            return;
        }
        ore = key;
        generation++;
        save();
    }

    static void resetOre() {
        selectOre(OreTargets.DEFAULT_KEY);
    }

    // ------------------------------------------------------------------ helpers

    static int clampRadius(int value) {
        return Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, value));
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** "minecraft:stone, deepslate_iron_ore" -> live Block instances. */
    private static Set<Block> resolveIds(String csv) {
        Set<Block> out = new java.util.HashSet<>();
        for (String token : csv.split(",")) {
            String t = token.trim();
            if (t.isEmpty()) {
                continue;
            }
            Identifier id = t.indexOf(':') < 0
                    ? Identifier.withDefaultNamespace(t)
                    : Identifier.tryParse(t);
            if (id == null) {
                continue;
            }
            Block b = BuiltInRegistries.BLOCK.getValue(id);
            if (b != null) {
                out.add(b);
            }
        }
        if (out.isEmpty()) {
            out.addAll(OreTargets.blocksOf(OreTargets.DEFAULT_KEY));
        }
        return out;
    }
}
