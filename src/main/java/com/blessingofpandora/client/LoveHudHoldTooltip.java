package com.blessingofpandora.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** Small rose progress accent below the Alt hint; independent of item data and server ticks. */
public record LoveHudHoldTooltip(int width) implements TooltipComponent
{
    public static final class Renderer implements ClientTooltipComponent
    {
        private final int width;
        public Renderer(LoveHudHoldTooltip value) { width = value.width; }
        @Override public int getHeight() { return 8; }
        @Override public int getWidth(Font font) { return width; }
        @Override public void renderImage(Font font, int x, int y, GuiGraphics graphics)
        {
            int end = x + Math.min(width, (int) Math.round(width * LoveHudKeyHandler.holdFraction()));
            graphics.fill(x, y + 2, x + width, y + 5, 0xFF47333E);
            if (end > x) graphics.fillGradient(x, y + 2, end, y + 5, 0xFFE9ACC5, 0xFFBD6C91);
        }
    }
}
