package com.blessingofpandora;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID)
public final class StrongAttributeHandler
{
    private static final UUID MAX_HEALTH_MODIFIER =
            UUID.fromString("b1e55b1e-4d52-4a54-9e1e-000000000001");

    private StrongAttributeHandler()
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

        var attributes = entity.getAttributes();
        var strong = BlessingOfPandora.STRONG.get();
        if (!attributes.hasAttribute(strong))
        {
            return;
        }

        double value = attributes.getValue(strong);
        if (value == 0.0 && !attributes.hasModifier(Attributes.MAX_HEALTH, MAX_HEALTH_MODIFIER)) return;

        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null)
        {
            return;
        }

        AttributeModifier existing = maxHealth.getModifier(MAX_HEALTH_MODIFIER);
        if (value == 0.0)
        {
            if (existing != null)
            {
                maxHealth.removeModifier(MAX_HEALTH_MODIFIER);
            }
            return;
        }

        if (existing != null && existing.getAmount() == value)
        {
            return;
        }
        if (existing != null)
        {
            maxHealth.removeModifier(MAX_HEALTH_MODIFIER);
        }
        maxHealth.addTransientModifier(new AttributeModifier(
                MAX_HEALTH_MODIFIER,
                "blessingofpandora:strong_max_health",
                value,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
}
