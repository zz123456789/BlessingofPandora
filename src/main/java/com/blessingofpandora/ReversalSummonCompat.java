package com.blessingofpandora;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.UUID;

/** Optional ownership APIs that do not implement Minecraft's ownership interfaces. */
final class ReversalSummonCompat
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private ReversalSummonCompat() {}

    static Entity owner(Entity entity)
    {
        if (entity == null) return null;
        Entity owner = Adapters.IRON.owner(entity);
        return owner != null ? owner : Adapters.ARS_FAMILIAR.owner(entity);
    }

    static UUID ownerUuid(Entity entity)
    {
        return entity == null ? null : Adapters.ARS_FAMILIAR.ownerUuid(entity);
    }

    private static final class Adapters
    {
        // Verified in Iron's Spells 'n Spellbooks 3.16.2 and 3.16.3 / Minecraft 1.20.1. The default method
        // delegates to SummonManager's UUID map; there is no owner UUID API here.
        private static final Adapter IRON = Adapter.load(
                "io.redspace.ironsspellbooks.entity.mobs.IMagicSummon",
                "getSummoner", Entity.class, null);
        // Verified in Ars Nouveau 4.12.7/1.20.1. Ordinary Ars summons already use
        // OwnableEntity; familiars instead expose this separate public interface.
        private static final Adapter ARS_FAMILIAR = Adapter.load(
                "com.hollingsworth.arsnouveau.api.familiar.IFamiliar",
                "getOwner", LivingEntity.class, "getOwnerID");
    }

    private static final class Adapter
    {
        private final Class<?> type;
        private final MethodHandle owner;
        private final MethodHandle ownerUuid;
        private volatile boolean disabled;

        private Adapter(Class<?> type, MethodHandle owner, MethodHandle ownerUuid)
        {
            this.type = type;
            this.owner = owner;
            this.ownerUuid = ownerUuid;
        }

        private static Adapter load(String className, String ownerMethod,
                                    Class<? extends Entity> returnType, String uuidMethod)
        {
            try
            {
                Class<?> type = Class.forName(className, false, ReversalSummonCompat.class.getClassLoader());
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                MethodHandle owner = lookup.findVirtual(type, ownerMethod, MethodType.methodType(returnType))
                        .asType(MethodType.methodType(Entity.class, Object.class));
                MethodHandle uuid = uuidMethod == null ? null
                        : lookup.findVirtual(type, uuidMethod, MethodType.methodType(UUID.class))
                        .asType(MethodType.methodType(UUID.class, Object.class));
                return new Adapter(type, owner, uuid);
            }
            catch (ClassNotFoundException ignored)
            {
                // The optional mod is absent. Do not add a mandatory dependency.
            }
            catch (ReflectiveOperationException | LinkageError | RuntimeException failure)
            {
                LOGGER.warn("[BlessingOfPandora] Optional reversal ownership API unavailable: {}", className, failure);
            }
            return new Adapter(null, null, null);
        }

        private Entity owner(Entity entity)
        {
            if (disabled || type == null || !type.isInstance(entity)) return null;
            try
            {
                return (Entity) owner.invokeExact((Object) entity);
            }
            catch (Throwable failure)
            {
                disable(failure);
                return null;
            }
        }

        private UUID ownerUuid(Entity entity)
        {
            if (disabled || type == null || ownerUuid == null || !type.isInstance(entity)) return null;
            try
            {
                return (UUID) ownerUuid.invokeExact((Object) entity);
            }
            catch (Throwable failure)
            {
                disable(failure);
                return null;
            }
        }

        private synchronized void disable(Throwable failure)
        {
            if (failure instanceof VirtualMachineError error) throw error;
            if (failure instanceof ThreadDeath death) throw death;
            if (disabled) return;
            disabled = true;
            LOGGER.warn("[BlessingOfPandora] Disabling incompatible reversal ownership API: {}", type.getName(), failure);
        }
    }
}
