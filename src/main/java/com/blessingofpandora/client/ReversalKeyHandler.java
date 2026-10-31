package com.blessingofpandora.client;

import com.blessingofpandora.BlessingOfPandora;
import com.blessingofpandora.PandoraItemUtil;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = BlessingOfPandora.MODID,
        value = Dist.CLIENT)
public final class ReversalKeyHandler
{
    private ReversalKeyHandler()
    {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }
        while (ReversalKeybindings.OPEN_REVERSAL_HUD.consumeClick())
        {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null
                    && PandoraItemUtil.isWearingPandoraHolder(minecraft.player))
            {
                ReversalHudClient.openGlobal();
            }
        }
    }
}
