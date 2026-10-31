package com.blessingofpandora;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID)
public class BlessingOfPandoraEvents
{
    private static final Map<UUID, Double> FALL_STARTS = new HashMap<>();

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
        if (!player.isAlive() || player.level().dimension() != Level.OVERWORLD)
        {
            FALL_STARTS.remove(id);
            return;
        }

        double y = player.getY();
        int configuredStartY = BlessingOfPandoraConfig.secondStartY();
        int configuredTargetY = BlessingOfPandoraConfig.secondTargetY();
        boolean falling = !player.onGround() && player.getDeltaMovement().y < 0.0;
        Double trackedStartY = FALL_STARTS.get(id);

        if (falling && y >= configuredStartY)
        {
            trackedStartY = y;
        }

        if (trackedStartY != null && y <= configuredTargetY)
        {
            convertWornProximity(player);
            FALL_STARTS.remove(id);
            return;
        }

        if (falling && trackedStartY != null)
        {
            FALL_STARTS.put(id, trackedStartY);
        }
        else
        {
            FALL_STARTS.remove(id);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        FALL_STARTS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event)
    {
        FALL_STARTS.remove(event.getOriginal().getUUID());
        FALL_STARTS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        FALL_STARTS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event)
    {
        FALL_STARTS.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onWitherKilled(LivingDeathEvent event)
    {
        if (event.isCanceled() || event.getEntity().level().isClientSide
                || !ReversalConfigUtil.entityMatches(
                        event.getEntity(), BlessingOfPandoraConfig.firstMob()))
        {
            return;
        }
        Player player = ReversalCombatCredit.player(event.getSource());
        if (player != null)
        {
            KillReversalProgress.tryConversion(player, BlessingDefinitions.FREEDOM);
        }
    }

    private static void convertWornProximity(Player player)
    {
        ReversalActions.convertWornCurse(player, BlessingDefinitions.INFINITY);
    }
}
