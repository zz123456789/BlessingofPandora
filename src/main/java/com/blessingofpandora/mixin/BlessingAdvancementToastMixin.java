package com.blessingofpandora.mixin;

import com.blessingofpandora.client.BlessingAdvancementStyle;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AdvancementToast.class)
public abstract class BlessingAdvancementToastMixin
{
    @Shadow @Final private Advancement advancement;

    // Only the advancement title is a FormattedCharSequence. The "Advancement Made!"
    // label uses Component, so its vanilla rendering and fade timing are untouched.
    @Redirect(method = "render", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I"),
            require = 2, allow = 2)
    private int blessingofpandora$drawTitle(GuiGraphics graphics, Font font,
                                          FormattedCharSequence text, int x, int y, int color,
                                          boolean shadow)
    {
        if (BlessingAdvancementStyle.isStyled(advancement))
        {
            return BlessingAdvancementStyle.drawTitle(graphics, font, text, x, y, color, shadow);
        }
        return graphics.drawString(font, text, x, y, color, shadow);
    }
}
