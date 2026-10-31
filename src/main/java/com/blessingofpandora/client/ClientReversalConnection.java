package com.blessingofpandora.client;

import com.blessingofpandora.BlessingOfPandora;
import com.blessingofpandora.ReversalSettings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID, value = Dist.CLIENT)
public final class ClientReversalConnection
{
    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event)
    {
        ReversalSettings.setServerSettings(null);
        boolean[] allowed = new boolean[com.blessingofpandora.BlessingDefinitions.values().length];
        java.util.Arrays.fill(allowed, true);
        ClientReversalPermission.setStates(allowed, 0, 0, 0, 0);
    }
}
