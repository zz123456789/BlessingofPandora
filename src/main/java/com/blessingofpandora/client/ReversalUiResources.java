package com.blessingofpandora.client;

import com.blessingofpandora.BlessingOfPandora;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ReversalUiResources
{
    private static volatile long revision;

    private ReversalUiResources() {}

    @SubscribeEvent
    public static void registerReloadListener(RegisterClientReloadListenersEvent event)
    {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> revision++);
    }

    public static long revision()
    {
        return revision;
    }
}
