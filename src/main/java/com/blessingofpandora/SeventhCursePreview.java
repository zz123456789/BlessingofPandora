package com.blessingofpandora;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Select the inspected hand explicitly; global views continue to describe worn equipment. */
public final class SeventhCursePreview
{
    private SeventhCursePreview() {}

    public static long count(Player player, InteractionHand inspectedHand)
    {
        if (player == null) return 0;
        var curse = BlessingDefinitions.LIBERATION.curseItem();
        if (curse == null) return 0;
        ItemStack stack = inspectedHand == null
                ? BlessingReversals.findWornCurseStack(player, curse)
                : player.getItemInHand(inspectedHand);
        return stack.is(curse) && !BlessingNbt.isBlessing(stack) ? CurseEnchantments.count(stack) : 0;
    }
}
