package com.blessingofpandora.client;

import java.util.function.LongSupplier;
import net.minecraft.Util;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/** Client-only lettering for this mod's advancement names; no tick handlers or network updates. */
public final class BlessingAdvancementStyle
{
    private static LongSupplier animationClock = Util::getMillis;

    private BlessingAdvancementStyle() {}

    public static boolean isStyled(Advancement advancement)
    {
        if (advancement == null || !"blessingofpandora".equals(advancement.getId().getNamespace())) return false;
        String path = advancement.getId().getPath();
        return path.equals("root") || path.startsWith("reversal/");
    }

    /** Preserves the original advance width and toast opacity while moving each glyph gently. */
    public static int drawTitle(GuiGraphics graphics, Font font, FormattedCharSequence title,
                                float x, float y, int color, boolean shadow)
    {
        return drawTitle(graphics, font, title, x, y, color, shadow, 1.05F, 1.0F, 2.0F);
    }

    /** Vanilla chat rows are only nine pixels tall, so use a finer rim and gentler movement. */
    public static int drawChatTitle(GuiGraphics graphics, Font font, FormattedCharSequence title,
                                    float x, float y, int color)
    {
        return drawTitle(graphics, font, title, x, y, color, true, 0.65F, 0.45F, 1.0F);
    }

    private static int drawTitle(GuiGraphics graphics, Font font, FormattedCharSequence title,
                                 float x, float y, int color, boolean shadow,
                                 float movement, float rim, float shadowDepth)
    {
        // Use elapsed UI time: the shimmer continues naturally while the world is paused.
        double seconds = animationClock.getAsLong() / 1000.0;
        int alpha = color & 0xFF000000;
        if (alpha == 0) alpha = 0xFF000000; // Vanilla RGB text arguments have implicit full opacity.
        final int opacity = alpha;
        float[] advance = {0};
        int[] ordinal = {0};
        title.accept((index, style, codePoint) -> {
            // Strip only the static fallback color: component colors otherwise override drawString's tint.
            Component glyph = Component.literal(new String(Character.toChars(codePoint)))
                    .setStyle(style.withColor((TextColor) null));
            // Keep fractional advances from resource-pack fonts; round only the final return value.
            float width = font.getSplitter().stringWidth(glyph);
            if (!Character.isWhitespace(codePoint))
            {
                double phase = seconds * 2.2 - ordinal[0] * 0.7;
                float bob = (float) (Math.sin(phase) * movement);
                double hue = seconds * 0.075 + advance[0] * 0.017;
                int rainbow = Mth.hsvToRgb((float) (hue - Math.floor(hue)), 0.60F, 1.0F);
                int face = blend(rainbow, 0xFFD879, 0.24);
                double glint = Math.pow(Math.max(0.0, Math.sin(seconds * 2.0 - advance[0] * 0.12)), 10.0);
                face = blend(face, 0xFFF5CF, glint * 0.85);
                float glyphX = x + advance[0];
                float glyphY = y + bob;
                // A warm metallic rim and a darker lower edge give the moving face a gilded finish.
                if (shadow) drawLayer(graphics, font, glyph, glyphX + rim, glyphY + shadowDepth, opacity | 0x35230E);
                drawLayer(graphics, font, glyph, glyphX - rim, glyphY, opacity | 0x91601D);
                drawLayer(graphics, font, glyph, glyphX + rim, glyphY, opacity | 0x91601D);
                drawLayer(graphics, font, glyph, glyphX, glyphY - rim, opacity | 0xF6CE78);
                drawLayer(graphics, font, glyph, glyphX, glyphY + rim, opacity | 0x785019);
                drawLayer(graphics, font, glyph, glyphX, glyphY, opacity | face);
            }
            advance[0] += width;
            ordinal[0]++;
            return true;
        });
        // One flush per title instead of five or six per glyph, without changing managed GUI state.
        graphics.flush();
        return (int) (x + advance[0]) + (shadow ? 1 : 0);
    }

    private static void drawLayer(GuiGraphics graphics, Font font, Component glyph,
                                  float x, float y, int color)
    {
        font.drawInBatch(glyph, x, y, color, false, graphics.pose().last().pose(),
                graphics.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);
    }

    private static int blend(int from, int to, double amount)
    {
        int red = (int) Math.round((from >> 16 & 255) * (1.0 - amount) + (to >> 16 & 255) * amount);
        int green = (int) Math.round((from >> 8 & 255) * (1.0 - amount) + (to >> 8 & 255) * amount);
        int blue = (int) Math.round((from & 255) * (1.0 - amount) + (to & 255) * amount);
        return red << 16 | green << 8 | blue;
    }
}
