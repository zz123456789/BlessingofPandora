package com.blessingofpandora;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class SoulSculptingEffect extends MobEffect
{
    public SoulSculptingEffect(MobEffectCategory category, int color)
    {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity livingEntity, int amplifier)
    {
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier)
    {
        return false;
    }
}
