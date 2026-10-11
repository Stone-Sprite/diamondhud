package dev.dsh.diamondhud;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The settings that have to survive a restart: is the arrow on, how far does it look,
 * and which ores does it care about.
 *
 * <p>Stored as plain text, one {@code key=value} per line. The whole file is a boolean,
 * an int and a list of short ids, so pulling in a JSON parser would buy nothing but a
 * dependency.
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
     * Smoke-test escape hatch: a raw block-id list that overrides the ore selection.
     *
     * <p>Kept because pointing the arrow at {@code minecraft:stone} is the only way to
     * see it draw without hunting for ore, and that must not depend on the settings file
     * having the right contents.
     */
    private static final String TARGET_OVERRIDE = System.getProperty("diamondhud.target");
    private static final String RADIUS_OVERRIDE = System.getProperty("diamondhud.radius");

    private static boolean enabled = true;
    private static int radius = 20;
    private static final Set<String> ores = new LinkedHashSet<>();

    /**
     * Bumped on every change.
     *
     * <p>The layer keeps a set of remembered ore positions, which is only meaningful for
     * the targets it was collected under. A generation counter lets the layer notice a
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
                    apply(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
                hadFile = true;
            } catch (IOException e) {
                LOG.warn("could not read {}: {}", FILE, e.toString());
            }
        }
        if (!hadFile) {
            // First run. Seed from the old system properties so launch scripts and smoke
            // tests written before the settings screen keep meaning the same thing.
            if (RADIUS_OVERRIDE != null) {
                radius = clampRadius(parseInt(RADIUS_OVERRIDE, radius));
            }
            ores.add(OreTargets.DEFAULT_KEY);
            save();
        }
        if (ores.isEmpty()) {
            ores.add(OreTargets.DEFAULT_KEY);
        }
        generation++;
    }

    private static void apply(String key, String value) {
        switch (key.toLowerCase(Locale.ROOT)) {
            case "enabled" -> enabled = Boolean.parseBoolean(value);
            case "radius" -> radius = clampRadius(parseInt(value, radius));
            case "ores" -> {
                ores.clear();
                for (String token : value.split(",")) {
                    String k = token.trim();
                    if (OreTargets.isKnown(k)) {
                        ores.add(k);
                    }
                }
            }
            default -> {
                // unknown keys are kept in the file but ignored, so a newer build
                // writing extra settings does not break an older one
            }
        }
    }

    static void save() {
        StringBuilder sb = new StringBuilder();
        sb.append("# DSH diamondhud - hand-editable; the in-game screen writes this file\n");
        sb.append("# 手持指南针右键可以在游戏里改\n");
        sb.append("enabled=").append(enabled).append('\n');
        sb.append("radius=").append(radius).append('\n');
        sb.append("ores=").append(String.join(",", ores)).append('\n');
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

    static Set<String> ores() {
        return Set.copyOf(ores);
    }

    static boolean oreSelected(String key) {
        return ores.contains(key);
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
            cachedBlocks = OreTargets.blocksOf(ores);
            cachedGeneration = generation;
        }
        return cachedBlocks;
    }

    /**
     * Lowest "dig down to here" depth among the selected ores, or null when none of them
     * generate in the overworld.
     */
    static Integer hintY() {
        Integer lowest = null;
        for (String key : ores) {
            OreTargets.Ore o = OreTargets.byKey(key);
            if (o != null && o.hintY != null && (lowest == null || o.hintY < lowest)) {
                lowest = o.hintY;
            }
        }
        return lowest;
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

    static void toggleOre(String key) {
        if (!OreTargets.isKnown(key)) {
            return;
        }
        if (!ores.remove(key)) {
            ores.add(key);
        }
        if (ores.isEmpty()) {
            // an empty selection means "point at nothing", which is indistinguishable
            // from a broken mod; keep the one you just unticked instead
            ores.add(key);
        }
        generation++;
        save();
    }

    static void selectAllOres() {
        ores.clear();
        for (OreTargets.Ore o : OreTargets.ALL) {
            ores.add(o.key);
        }
        generation++;
        save();
    }

    static void selectOnlyDefault() {
        ores.clear();
        ores.add(OreTargets.DEFAULT_KEY);
        generation++;
        save();
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
            out.addAll(OreTargets.blocksOf(List.of(OreTargets.DEFAULT_KEY)));
        }
        return out;
    }
}
