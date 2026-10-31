package com.blessingofpandora.compat.love;

import com.blessingofpandora.LoveOrbRegistration;
import com.blessingofpandora.network.BlessingNetwork;
import com.blessingofpandora.network.EternalLoveStateS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** One authority per equipped owner; combat lookups never scan inventories or worlds. */
public final class EternalLoveEffects
{
    private static final Map<UUID, Link> OWNERS = new HashMap<>();
    private static final Map<UUID, Link> MAIDS = new HashMap<>();
    private static boolean registered;

    private static final class Link
    {
        final ServerPlayer owner;
        final UUID maidId;
        WeakReference<LivingEntity> maid = new WeakReference<>(null);
        Link(ServerPlayer owner, UUID maidId) { this.owner = owner; this.maidId = maidId; }
    }

    private EternalLoveEffects() {}

    public static void register()
    {
        if (registered || !LoveOrbRegistration.isAvailable()) return;
        registered = true;
        EternalLoveCombat.register();
        EternalLoveMobility.register();
        MinecraftForge.EVENT_BUS.addListener(EternalLoveEffects::onStartTracking);
        MinecraftForge.EVENT_BUS.addListener(EternalLoveEffects::onDimensionChanged);
    }

    /** Invoked from the same authoritative equipment snapshot as Love progression. */
    public static void observe(ServerPlayer player, boolean worn, LoveLedger.Bond bond)
    {
        if (!worn || bond == null || !bond.evolved || bond.banned)
        {
            clear(player);
            return;
        }
        Link link = OWNERS.get(player.getUUID());
        if (link != null && link.owner != player) { clear(link.owner); link = null; }
        if (link == null)
        {
            link = new Link(player, bond.maid);
            OWNERS.put(player.getUUID(), link);
            MAIDS.put(bond.maid, link);
            EternalLoveCombat.activate(player);
            broadcast(player, true);
        }
        attach(link, LoveProgressEvents.loadedMaid(bond.maid));
    }

    /** Health may already be zero at the death-prevention hook; do not test isAlive here. */
    public static boolean isActive(LivingEntity entity)
    {
        if (entity.level().isClientSide)
            return com.blessingofpandora.client.EternalLoveClient.isActive(entity);
        if (entity.isRemoved()) return false;
        if (entity instanceof ServerPlayer player)
        {
            Link link = OWNERS.get(player.getUUID());
            return link != null && link.owner == player;
        }
        Link link = MAIDS.get(entity.getUUID());
        return link != null && !link.owner.isRemoved() && link.maid.get() == entity
                && entity instanceof TamableAnimal tame && link.owner.getUUID().equals(tame.getOwnerUUID());
    }

    public static boolean isBoundPair(LivingEntity attacker, LivingEntity target)
    {
        if (!(attacker instanceof ServerPlayer player) || !isActive(player)) return false;
        Link link = OWNERS.get(player.getUUID());
        return link.maidId.equals(target.getUUID()) && isActive(target);
    }

    public static LivingEntity linkedMaid(ServerPlayer player)
    {
        Link link = OWNERS.get(player.getUUID());
        LivingEntity maid = link == null ? null : link.maid.get();
        return maid != null && isActive(maid) ? maid : null;
    }

    public static void tick(ServerPlayer player)
    {
        Link link = OWNERS.get(player.getUUID());
        if (link == null || link.owner != player) return;
        if (!player.isAlive() || player.isRemoved()) { clear(player); return; }
        attach(link, LoveProgressEvents.loadedMaid(link.maidId));
        EternalLoveMobility.tick(player, linkedMaid(player));
    }

    public static void maidJoined(LivingEntity maid)
    {
        Link link = MAIDS.get(maid.getUUID());
        if (link != null) attach(link, maid);
    }

    public static void maidLeft(LivingEntity maid)
    {
        Link link = MAIDS.get(maid.getUUID());
        if (link != null && link.maid.get() == maid) attach(link, null);
    }

    private static void attach(Link link, LivingEntity candidate)
    {
        if (candidate != null && (candidate.isRemoved() || !(candidate instanceof TamableAnimal tame)
                || !link.owner.getUUID().equals(tame.getOwnerUUID()))) candidate = null;
        LivingEntity old = link.maid.get();
        if (old == candidate) return;
        if (old != null) { EternalLoveCombat.deactivate(old); broadcast(old, false); }
        link.maid = new WeakReference<>(candidate);
        if (candidate != null) { EternalLoveCombat.activate(candidate); broadcast(candidate, true); }
    }

    public static void clear(ServerPlayer player)
    {
        Link link = OWNERS.get(player.getUUID());
        if (link == null || link.owner != player) return;
        OWNERS.remove(player.getUUID());
        MAIDS.remove(link.maidId, link);
        LivingEntity maid = link.maid.get();
        if (maid != null) { EternalLoveCombat.deactivate(maid); broadcast(maid, false); }
        EternalLoveCombat.deactivate(player);
        EternalLoveMobility.clear(player);
        broadcast(player, false);
    }

    private static void broadcast(LivingEntity entity, boolean active)
    {
        if (entity.isRemoved()) return;
        BlessingNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new EternalLoveStateS2CPacket(entity.getId(), entity.getUUID(), active));
    }

    private static void onStartTracking(PlayerEvent.StartTracking event)
    {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof LivingEntity entity && isActive(entity))
            BlessingNetwork.sendToPlayer(player, new EternalLoveStateS2CPacket(entity.getId(), entity.getUUID(), true));
    }

    private static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player && isActive(player)) broadcast(player, true);
    }

    public static void serverStopped()
    {
        for (Link link : List.copyOf(OWNERS.values())) clear(link.owner);
        OWNERS.clear();
        MAIDS.clear();
        EternalLoveMobility.serverStopped();
    }
}
