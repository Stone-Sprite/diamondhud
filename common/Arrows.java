package dev.dsh.diamondhud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * The arrow, drawn straight from the Create mod's own texture.
 *
 * <p>The asset is reused verbatim: the PNG shipped at
 * {@code assets/diamondhud/textures/gui/placement_indicator.png} is a byte-for-byte
 * copy of {@code assets/create/textures/gui/placement_indicator.png} from
 * {@code create-1.20.1-0.5.1.j.jar}. Nothing about it is redrawn or recoloured.
 *
 * <p>That file is 16x256: <b>sixteen 16x16 frames</b> of the arrow at successive
 * rotations, which is what Create cycles through when it draws the indicator over
 * a block. So the only logic needed here is picking the frame whose rotation
 * matches the heading we want to show — no procedural rotation, no pixel tables,
 * and the art stays exactly as its author drew it.
 */
final class Arrows {

    private Arrows() {
    }

    static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            DiamondHudMod.MODID, "textures/gui/placement_indicator.png");

    /** Side of one frame, in source pixels. */
    static final int FRAME = 16;

    /** How many rotation steps the sheet provides. 16 steps = 22.5 degrees each. */
    static final int FRAMES = 16;

    /** White, matching the indicator's own body colour, for the distance digits. */
    static final int TEXT = 0xFFFFFFFF;

    /**
     * GuiGraphics.drawString() does not read a colour int the way fill() does — the
     * same value came out yellow-green through drawString() and cyan through fill(),
     * which is an R/B swap. Push every textual colour through here so both agree.
     */
    static int text(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    /**
     * Draw the arrow with its centre at ({@code cx}, {@code cy}), turned to
     * {@code deg} (clockwise from up), each source pixel {@code px} wide.
     *
     * <p>Implemented by selecting one of the sheet's sixteen pre-rotated frames.
     * An attempt to rotate frame 0 through the pose stack instead produced headings
     * that did not match the input at all, so the frame table is used: it is the
     * path that has actually been verified on screen, and 22.5 degree steps are not
     * perceptible now that the heading itself updates every frame.
     */
    /**
     * Which of the sheet's sixteen frames draws the arrow turned {@code deg}
     * clockwise from screen-up.
     *
     * <p>Read out of the texture rather than assumed: frame 0 is the up arrow, and
     * matching frame 4 against frame 0 turned 90 degrees clockwise scores 0.71
     * against 0.63 for the counter-clockwise turn, so the index really does grow
     * clockwise. Kept as its own method so the debug log reports the same number
     * the blit uses instead of a second copy of the arithmetic.
     */
    static int frameFor(double deg) {
        double a = deg % 360.0;
        if (a < 0.0) {
            a += 360.0;
        }
        return (int) Math.round(a / (360.0 / FRAMES)) % FRAMES;
    }

    static void drawCreate(GuiGraphics g, int cx, int cy, double deg, int px) {
        int frame = frameFor(deg);
        int size = FRAME * px;
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                cx - size / 2, cy - size / 2,
                0.0f, (float) (frame * FRAME),
                size, size,
                FRAME, FRAME * FRAMES);
    }

    /**
     * Heading and pitch corrections needed to face a point, phrased as instructions.
     * The arrow only shows where the target is; when it is behind you the words are
     * what actually tell you which way to turn.
     */
    static String aimHint(double dx, double dy, double dz,
                          float playerYaw, float playerPitch) {
        double horiz = Math.sqrt(dx * dx + dz * dz);
        // Minecraft yaw runs 0 = south, 90 = west, 180 = north, 270 = east. That is
        // NOT the screen angle used for the arrow rotation, which is atan2(dx, -dz);
        // using the screen angle here inverted every left/right instruction.
        double wantYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double wantPitch = -Math.toDegrees(Math.atan2(dy, horiz));
        double errYaw = wrap(wantYaw - playerYaw);
        double errPitch = wantPitch - playerPitch;

        StringBuilder sb = new StringBuilder();
        // Yaw grows towards the player's right in this coordinate system.
        if (Math.abs(errYaw) > 8.0) {
            sb.append(errYaw > 0 ? "右转 " : "左转 ").append(Math.round(Math.abs(errYaw))).append("°");
        }
        // xRot is positive when looking down, so a positive error means look lower.
        if (Math.abs(errPitch) > 8.0) {
            if (sb.length() > 0) {
                sb.append("  ");
            }
            sb.append(errPitch > 0 ? "低头 " : "抬头 ").append(Math.round(Math.abs(errPitch))).append("°");
        }
        if (sb.length() == 0) {
            return "已对准";
        }
        return sb.toString();
    }

    private static double wrap(double deg) {
        double d = deg % 360.0;
        if (d >= 180.0) {
            d -= 360.0;
        }
        if (d < -180.0) {
            d += 360.0;
        }
        return d;
    }

    private static int fade(int argb, int alpha) {
        if (alpha >= 255) {
            return argb;
        }
        return (argb & 0x00FFFFFF) | (((argb >>> 24) * alpha / 255) << 24);
    }
}
