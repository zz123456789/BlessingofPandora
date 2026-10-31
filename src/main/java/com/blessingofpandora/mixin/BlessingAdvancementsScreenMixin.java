package com.blessingofpandora.mixin;

import com.blessingofpandora.client.BlessingAdvancementStyle;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AdvancementsScreen.class)
public abstract class BlessingAdvancementsScreenMixin
{
    @Shadow @Final private Map<Advancement, AdvancementTab> tabs;

    @Redirect(method = "renderTooltips", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/GuiGraphics;renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;II)V"))
    private void blessingofpandora$drawTabTitle(GuiGraphics graphics, Font font,
                                              Component title, int mouseX, int mouseY)
    {
        for (Map.Entry<Advancement, AdvancementTab> entry : tabs.entrySet())
        {
            // Identity ties this hook to the actual tab, including when another mod
            // happens to use the same visible title or translation text.
            if (entry.getValue().getTitle() != title || !BlessingAdvancementStyle.isStyled(entry.getKey()))
            {
                continue;
            }

            FormattedCharSequence text = title.getVisualOrderText();
            int width = font.width(text);
            int height = font.lineHeight + 2;
            Vector2ic position = DefaultTooltipPositioner.INSTANCE.positionTooltip(
                    graphics.guiWidth(), graphics.guiHeight(), mouseX, mouseY, width, height);
            graphics.pose().pushPose();
            try
            {
                graphics.drawManaged(() -> TooltipRenderUtil.renderTooltipBackground(
                        graphics, position.x(), position.y(), width, height, 400));
                graphics.pose().translate(0.0F, 0.0F, 400.0F);
                BlessingAdvancementStyle.drawTitle(graphics, font, text,
                        position.x(), position.y() + 1, -1, true);
            }
            finally
            {
                graphics.pose().popPose();
            }
            return;
        }
        graphics.renderTooltip(font, title, mouseX, mouseY);
    }
}
