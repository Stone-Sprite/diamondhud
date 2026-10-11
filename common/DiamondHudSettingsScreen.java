package dev.dsh.diamondhud;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The diamondhud settings screen, opened by right-clicking with a compass or by the
 * settings keybind.
 *
 * <p>Three things are configurable, which is everything the arrow actually uses: whether
 * it draws at all, how far it looks, and which ores it looks for. The ore list itself
 * lives one level down in {@link OreSelectScreen} -- eleven toggles on this panel made it
 * tall enough to collide with its own footer at a small GUI scale.
 *
 * <p>Every change is written to disk immediately, so there is no OK/Cancel to get wrong.
 */
final class DiamondHudSettingsScreen extends Screen {

    // Create's Theme palette, the same values GogglesPanel uses.
    private static final int PANEL = 0xF0100010;
    private static final int EDGE = 0xDD8AB6D6;
    private static final int DIVIDER = 0xFF9ABBD3;
    private static final int TITLE = 0xEEFFFFFF;
    private static final int BODY = 0xFFEEEEEE;
    private static final int DIM = 0xFFA3A3A3;
    private static final int ACCENT = 0xFF8AB6D6;
    private static final int GREEN = 0xCC20CC20;
    private static final int RED = 0xCCCC2020;

    private static final int PAD = 8;
    private static final int BTN_H = 14;
    private static final int ROW_STEP = BTN_H + 3;
    private static final int LINE_H = 10;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int contentX;
    private int contentW;
    private int dividerY;
    private int radiusY;
    private int summaryY;
    private List<String> summary = List.of();

    DiamondHudSettingsScreen() {
        super(Component.literal("Diamond HUD"));
    }

    /** GuiGraphics.drawString() reads a colour int the opposite way round from fill(). */
    private static int text(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 16, 310);
        panelH = Math.min(height - 16, 24 + ROW_STEP * 3 + 4 + BTN_H + 6 + LINE_H * 3 + PAD);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        contentX = panelX + PAD;
        contentW = panelW - PAD * 2;

        int y = panelY + 24;
        dividerY = y - 6;

        addRenderableWidget(Button.builder(enabledLabel(), b -> {
            DiamondHudConfig.toggleEnabled();
            b.setMessage(enabledLabel());
        }).bounds(contentX, y, contentW, BTN_H).build());
        y += ROW_STEP;

        radiusY = y;
        addRenderableWidget(Button.builder(Component.literal("半径 -"), b ->
                DiamondHudConfig.setRadius(DiamondHudConfig.radius() - 4))
                .bounds(contentX, y, 56, BTN_H).build());
        addRenderableWidget(Button.builder(Component.literal("半径 +"), b ->
                DiamondHudConfig.setRadius(DiamondHudConfig.radius() + 4))
                .bounds(contentX + contentW - 56, y, 56, BTN_H).build());
        y += ROW_STEP;

        addRenderableWidget(Button.builder(oresLabel(), b ->
                minecraft.setScreen(new OreSelectScreen(this)))
                .bounds(contentX, y, contentW, BTN_H).build());
        y += ROW_STEP + 4;

        addRenderableWidget(Button.builder(Component.literal("关闭"), b -> onClose())
                .bounds(contentX + contentW / 4, y, contentW / 2, BTN_H).build());
        y += ROW_STEP + 4;

        summaryY = y;
        summary = wrap(selectedOresText(), contentW);
    }

    private static Component enabledLabel() {
        return Component.literal("指示箭头：" + (DiamondHudConfig.enabled() ? "开" : "关"));
    }

    private static Component oresLabel() {
        return Component.literal("选择矿石（当前：" + OreTargets.label(DiamondHudConfig.ore()) + "）");
    }

    private static String selectedOresText() {
        OreTargets.Ore o = OreTargets.byKey(DiamondHudConfig.ore());
        if (o == null) {
            return "找：" + DiamondHudConfig.ore();
        }
        return "找：" + o.label
                + (o.hintY == null ? "" : "（扫不到时朝下指 Y=" + o.hintY + "）");
    }

    /** Greedy wrap on the separator the ore names already use. */
    private List<String> wrap(String s, int maxWidth) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String part : s.split("(?<=、)")) {
            String candidate = line + part;
            if (line.length() > 0 && font.width(candidate) > maxWidth) {
                out.add(line.toString());
                line.setLength(0);
            }
            line.append(part);
        }
        if (line.length() > 0) {
            out.add(line.toString());
        }
        return out;
    }

    /**
     * The panel is drawn from renderBackground, not render.
     *
     * <p>Screen.render() already calls renderBackground(), and that call performs a
     * one-per-frame blur -- drawing the panel from render() as well threw "Can only blur
     * once per frame" in the dshmusic screen. Hooking here gets exactly one call and puts
     * the panel underneath the widgets.
     */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.renderBackground(g, mouseX, mouseY, partial);

        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL);
        g.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY, EDGE);
        g.fill(panelX - 1, panelY + panelH, panelX + panelW + 1, panelY + panelH + 1, EDGE);
        g.fill(panelX - 1, panelY, panelX, panelY + panelH, EDGE);
        g.fill(panelX + panelW, panelY, panelX + panelW + 1, panelY + panelH, EDGE);

        String title = "◆ Diamond HUD";
        g.drawString(font, title, contentX, panelY + 6, text(TITLE), false);
        String keys = "开关 " + DiamondHudKeys.keyName(DiamondHudKeys.TOGGLE)
                + " · 设置 " + DiamondHudKeys.keyName(DiamondHudKeys.SETTINGS)
                + " · 也可右键指南针";
        g.drawString(font, keys, contentX + font.width(title) + 8, panelY + 6, text(DIM), false);
        g.fill(panelX + 4, dividerY, panelX + panelW - 4, dividerY + 1, DIVIDER);

        // the radius value sits between the two buttons
        String r = "搜索半径 " + DiamondHudConfig.radius() + " 格";
        g.drawString(font, r, contentX + contentW / 2 - font.width(r) / 2, radiusY + 3,
                text(BODY), false);

        for (int i = 0; i < summary.size() && i < 3; i++) {
            g.drawString(font, summary.get(i), contentX, summaryY + i * LINE_H,
                    text(DiamondHudConfig.enabled() ? GREEN : DIM), false);
        }
        if (!DiamondHudConfig.enabled()) {
            String off = "箭头已关闭";
            g.drawString(font, off, panelX + panelW - PAD - font.width(off),
                    summaryY, text(RED), false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
