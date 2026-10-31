package com.blessingofpandora;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;

/** Resolves combat credit for reversal conditions without changing the actual damage source. */
public final class ReversalCombatCredit
{
    // Broken/cyclic ownership supplied by another mod must not stall the server.
    private static final int MAX_OWNER_DEPTH = 32;

    private ReversalCombatCredit() {}

    @Nullable
    public static Player player(DamageSource source)
    {
        Entity attacker = source.getEntity();
        // The causing entity is authoritative. Do not steal another attacker's kill by
        // falling back to an unrelated direct projectile when its owner cannot be found.
        return player(attacker != null ? attacker : source.getDirectEntity());
    }

    @Nullable
    public static Player player(@Nullable Entity attacker)
    {
        Entity current = attacker;
        for (int depth = 0; current != null && depth < MAX_OWNER_DEPTH; depth++)
        {
            if (current instanceof Player player) return player;

            Entity owner = null;
            if (current instanceof OwnableEntity ownable)
            {
                // Covers tame animals (including maids), Ars summons and Goety servants.
                owner = ownable.getOwner();
                if (owner == null) owner = findOwner(current, ownable.getOwnerUUID());
            }
            if (owner == null && current instanceof TraceableEntity traceable)
            {
                // Follow arrows, spell projectiles, fangs, clouds and summon chains.
                owner = traceable.getOwner();
            }
            if (owner == null)
            {
                // Some custom summons also inherit a vanilla owner API that returns null
                // (for example Iron's summoned vex uses its own summon manager).
                owner = ReversalSummonCompat.owner(current);
                if (owner == null) owner = findOwner(current, ReversalSummonCompat.ownerUuid(current));
            }

            if (owner == current) return null;
            current = owner;
        }
        return null;
    }

    @Nullable
    private static Entity findOwner(Entity entity, @Nullable UUID id)
    {
        if (id == null) return null;
        MinecraftServer server = entity.getServer();
        if (server == null) return null;
        // Online owners can be in another dimension. No entity/chunk scans or loading.
        Player player = server.getPlayerList().getPlayer(id);
        if (player != null) return player;
        return entity.level() instanceof ServerLevel level ? level.getEntity(id) : null;
    }
}
