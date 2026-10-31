package com.blessingofpandora;

import com.blessingofpandora.client.ReversalHudClient;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.List;

public class ReversalControlItem extends Item
{
    public ReversalControlItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand)
    {
        if (!ReversalAuthorization.canUseDebugControl(player))
        {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        if (level.isClientSide)
        {
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ReversalHudClient::toggleAction);
        }
        return InteractionResultHolder.sidedSuccess(
                player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable(
                        "item.blessingofpandora.reversal_control.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }
}
