package com.blessingofpandora.compat.love;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Combat benefits for the authoritative evolved pair; never changes damage attribution. */
public final class EternalLoveCombat
{
    public static final double DAMAGE_MULTIPLIER = 14.14;
    public static final int FIXED_ARMOR = 520;
    public static final long REVIVE_COOLDOWN = 60;
    public static final String REVIVE_READY = "blessingofpandora:eternal_love_revive_ready";
    // Integrated clients and their servers share this class. Weak keys and weak values
    // prevent an AttributeInstance -> entity -> attribute retention cycle.
    private static final Map<AttributeInstance, WeakReference<LivingEntity>> ARMOR_OWNERS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static boolean registered;

    private EternalLoveCombat() {}

    public static void register()
    {
        if (registered) return;
        registered = true;
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(EventPriority.HIGHEST, EternalLoveCombat::onPlayerAttack);
        bus.addListener(EventPriority.HIGHEST, EternalLoveCombat::onLivingAttack);
        bus.addListener(EventPriority.HIGHEST, EternalLoveCombat::onLivingHurt);
        bus.addListener(EventPriority.HIGHEST, EternalLoveCombat::onLivingDeath);
        bus.addListener(EternalLoveCombat::onClone);
        bus.addListener((ServerStoppedEvent event) -> ARMOR_OWNERS.clear());
    }

    /** Called only on an activation/state-sync transition, not on every damage query. */
    public static void activate(LivingEntity entity)
    {
        AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
        if (armor != null) ARMOR_OWNERS.put(armor, new WeakReference<>(entity));
    }

    public static void deactivate(LivingEntity entity)
    {
        AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
        if (armor != null) ARMOR_OWNERS.remove(armor);
        // Cooldown deliberately survives unequipping, relogging and dimensional transfer.
    }

    /** Override the final result only; preserve every original base value and modifier. */
    public static double armorValue(AttributeInstance attribute, double original)
    {
        if (attribute.getAttribute() != Attributes.ARMOR) return original;
        WeakReference<LivingEntity> reference = ARMOR_OWNERS.get(attribute);
        LivingEntity entity = reference == null ? null : reference.get();
        return entity != null && EternalLoveEffects.isActive(entity) ? FIXED_ARMOR : original;
    }

    public static int armorValue(LivingEntity entity, int original)
    {
        return EternalLoveEffects.isActive(entity) ? FIXED_ARMOR : original;
    }

    /** Bounded ownership traversal supports arrows, thrown potions and other traceable projectiles. */
    public static LivingEntity responsibleAttacker(DamageSource source)
    {
        LivingEntity attacker = livingOwner(source.getEntity());
        return attacker != null ? attacker : livingOwner(source.getDirectEntity());
    }

    private static LivingEntity livingOwner(Entity entity)
    {
        for (int i = 0; entity != null && i < 8; i++)
        {
            if (entity instanceof LivingEntity living) return living;
            if (!(entity instanceof TraceableEntity traceable)) return null;
            Entity next = traceable.getOwner();
            if (next == entity) return null;
            entity = next;
        }
        return null;
    }

    public static boolean blocksAttack(LivingEntity target, DamageSource source)
    {
        LivingEntity attacker = responsibleAttacker(source);
        return attacker != null && EternalLoveEffects.isBoundPair(attacker, target);
    }

    public static float scaleDamage(LivingEntity target, DamageSource source, float amount)
    {
        if (target.level().isClientSide || !Float.isFinite(amount) || amount <= 0) return amount;
        LivingEntity attacker = responsibleAttacker(source);
        if (attacker == null || !EternalLoveEffects.isActive(attacker)) return amount;
        return (float)Math.min(Float.MAX_VALUE, (double)amount * DAMAGE_MULTIPLIER);
    }

    /** Used before vanilla totems and before death-side effects, with an event fallback. */
    public static boolean preventDeath(LivingEntity entity, DamageSource source)
    {
        if (entity.level().isClientSide || entity.isRemoved() || entity.getServer() == null
                || !EternalLoveEffects.isActive(entity)) return false;
        float maximum = entity.getMaxHealth();
        if (!Float.isFinite(maximum) || maximum <= 0) return false;
        long now = entity.getServer().overworld().getGameTime();
        CompoundTag data = entity.getPersistentData();
        if (data.contains(REVIVE_READY) && now < data.getLong(REVIVE_READY)) return false;
        // Record only when this call actually performs a rescue. No shared player/maid timer.
        data.putLong(REVIVE_READY, now + REVIVE_COOLDOWN);
        entity.setHealth(maximum);
        entity.deathTime = 0;
        return true;
    }

    private static void onPlayerAttack(AttackEntityEvent event)
    {
        if (event.getTarget() instanceof LivingEntity target
                && EternalLoveEffects.isBoundPair(event.getEntity(), target)) event.setCanceled(true);
    }

    private static void onLivingAttack(LivingAttackEvent event)
    {
        if (blocksAttack(event.getEntity(), event.getSource())) event.setCanceled(true);
    }

    private static void onLivingHurt(LivingHurtEvent event)
    {
        // This is the first mutable Forge damage stage. There is no second multiplier
        // in the health-commit hook, so a hit is scaled exactly once.
        event.setAmount(scaleDamage(event.getEntity(), event.getSource(), event.getAmount()));
    }

    private static void onLivingDeath(LivingDeathEvent event)
    {
        if (preventDeath(event.getEntity(), event.getSource())) event.setCanceled(true);
    }

    private static void onClone(PlayerEvent.Clone event)
    {
        CompoundTag before = event.getOriginal().getPersistentData();
        if (before.contains(REVIVE_READY))
            event.getEntity().getPersistentData().putLong(REVIVE_READY, before.getLong(REVIVE_READY));
    }
}
