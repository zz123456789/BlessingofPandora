package com.blessingofpandora.mixin;

import com.blessingofpandora.client.BlessingAdvancementChatStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChatComponent.class)
public abstract class BlessingAdvancementChatMixin
{
    @Unique
    private final BlessingAdvancementChatStyle blessingofpandora$advancementStyle =
            new BlessingAdvancementChatStyle();

    // Keep the original cached chat lines, signatures, hit testing, fade and wrapping.
    @Redirect(method = "render", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"),
            require = 1, allow = 1)
    private int blessingofpandora$drawAdvancementNames(GuiGraphics graphics, Font font,
                                                       FormattedCharSequence line, int x, int y, int color)
    {
        return blessingofpandora$advancementStyle.drawLine(graphics, font, line, x, y, color);
    }
}
