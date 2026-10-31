package com.blessingofpandora.mixin;

import com.blessingofpandora.BlessingBenefits;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class BlessingLivingDamageMixin
{
    // L2 Damage Tracker moves LivingDamageEvent before absorption. Resolve the remaining
    // health damage here, before both combat tracking and vanilla's health subtraction.
    @ModifyVariable(method = "actuallyHurt", ordinal = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getCombatTracker()Lnet/minecraft/world/damagesource/CombatTracker;"))
    private float blessingofpandora$healthDamage(float amount, DamageSource source, float originalAmount)
    {
        return BlessingBenefits.finalizeDamage((LivingEntity) (Object) this, source, amount);
    }
}
