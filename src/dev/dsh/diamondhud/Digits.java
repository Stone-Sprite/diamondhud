package dev.dsh.diamondhud;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A 5x7 pixel digit font drawn with {@code fill()}.
 *
 * <p>5x7 is the size of the vanilla font's digits, so the number reads at the same
 * weight as the rest of the HUD. An earlier build used a 3x5 font with a one-pixel
 * dilation as an outline; at that size the dilation ate the glyph and adjacent
 * digits ran together, which is what made the number look broken. A single
 * offset shadow gives the same readability without touching the glyph.
 *
 * <p>Drawn with fill() rather than GuiGraphics.drawString() on purpose: the two do
 * not agree on the byte order of a colour int in this version, so the same value
 * came out as the intended colour through fill() and as a different one through
 * drawString().
 */
final class Digits {

    private Digits() {
    }

    private static final String[][] GLYPHS = {
        {"01110", "10001", "10011", "10101", "11001", "10001", "01110"},   // 0
        {"00100", "01100", "00100", "00100", "00100", "00100", "01110"},   // 1
        {"01110", "10001", "00001", "00010", "00100", "01000", "11111"},   // 2
        {"11111", "00010", "00100", "00010", "00001", "10001", "01110"},   // 3
        {"00010", "00110", "01010", "10010", "11111", "00010", "00010"},   // 4
        {"11111", "10000", "11110", "00001", "00001", "10001", "01110"},   // 5
        {"00110", "01000", "10000", "11110", "10001", "10001", "01110"},   // 6
        {"11111", "10001", "00001", "00010", "00100", "00100", "00100"},   // 7
        {"01110", "10001", "10001", "01110", "10001", "10001", "01110"},   // 8
        {"01110", "10001", "10001", "01111", "00001", "00010", "01100"},   // 9
    };

    private static final int GW = 5;
    private static final int GH = 7;
    private static final int GAP = 1;

    static int width(String text) {
        int n = text.length();
        return n <= 0 ? 0 : n * GW + (n - 1) * GAP;
    }

    static int height() {
        return GH;
    }

    static void draw(GuiGraphics g, String text, int x, int y, int px) {
        // shadow first, offset by one source pixel
        paint(g, text, x + px, y + px, px, 0xC0000000);
        paint(g, text, x, y, px, Arrows.TEXT);
    }

    private static void paint(GuiGraphics g, String text, int x, int y, int px, int argb) {
        int cursor = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                String[] glyph = GLYPHS[c - '0'];
                for (int gy = 0; gy < GH; gy++) {
                    String row = glyph[gy];
                    for (int gx = 0; gx < GW; gx++) {
                        if (row.charAt(gx) != '1') {
                            continue;
                        }
                        int px1 = cursor + gx * px;
                        int py1 = y + gy * px;
                        g.fill(px1, py1, px1 + px, py1 + px, argb);
                    }
                }
            }
            cursor += (GW + GAP) * px;
        }
    }
}
