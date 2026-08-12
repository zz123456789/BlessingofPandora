package com.blessingofpandora;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.event.CurioEquipEvent;

@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID)
public final class PandoraBraceletHandler
{
    private PandoraBraceletHandler()
    {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        Player player = event.player;
        if (player.level().isClientSide)
        {
            return;
        }
        PandoraProgress.onPlayerTick(player);
    }

    @SubscribeEvent
    public static void onEquip(CurioEquipEvent event)
    {
        if (event.getEntity().level().isClientSide)
        {
            return;
        }
        if (PandoraBraceletItem.isPandoraBracelet(event.getStack())
                && !PandoraBraceletItem.isOwner(event.getStack(), event.getEntity().getUUID()))
        {
            event.setResult(Event.Result.DENY);
        }
    }
}
