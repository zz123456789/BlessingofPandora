package com.blessingofpandora;

import com.blessingofpandora.network.BlessingNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID)
public final class ReversalPermissionHandler
{
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event)
    {
        PandoraProgress.copyCount(event.getOriginal(), event.getEntity());
        KillReversalProgress.copy(event.getOriginal(), event.getEntity());
    }

    private ReversalPermissionHandler()
    {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            PandoraProgress.restoreCount(player);
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawned(PlayerEvent.PlayerRespawnEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    private static void sync(ServerPlayer player)
    {
        BlessingNetwork.syncConfig(player);
        BlessingReversals.syncReversalState(player);
    }
}
