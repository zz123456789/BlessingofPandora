package com.blessingofpandora.mixin;

import com.blessingofpandora.client.BlessingAdvancementStyle;
import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementWidget.class)
public abstract class BlessingAdvancementWidgetMixin
{
    @Shadow @Final private Advancement advancement;
    @Shadow @Final @Mutable private int width;
    @Shadow @Final @Mutable private List<FormattedCharSequence> description;

    @Shadow
    private List<FormattedText> findOptimalLines(Component text, int contentWidth)
    {
        throw new AssertionError();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void blessingofpandora$giveDescriptionRoom(AdvancementTab tab, Minecraft minecraft,
                                                      Advancement advancement, DisplayInfo display,
                                                      CallbackInfo ci)
    {
        if (!BlessingAdvancementStyle.isStyled(advancement))
        {
            return;
        }

        // Vanilla derives the initial wrap width from the title. Two-character Chinese
        // names otherwise squeeze these descriptions into a tall, unreadable column.
        // Keep the existing space for long titles/progress and vanilla's line splitting,
        // language ordering and active resource-pack font measurements.
        int contentWidth = Math.max(130, width - 8);
        description = Language.getInstance().getVisualOrder(findOptimalLines(
                ComponentUtils.mergeStyles(display.getDescription().copy(),
                        Style.EMPTY.withColor(display.getFrame().getChatColor())), contentWidth));
        for (FormattedCharSequence line : description)
        {
            contentWidth = Math.max(contentWidth, minecraft.font.width(line));
        }
        width = contentWidth + 8;
    }

    // The title uses the five-argument overload; description lines use the six-argument one.
    // Both the left- and right-opening hover layouts must retain the animated title.
    @Redirect(method = "drawHover", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"),
            require = 2, allow = 2)
    private int blessingofpandora$drawTitle(GuiGraphics graphics, Font font,
                                          FormattedCharSequence text, int x, int y, int color)
    {
        if (BlessingAdvancementStyle.isStyled(advancement))
        {
            return BlessingAdvancementStyle.drawTitle(graphics, font, text, x, y, color, true);
        }
        return graphics.drawString(font, text, x, y, color);
    }
}
