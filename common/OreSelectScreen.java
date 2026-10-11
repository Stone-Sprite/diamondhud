package dev.dsh.diamondhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Second-level screen: pick the one ore to look for.
 *
 * <p>Split out of the main settings screen on purpose. Eleven buttons on a grid made the
 * primary panel tall enough that at a small GUI scale the rows ran into the footer and the
 * whole thing had to be re-derived from the panel rectangle every time an ore was added.
 * A plain list has one row height to get right and stays correct however many ores the
 * game grows.
 *
 * <p>Rows are hit-tested by hand rather than built as widgets: a widget per row would have
 * to be repositioned on every scroll, and this is the one place where a stale widget
 * position shows up as a click landing on the wrong ore.
 */
final class OreSelectScreen extends Screen {

    private static final int PANEL = 0xF0100010;
    private static final int EDGE = 0xDD8AB6D6;
    private static final int DIVIDER = 0xFF9ABBD3;
    private static final int TITLE = 0xEEFFFFFF;
    private static final int BODY = 0xFFEEEEEE;
    private static final int DIM = 0xFFA3A3A3;
    private static final int ACCENT = 0xFF8AB6D6;
    private static final int GREEN = 0xCC20CC20;
    private static final int ROW_HOVER = 0x505000FF;
    private static final int ROW_H = 12;

    private final Screen parent;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int labelY;
    private int footerY;
    private int scroll;

    OreSelectScreen(Screen parent) {
        super(Component.literal("选择矿石"));
        this.parent = parent;
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
        panelW = Math.min(width - 16, 300);
        // The panel is sized to fit every ore without scrolling when the screen allows it,
        // and the chrome around the list is subtracted rather than guessed: the first cut
        // reserved too little and the "已选中 N" line printed straight through the last row.
        int chrome = 60;
        panelH = Math.min(height - 16, OreTargets.ALL.size() * ROW_H + chrome);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        listX = panelX + 8;
        listW = panelW - 16;
        listY = panelY + 24;
        footerY = panelY + panelH - 20;
        labelY = footerY - 13;
        listH = Math.max(ROW_H, labelY - 3 - listY);

        int half = (listW - 4) / 2;
        addRenderableWidget(Button.builder(Component.literal("重置为钻石"), b -> {
            DiamondHudConfig.resetOre();
            refresh();
        }).bounds(listX, footerY, half, 14).build());
        addRenderableWidget(Button.builder(Component.literal("返回"), b -> onClose())
                .bounds(listX + half + 4, footerY, listW - half - 4, 14).build());

        refresh();
    }

    private void refresh() {
        scroll = Mth.clamp(scroll, 0, Math.max(0, OreTargets.ALL.size() - rows()));
    }

    private int rows() {
        return Math.max(1, listH / ROW_H);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.renderBackground(g, mouseX, mouseY, partial);

        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL);
        g.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY, EDGE);
        g.fill(panelX - 1, panelY + panelH, panelX + panelW + 1, panelY + panelH + 1, EDGE);
        g.fill(panelX - 1, panelY, panelX, panelY + panelH, EDGE);
        g.fill(panelX + panelW, panelY, panelX + panelW + 1, panelY + panelH, EDGE);

        String title = "◆ 选择矿石";
        g.drawString(font, title, listX, panelY + 6, text(TITLE), false);
        String sub = "点一行选中 · 键位在 选项→控制→按键绑定 里设";
        g.drawString(font, sub, listX + font.width(title) + 8, panelY + 6, text(DIM), false);
        g.fill(panelX + 4, listY - 5, panelX + panelW - 4, listY - 4, DIVIDER);

        g.fill(listX - 1, listY - 1, listX + listW + 1, listY + listH + 1, 0x60000000);

        for (int i = 0; i < rows(); i++) {
            int idx = scroll + i;
            if (idx >= OreTargets.ALL.size()) {
                break;
            }
            OreTargets.Ore ore = OreTargets.ALL.get(idx);
            int ry = listY + i * ROW_H;
            boolean selected = DiamondHudConfig.isOre(ore.key);
            boolean hover = mouseX >= listX && mouseX < listX + listW
                    && mouseY >= ry && mouseY < ry + ROW_H;
            if (selected) {
                // the chosen row gets a solid bar, so one target is readable at a glance
                g.fill(listX, ry, listX + listW, ry + ROW_H, 0x6020CC20);
            } else if (hover) {
                g.fill(listX, ry, listX + listW, ry + ROW_H, ROW_HOVER);
            }
            String mark = (selected ? "● " : "○ ") + ore.label;
            g.drawString(font, mark, listX + 3, ry + 2,
                    text(selected ? GREEN : (hover ? TITLE : BODY)), false);

            String key = DiamondHudKeys.keyName(DiamondHudKeys.oreKey(ore.key));
            g.drawString(font, key, listX + listW - font.width(key) - 3, ry + 2,
                    text(DiamondHudKeys.oreKey(ore.key).isUnbound() ? DIM : ACCENT), false);
        }

        String foot = "当前：" + OreTargets.label(DiamondHudConfig.ore());
        g.drawString(font, foot, listX, labelY, text(ACCENT), false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        if (mx >= listX && mx < listX + listW && my >= listY && my < listY + listH) {
            int idx = scroll + (int) ((my - listY) / ROW_H);
            if (idx >= 0 && idx < OreTargets.ALL.size()) {
                DiamondHudConfig.selectOre(OreTargets.ALL.get(idx).key);
                refresh();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int max = Math.max(0, OreTargets.ALL.size() - rows());
        scroll = Mth.clamp(scroll - (int) Math.signum(dy), 0, max);
        return true;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
