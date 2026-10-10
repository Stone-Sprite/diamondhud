package dev.dsh.diamondhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * A compass read-out panel styled after Create's Engineer's Goggles overlay.
 *
 * <p>Shown while either hand holds {@code minecraft:compass}. The check is by item
 * identity only — enchantments, custom names and NBT are ignored — so any compass
 * counts.
 *
 * <p>The palette is Create's own, read out of {@code foundation/gui/Theme.class}:
 * the panel fill is {@code #F0100010}, the glowing edge {@code #DD8AB6D6}, the
 * divider {@code #FF9ABBD3}, text {@code #EEFFFFFF} / {@code #FFEEEEEE},
 * secondary text {@code #FFA3A3A3}, accents {@code #FF8AB6D6}, and the status
 * colours {@code #CC20CC20} green and {@code #CCCC2020} red. The layout is a
 * reconstruction: Create draws that overlay entirely in code and ships no texture
 * for it, so the geometry here is modelled on its look rather than copied.
 */
final class GogglesPanel {

    private GogglesPanel() {
    }

    // Create's Theme palette, verbatim.
    private static final int PANEL = 0xF0100010;
    private static final int EDGE = 0xDD8AB6D6;
    private static final int DIVIDER = 0xFF9ABBD3;
    private static final int TITLE = 0xEEFFFFFF;
    private static final int BODY = 0xFFEEEEEE;
    private static final int DIM = 0xFFA3A3A3;
    private static final int ACCENT = 0xFF8AB6D6;
    private static final int GREEN = 0xCC20CC20;
    private static final int RED = 0xCCCC2020;

    private static final int WIDTH = 216;
    private static final int ROW = 11;

    /**
     * GuiGraphics.drawString() does not read a colour int the same way fill() does:
     * the same value came out yellow-green through drawString() and cyan through
     * fill(), which is an R/B swap. Everything textual is pushed through here so
     * the two paths agree.
     */
    private static int text(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    /** True when either hand holds a compass, matched on item ID alone. */
    static boolean holdingCompass(LocalPlayer p) {
        return p.getMainHandItem().is(net.minecraft.world.item.Items.COMPASS)
                || p.getOffhandItem().is(net.minecraft.world.item.Items.COMPASS);
    }

    /**
     * Draw the panel centred above the hotbar.
     *
     * @param target the nearest diamond ore, or null when none is in range.
     */
    static void render(GuiGraphics g, Minecraft mc, LocalPlayer p, BlockPos target) {
        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();

        int rows = 7;
        int height = 16 + rows * ROW + 6;
        int x = guiW / 2 - WIDTH / 2;
        int y = guiH - 48 - height;

        // panel body and glowing edge, one pixel at a time (fill has no rounding)
        g.fill(x, y, x + WIDTH, y + height, PANEL);
        g.fill(x - 1, y - 1, x + WIDTH + 1, y, EDGE);
        g.fill(x - 1, y + height, x + WIDTH + 1, y + height + 1, EDGE);
        g.fill(x - 1, y, x, y + height, EDGE);
        g.fill(x + WIDTH, y, x + WIDTH + 1, y + height, EDGE);

        int pad = 8;
        int lx = x + pad;
        int rx = x + 96;
        int ty = y + 5;

        g.drawString(mc.font, "指南针", lx, ty, text(TITLE), false);
        g.drawString(mc.font, "Compass", x + WIDTH - pad - mc.font.width("Compass"), ty,
                text(ACCENT), false);
        int lineY = ty + 10;
        g.fill(x + 4, lineY, x + WIDTH - 4, lineY + 1, DIVIDER);

        int row = 0;
        int cy = lineY + 4;

        // ---- navigation ----
        row(g, mc, lx, rx, cy + row++ * ROW, "坐标", String.format("%.0f %.0f %.0f",
                p.getX(), p.getY(), p.getZ()), BODY);
        row(g, mc, lx, rx, cy + row++ * ROW, "朝向", facing(p.getYRot()), BODY);
        row(g, mc, lx, rx, cy + row++ * ROW, "群系", biome(mc, p), ACCENT);

        // ---- time ----
        long dayTime = mc.level == null ? 0L : mc.level.getDayTime() % 24000L;
        String clock = String.format("%02d:%02d", ((dayTime / 1000L) + 6L) % 24L,
                (dayTime % 1000L) * 60L / 1000L);
        boolean isDay = dayTime < 13000L;
        row(g, mc, lx, rx, cy + row++ * ROW, "时间", clock + (isDay ? "  白天" : "  夜晚"),
                isDay ? BODY : ACCENT);
        long until = isDay ? (13000L - dayTime) : (24000L - dayTime);
        row(g, mc, lx, rx, cy + row++ * ROW, isDay ? "距天黑" : "距天亮",
                (until / 1000L) + " 分 " + ((until % 1000L) * 60L / 1000L) + " 秒", DIM);

        // ---- diamond, with the arrow folded into the panel ----
        int arrowCx = x + 20;
        int arrowCy = cy + (row + 1) * ROW - 2;
        if (target == null) {
            row(g, mc, lx + 22, rx - 24, cy + row * ROW, "钻石", "范围内无", DIM);
            Arrows.drawCreate(g, arrowCx, arrowCy, 180.0, 1);
        } else {
            double dx = (target.getX() + 0.5) - p.getX();
            double dy = (target.getY() + 0.5) - (p.getY() + p.getEyeHeight());
            double dz = (target.getZ() + 0.5) - p.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double deg = Math.toDegrees(Math.atan2(dx, -dz))
                    - DiamondHudLayer.screenYaw(p);
            // The ore's world coordinates rather than a bare distance: a distance says
            // how far, coordinates say where, and they are still useful after a detour.
            String coords = String.format("%d %d %d", target.getX(), target.getY(), target.getZ());
            row(g, mc, lx + 22, rx - 24, cy + row * ROW, "钻石", coords, GREEN);
            Arrows.drawCreate(g, arrowCx, arrowCy, deg, 1);
        }

        // Words for the last row: which way to turn and whether to look up or down.
        // The arrow alone cannot convey "the target is behind you".
        int hintY = cy + (row + 1) * ROW;
        g.drawString(mc.font, "操作", lx, hintY, text(DIM), false);
        if (target == null) {
            g.drawString(mc.font, "无目标", rx, hintY, text(DIM), false);
        } else {
            double hx = (target.getX() + 0.5) - p.getX();
            double hy = (target.getY() + 0.5) - (p.getY() + p.getEyeHeight());
            double hz = (target.getZ() + 0.5) - p.getZ();
            g.drawString(mc.font, Arrows.aimHint(hx, hy, hz, p.getYRot(), p.getXRot()),
                    rx, hintY, text(TITLE), false);
        }
    }

    private static void row(GuiGraphics g, Minecraft mc, int lx, int rx, int y,
                            String label, String value, int color) {
        g.drawString(mc.font, label, lx, y, text(DIM), false);
        g.drawString(mc.font, value, rx, y, text(color), false);
    }

    private static String facing(float yaw) {
        String[] dirs = {"正南", "西南", "正西", "西北", "正北", "东北", "正东", "东南"};
        int i = (int) Math.round((((yaw % 360.0f) + 360.0f) % 360.0f) / 45.0f) % 8;
        return dirs[i] + String.format("  %.0f°", ((yaw % 360.0f) + 360.0f) % 360.0f);
    }

    private static String biome(Minecraft mc, LocalPlayer p) {
        if (mc.level == null) {
            return "-";
        }
        return mc.level.getBiome(p.blockPosition()).unwrapKey()
                .map(k -> k.identifier().getPath())
                .orElse("-");
    }
}
