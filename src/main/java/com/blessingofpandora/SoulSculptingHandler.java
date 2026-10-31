package com.blessingofpandora;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID)
public final class SoulSculptingHandler
{
    private static final int EFFECT_TICKS = 100;
    private static final UUID MAX_HEALTH_MODIFIER =
            UUID.fromString("b1e55b1e-4d52-4a54-9e1e-000000000003");
    private static final UUID DAMAGE_TAKEN_MODIFIER =
            UUID.fromString("b1e55b1e-4d52-4a54-9e1e-000000000004");

    private SoulSculptingHandler()
    {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide)
        {
            return;
        }

        MobEffect soulSculpting =
                BlessingOfPandora.SOUL_SCULPTING.get();
        MobEffectInstance current =
                entity.getEffect(soulSculpting);
        if (!hasEnoughOtherEffects(entity, current != null))
        {
            if (current != null)
            {
                entity.removeEffect(soulSculpting);
            }
            updateAttributeModifiers(entity);
            return;
        }

        if (current == null || current.getDuration() < EFFECT_TICKS)
        {
            entity.removeEffect(soulSculpting);
            entity.addEffect(new MobEffectInstance(
                    soulSculpting,
                    EFFECT_TICKS,
                    0,
                    false,
                    true,
                    true));
        }
        updateAttributeModifiers(entity);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event)
    {
        if (event.isCanceled() || event.getEntity().level().isClientSide)
        {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (!entity.hasEffect(BlessingOfPandora.SOUL_SCULPTING.get()))
        {
            return;
        }

        AttributeInstance damageTaken =
                entity.getAttribute(
                        BlessingOfPandora.SOUL_DAMAGE_TAKEN.get());
        if (damageTaken == null)
        {
            return;
        }

        double multiplier = damageTaken.getValue();
        if (multiplier != 1.0)
        {
            event.setAmount(
                    (float) (event.getAmount() * multiplier));
        }
    }

    private static boolean hasEnoughOtherEffects(LivingEntity entity, boolean hasSoulSculpting)
    {
        // Active effects are keyed by type: amplifier upgrades never add a kind.
        return entity.getActiveEffects().size() - (hasSoulSculpting ? 1 : 0) >= 10;
    }

    private static void updateAttributeModifiers(LivingEntity entity)
    {
        boolean active = entity.hasEffect(
                BlessingOfPandora.SOUL_SCULPTING.get());
        applyMaxHealthModifier(entity, active);
        applyDamageTakenModifier(entity, active);
    }

    private static void applyMaxHealthModifier(
            LivingEntity entity, boolean active)
    {
        if (!active && !entity.getAttributes().hasModifier(Attributes.MAX_HEALTH, MAX_HEALTH_MODIFIER)) return;
        AttributeInstance attribute =
                entity.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null)
        {
            applyTransientModifier(
                    attribute,
                    MAX_HEALTH_MODIFIER,
                    "blessingofpandora:soul_sculpting_max_health",
                    0.2,
                    AttributeModifier.Operation.MULTIPLY_TOTAL,
                    active);
        }
    }

    private static void applyDamageTakenModifier(
            LivingEntity entity, boolean active)
    {
        if (!active && !entity.getAttributes().hasModifier(
                BlessingOfPandora.SOUL_DAMAGE_TAKEN.get(), DAMAGE_TAKEN_MODIFIER)) return;
        AttributeInstance attribute = entity.getAttribute(
                BlessingOfPandora.SOUL_DAMAGE_TAKEN.get());
        if (attribute != null)
        {
            applyTransientModifier(
                    attribute,
                    DAMAGE_TAKEN_MODIFIER,
                    "blessingofpandora:soul_sculpting_damage_taken",
                    0.2,
                    AttributeModifier.Operation.ADDITION,
                    active);
        }
    }

    private static void applyTransientModifier(
            AttributeInstance attribute,
            UUID modifierId,
            String name,
            double amount,
            AttributeModifier.Operation operation,
            boolean active)
    {
        AttributeModifier existing =
                attribute.getModifier(modifierId);
        if (!active)
        {
            if (existing != null)
            {
                attribute.removeModifier(modifierId);
            }
            return;
        }
        if (existing != null && existing.getAmount() == amount)
        {
            return;
        }
        if (existing != null)
        {
            attribute.removeModifier(modifierId);
        }
        attribute.addTransientModifier(
                new AttributeModifier(
                        modifierId,
                        name,
                        amount,
                        operation));
    }
}
