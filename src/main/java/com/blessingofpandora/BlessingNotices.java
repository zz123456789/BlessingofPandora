package com.blessingofpandora;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID)
public class BlessingNotices
{
    private static final Map<UUID, Notice> ACTIVE_NOTICES = new HashMap<>();

    private record Notice(Component message, long endTick, long lastSentTick) {}

    public static void show(Player player, Component message)
    {
        long now = player.level().getGameTime();
        ACTIVE_NOTICES.put(player.getUUID(), new Notice(message, now + 160L, now));
        player.displayClientMessage(message, true);
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

        UUID id = player.getUUID();
        Notice notice = ACTIVE_NOTICES.get(id);
        if (notice == null)
        {
            return;
        }

        long now = player.level().getGameTime();
        if (now >= notice.endTick())
        {
            ACTIVE_NOTICES.remove(id);
            return;
        }

        if (now - notice.lastSentTick() >= 10L)
        {
            player.displayClientMessage(notice.message(), true);
            ACTIVE_NOTICES.put(id, new Notice(notice.message(), notice.endTick(), now));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        ACTIVE_NOTICES.remove(event.getEntity().getUUID());
    }
}
