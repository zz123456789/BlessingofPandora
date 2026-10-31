package com.blessingofpandora.client;

import com.blessingofpandora.BlessingOfPandora;
import com.blessingofpandora.compat.love.EternalLoveCombat;
import com.blessingofpandora.network.EternalLoveStateS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Display-side armor authority, never accepted as a source of server abilities. */
@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID, value = Dist.CLIENT)
public final class EternalLoveClient
{
    private static final Map<UUID, Tracked> ACTIVE = new HashMap<>();
    private static final class Tracked
    {
        final int id;
        WeakReference<LivingEntity> entity = new WeakReference<>(null);
        Tracked(int id) { this.id = id; }
    }
    private EternalLoveClient() {}
    public static boolean isActive(LivingEntity entity)
    {
        Tracked tracked = ACTIVE.get(entity.getUUID());
        return tracked != null && tracked.id == entity.getId() && tracked.entity.get() == entity;
    }
    public static void receive(EternalLoveStateS2CPacket packet)
    {
        Tracked previous = ACTIVE.get(packet.entityUuid());
        if (packet.active()) ACTIVE.put(packet.entityUuid(), new Tracked(packet.entityId()));
        else if (previous != null && previous.id == packet.entityId()) ACTIVE.remove(packet.entityUuid());
        else return; // An old instance's removal must not revoke its replacement's display state.
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(packet.entityId()) instanceof LivingEntity entity
                && entity.getUUID().equals(packet.entityUuid()))
        {
            if (packet.active())
            {
                ACTIVE.get(packet.entityUuid()).entity = new WeakReference<>(entity);
                EternalLoveCombat.activate(entity);
            }
            else EternalLoveCombat.deactivate(entity);
        }
    }
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event)
    {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living)
        {
            Tracked tracked = ACTIVE.get(living.getUUID());
            if (tracked != null && tracked.id == living.getId())
            {
                tracked.entity = new WeakReference<>(living);
                EternalLoveCombat.activate(living);
            }
        }
    }
    @SubscribeEvent public static void left(EntityLeaveLevelEvent event)
    {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living)
        {
            Tracked tracked = ACTIVE.get(living.getUUID());
            if (tracked != null && tracked.entity.get() == living) ACTIVE.remove(living.getUUID());
            EternalLoveCombat.deactivate(living);
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ACTIVE.clear(); }
}
