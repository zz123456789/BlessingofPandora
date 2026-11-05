package com.blessingofpandora.network;

import com.blessingofpandora.BlessingDefinitions;
import com.blessingofpandora.BlessingOfPandora;
import com.blessingofpandora.BlessingReversals;
import com.blessingofpandora.ReversalPermission;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** A bounded per-connection inbox: network bursts never enqueue one server task per packet. */
@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID)
public final class ReversalClientRequests
{
    public static final int SYNC_INTERVAL_TICKS = 5;
    private static final ConcurrentHashMap<ServerPlayer, Inbox> INBOXES = new ConcurrentHashMap<>();

    private static final class Inbox
    {
        int toggles;
        boolean requested, touched;
        // These three fields belong only to the server thread.
        boolean sendConfig, sendState;
        long nextSync;
    }

    private ReversalClientRequests() {}

    public static void request(ServerPlayer player, UUID claimedPlayer)
    {
        if (player == null || !player.getUUID().equals(claimedPlayer)) return;
        Inbox box = INBOXES.computeIfAbsent(player, ignored -> new Inbox());
        synchronized (box) { box.requested = true; }
    }

    public static void toggle(ServerPlayer player, UUID claimedPlayer, BlessingDefinitions definition)
    {
        if (player == null || definition == null || !player.getUUID().equals(claimedPlayer)) return;
        Inbox box = INBOXES.computeIfAbsent(player, ignored -> new Inbox());
        synchronized (box)
        {
            // Preserve the final state of every click, including an even number in the same tick.
            box.toggles ^= 1 << definition.ordinal();
            box.touched = true;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || INBOXES.isEmpty()) return;
        var server = event.getServer();
        long now = server.overworld().getGameTime();
        INBOXES.forEach((player, box) -> {
            if (player.server != server) return;
            if (player.isRemoved() || player.connection == null || !player.connection.connection.isConnected())
            { INBOXES.remove(player, box); return; }
            int toggles;
            synchronized (box)
            {
                toggles = box.toggles;
                box.sendConfig |= box.requested;
                box.sendState |= box.requested || box.touched;
                box.toggles = 0;
                box.requested = box.touched = false;
            }
            if (toggles != 0)
                for (BlessingDefinitions definition : BlessingDefinitions.values())
                    if ((toggles & 1 << definition.ordinal()) != 0) ReversalPermission.toggle(player, definition);
            // Mutations follow the next server tick; only responses are throttled.
            if (box.sendState && now >= box.nextSync)
            {
                if (box.sendConfig) BlessingNetwork.syncConfig(player);
                BlessingReversals.syncReversalState(player);
                box.sendConfig = box.sendState = false;
                box.nextSync = now + SYNC_INTERVAL_TICKS;
            }
        });
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event)
    { if (event.getEntity() instanceof ServerPlayer player) INBOXES.remove(player); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { INBOXES.clear(); }
}
