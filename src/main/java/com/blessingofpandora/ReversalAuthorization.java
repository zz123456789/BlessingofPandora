package com.blessingofpandora;

import net.minecraft.world.entity.player.Player;

public final class ReversalAuthorization
{
    private ReversalAuthorization() {}

    public static boolean canUseDebugControl(Player player)
    {
        return (player.isCreative() || player.hasPermissions(2))
                && (player.getMainHandItem().is(BlessingOfPandora.REVERSAL_CONTROL.get())
                || player.getOffhandItem().is(BlessingOfPandora.REVERSAL_CONTROL.get()));
    }
}
