package dev.dsh.diamondhud;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Draws an arrow in the gap between the health bar and the hunger bar, pointing
 * at the nearest diamond ore, with the distance above it.
 *
 * <p>Headings come from rotating one "up" sprite, so the eight compass directions
 * and the diagonals are all the same art. When the target is essentially straight
 * up or down the arrow locks to that axis and animates in a loop instead, which is
 * what makes "dig down here" read at a glance.
 */
final class DiamondHudLayer implements HudElement {

    private static final Logger LOG = LoggerFactory.getLogger("diamondhud");

    /**
     * Search box half-extent, in blocks, around the player.
     *
     * <p>Override with {@code -Ddiamondhud.radius=N}. Note that a cube this size
     * around a player standing at the surface does not reach the deep layers where
     * diamond actually generates, so the arrow is meant to be read while caving or
     * mining rather than while walking around above ground.
     */
    private static final int RADIUS = Integer.getInteger("diamondhud.radius", 20);

    /**
     * Which blocks to point at. Override with {@code -Ddiamondhud.target=a,b,c} to
     * aim the arrow at something else, which is also how the render is smoke tested
     * without hunting for real diamond.
     */
    private static final Set<Block> TARGETS = resolveTargets(
            System.getProperty("diamondhud.target",
                    "minecraft:diamond_ore,minecraft:deepslate_diamond_ore"));

    /**
     * Client ticks between world rescans. Kept short so the chosen target keeps up
     * with the player; the scan is skipped for any cell that cannot beat the best
     * distance already found, which is what keeps this affordable.
     */

    /** Vertical difference past which "up"/"down" matters at all. */
    private static final int VERTICAL_BIAS = 4;

    /** Horizontal distance under which the target counts as straight up or down. */
    private static final double VERTICAL_HORIZ = 3.0;

    /** Middle of the band diamond ore generates in, used for the empty-scan hint. */
    private static final double DIAMOND_Y = -59.0;

    /** Point at the diamond layer when nothing is in range instead of drawing nothing. */
    private static final boolean HINT_WHEN_EMPTY =
            !"false".equals(System.getProperty("diamondhud.hint", "true"));

    /**
     * Screen-relative or world-absolute heading.
     *
     * <p>Relative (the default) puts screen-up at whatever the player is looking at,
     * so an ore in front always draws as an arrow pointing up. Absolute keeps
     * screen-up pinned to north, which is what a compass does but reads backwards
     * from the player's point of view.
     */
    private static final boolean RELATIVE =
            !"absolute".equalsIgnoreCase(System.getProperty("diamondhud.arrow", "relative"));

    /**
     * {@code -Ddiamondhud.debug=true}: log what the arrow decided, once a second.
     *
     * <p>"The arrow points the wrong way" cannot be settled from a screenshot — the
     * frame on screen is the product of four numbers that are all invisible in it
     * (the target offset, the absolute bearing, the player's view bearing, and the
     * resulting angle). This prints all four plus the frame index it selected, so a
     * wrong heading can be located rather than guessed at.
     */
    private static final boolean DEBUG = Boolean.getBoolean("diamondhud.debug");

    /** Wall clock of the last debug line, so the log stays readable at 60 fps. */
    private long lastDebugAt;

    /**
     * The player's facing expressed in the same clockwise-from-north convention the
     * sprite uses: facing north is 0, east 90, south 180, west 270.
     */
    static double screenYaw(LocalPlayer p) {
        return ((p.getYRot() + 180.0) % 360.0 + 360.0) % 360.0;
    }

    /**
     * Screen pixels per sprite pixel, in GUI units.
     *
     * <p>Deliberately 1: the vanilla status icons are 9 GUI px tall, so an 11 px
     * sprite at 1:1 sits at the same visual weight as the hearts and the hunger
     * drumsticks. Because this is expressed in GUI units, Minecraft's own GUI scale
     * setting scales the whole widget along with the rest of the HUD — a hard-coded
     * 2 made it twice the size of the bars at every scale.
     */
    private static final int PIXEL = Math.max(1, Integer.getInteger("diamondhud.pixel", 1));
    private BlockPos target;
    private double targetDist;
    private String lastReported;

    static {
        // slf4j rather than System.out: stdout is not captured into latest.log here.
        LOG.info("init radius={} targets={} {}", RADIUS, TARGETS.size(),
                TARGETS.stream().map(b -> String.valueOf(BuiltInRegistries.BLOCK.getKey(b))).toList());
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        // Discovery feeds the memory; navigation reads it. Splitting the two is what
        // lets the arrow keep pointing at ore long after it left the scan radius,
        // and lets the scan itself stay cheap.
        scanSlice(mc, p);
        verifyKnown(mc, p);
        target = nearestKnown(p);
        // Holding a compass replaces the bare arrow with the goggles-style read-out;
        // the arrow is folded into that panel rather than drawn twice.
        if (GogglesPanel.holdingCompass(p)) {
            GogglesPanel.render(g, mc, p, target);
            return;
        }
        if (target == null && !HINT_WHEN_EMPTY) {
            return;
        }

        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();

        // The health and hunger icons sit on the row at (guiScaledHeight - 39) and the
        // experience level number is centred on that same row, so the arrow is lifted
        // just clear of it while staying horizontally between the two bars.
        int cx = guiW / 2;
        int cy = guiH - 47;

        double dx;
        double dy;
        double dz;
        double dist;
        boolean hint = false;
        if (target != null) {
            dx = (target.getX() + 0.5) - p.getX();
            dy = (target.getY() + 0.5) - (p.getY() + p.getEyeHeight());
            dz = (target.getZ() + 0.5) - p.getZ();
            dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        } else {
            // Nothing in range. Drawing nothing reads as "broken", and above ground no
            // cube around the player can ever reach the deep layers, so point straight
            // down at the layer diamond generates in and show the depth to it.
            double below = p.getY() - DIAMOND_Y;
            if (below < 2.0) {
                return;   // already down there; an empty screen is honest here
            }
            dx = 0.0;
            dz = 0.0;
            // Keep one sign convention everywhere: dy is target minus eye, so a
            // target below is NEGATIVE. Feeding a positive depth here made the aim
            // hint say "look up" for something straight down.
            dy = -below;
            dist = below;
            hint = true;
        }
        double horiz = Math.sqrt(dx * dx + dz * dz);

        // One second loop; wave drives the travel and the fade of the trailing ghost.
        double phase = (System.currentTimeMillis() % 1000L) / 1000.0;
        double wave = Math.sin(phase * Math.PI * 2.0);
        int bob = (int) Math.round(wave * 3.0);
        int alpha = 150 + (int) Math.round(105 * (0.5 + 0.5 * wave));

        boolean straightUpDown = horiz < VERTICAL_HORIZ && Math.abs(dy) > VERTICAL_BIAS;

        // The bearing is absolute; what the sprite shows is that minus where the player
        // is looking, because the sheet's frame 0 is "ahead", not "north".
        double absolute = Math.toDegrees(Math.atan2(dx, -dz));
        double view = screenYaw(p);
        double drawn;

        if (straightUpDown) {
            // Lock to the vertical axis and drift along it so it reads as a repeating
            // "this way" nudge rather than a static icon. The sprite points up, so a
            // target below has to be drawn rotated a half turn. Exactly one arrow is
            // drawn: an earlier build also painted a trailing ghost, which just looked
            // like a second, broken arrow.
            // dy is target minus eye, so a positive dy means the target is ABOVE.
            boolean up = dy > 0;
            drawn = up ? 0.0 : 180.0;
            int offset = up ? -bob : bob;
            Arrows.drawCreate(g, cx, cy + offset, drawn, PIXEL);
        } else {
            // atan2(dx, -dz) is the clockwise angle from north: 0 = up on screen,
            // 90 = right, 180 = down. That is an ABSOLUTE heading, and with it the
            // arrow says "the ore is north of you" — which reads as pointing behind
            // you whenever the ore is in front but you happen to face south.
            drawn = RELATIVE ? absolute - view : absolute;
            Arrows.drawCreate(g, cx, cy, drawn, PIXEL);
        }
        if (DEBUG) {
            debugLog(p, target, dx, dy, dz, dist, absolute, view, drawn, straightUpDown, hint, cx, cy);
        }

        // Distance, centred above the arrow.
        String label = String.valueOf(Math.round(dist));
        int textW = Digits.width(label) * PIXEL;
        int textH = Digits.height() * PIXEL;
        Digits.draw(g, label, cx - textW / 2, cy - 6 - textH, PIXEL);

        // Words underneath: the arrow only shows where the target is, so when it is
        // behind the player the hint is what actually says which way to turn.
        String hintText = Arrows.aimHint(dx, dy, dz, p.getYRot(), p.getXRot());
        g.drawString(mc.font, hintText, cx - mc.font.width(hintText) / 2, cy + 12,
                Arrows.text(0xEEFFFFFF), true);
    }

    /**
     * Cells examined per client tick.
     *
     * <p>A full cube looks cheap written as a triple loop but it is 41^3 = 68,921
     * block lookups. Doing that in one tick stutters the frame, which is exactly
     * what an earlier build did when it shortened the rescan interval — the fix is
     * not to scan less often but to scan a slice per tick, so no single frame ever
     * pays for the whole volume. 4000/tick finishes a full pass in about 17 ticks,
     * under a second, for a tiny cost each frame.
     */
    /**
     * Discovery budget per client tick. Scanning exists only to <em>find</em> ore;
     * navigation afterwards runs off {@link #known}, so this can stay small and no
     * frame ever pays for the whole volume. A full 41^3 pass takes a couple of
     * seconds at this rate, which is fine because a block only has to be seen once.
     */
    private static final int CELLS_PER_TICK = 1200;

    /** Cap on remembered ore so the set cannot grow without bound. */
    private static final int MAX_KNOWN = 96;

    /** Re-check remembered ore only this close; farther ones cannot be verified cheaply. */
    private static final double VERIFY_RANGE = 40.0;

    /** Remembered ore positions, keyed by packed coordinate. */
    private final java.util.Map<Long, BlockPos> known = new java.util.LinkedHashMap<>();

    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private int scanOx = -RADIUS;
    private int scanOy = -RADIUS;
    private int scanOz = -RADIUS;
    private int verifyCursor;

    private static long key(BlockPos p) {
        return ((long) (p.getX() & 0x3FFFFFF) << 38)
                | ((long) (p.getZ() & 0x3FFFFFF) << 12)
                | (p.getY() & 0xFFF);
    }

    /**
     * Advance discovery by one slice. Every hit is remembered immediately rather
     * than kept as a candidate, so a block found at any point stays navigable even
     * after it leaves the scan radius.
     */
    private void scanSlice(Minecraft mc, LocalPlayer p) {
        BlockPos base = p.blockPosition();
        int remaining = CELLS_PER_TICK;
        while (remaining-- > 0) {
            cursor.set(base.getX() + scanOx, base.getY() + scanOy, base.getZ() + scanOz);
            if (isTarget(mc.level.getBlockState(cursor))) {
                BlockPos found = cursor.immutable();
                if (known.size() >= MAX_KNOWN) {
                    // drop the farthest remembered ore to make room
                    Long farthest = null;
                    double farSq = -1.0;
                    for (var e : known.entrySet()) {
                        double d = e.getValue().distSqr(base);
                        if (d > farSq) {
                            farSq = d;
                            farthest = e.getKey();
                        }
                    }
                    if (farthest != null) {
                        known.remove(farthest);
                    }
                }
                known.putIfAbsent(key(found), found);
            }
            if (++scanOz > RADIUS) {
                scanOz = -RADIUS;
                if (++scanOy > RADIUS) {
                    scanOy = -RADIUS;
                    if (++scanOx > RADIUS) {
                        scanOx = -RADIUS;
                        scanOy = -RADIUS;
                        scanOz = -RADIUS;
                        LOG.info("discovery pass done, remembered={}", known.size());
                        return;
                    }
                }
            }
        }
    }

    /**
     * Drop remembered ore that no longer exists, a few per tick.
     *
     * <p>Without this a mined vein would keep being navigated to forever. Only
     * positions close enough to be certain about are checked, and only a handful
     * per tick, so the cost stays negligible.
     */
    private void verifyKnown(Minecraft mc, LocalPlayer p) {
        int n = known.size();
        if (n == 0) {
            return;
        }
        var it = known.entrySet().iterator();
        int skip = verifyCursor % n;
        for (int i = 0; i < skip && it.hasNext(); i++) {
            it.next();
        }
        int checked = 0;
        while (it.hasNext() && checked < 4) {
            var e = it.next();
            checked++;
            verifyCursor++;
            BlockPos pos = e.getValue();
            if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                    > VERIFY_RANGE * VERIFY_RANGE) {
                continue;   // too far to trust a chunk that may not be loaded
            }
            if (!isTarget(mc.level.getBlockState(pos))) {
                it.remove();
                LOG.info("forgot {} (no longer there)", pos.toShortString());
            }
        }
        verifyCursor %= Math.max(1, known.size());
    }

    /** Nearest remembered ore, or null when nothing is known yet. */
    private BlockPos nearestKnown(LocalPlayer p) {
        BlockPos best = null;
        double bestSq = Double.MAX_VALUE;
        for (BlockPos pos : known.values()) {
            double d = p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (d < bestSq) {
                bestSq = d;
                best = pos;
            }
        }
        if (best != null) {
            targetDist = Math.sqrt(bestSq);
        }
        return best;
    }

    /**
     * One line a second describing the arrow's decision. See {@link #DEBUG}.
     *
     * <p>{@code drawn} is the angle handed to the sprite, so {@code frame} is what
     * the blit actually picked: frame 0 is up, 4 right, 8 down, 12 left.
     */
    private void debugLog(LocalPlayer p, BlockPos tgt, double dx, double dy, double dz,
                          double dist, double absolute, double view, double drawn,
                          boolean locked, boolean hint, int cx, int cy) {
        long now = System.currentTimeMillis();
        if (now - lastDebugAt < 1000L) {
            return;
        }
        lastDebugAt = now;
        LOG.info("arrow tgt={} off=({},{},{}) dist={} at=({}, {}, {}) yaw={} pitch={} "
                        + "absolute={} view={} relative={} drawn={} frame={} locked={} hint={} screen=({},{})",
                tgt == null ? "none" : tgt.toShortString(),
                fmt(dx), fmt(dy), fmt(dz), fmt(dist),
                fmt(p.getX()), fmt(p.getY()), fmt(p.getZ()), fmt(p.getYRot()), fmt(p.getXRot()),
                fmt(absolute), fmt(view), fmt(absolute - view), fmt(drawn),
                Arrows.frameFor(drawn), locked, hint, cx, cy);
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    private static boolean isTarget(BlockState st) {
        return TARGETS.contains(st.getBlock());
    }

    /** Turn "minecraft:x,minecraft:y" into a set of live block instances. */
    private static Set<Block> resolveTargets(String csv) {
        Set<Block> out = new HashSet<>();
        for (String token : csv.split(",")) {
            Identifier id = Identifier.tryParse(token.trim());
            if (id == null) {
                continue;
            }
            Block block = BuiltInRegistries.BLOCK.getValue(id);
            if (block != null) {
                out.add(block);
            }
        }
        if (out.isEmpty()) {
            out.add(Blocks.DIAMOND_ORE);
        }
        return out;
    }
}
