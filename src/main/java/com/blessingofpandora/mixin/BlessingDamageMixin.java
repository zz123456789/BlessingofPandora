package com.blessingofpandora.mixin;

import com.blessingofpandora.BlessingBenefits;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ForgeHooks.class, remap = false)
public abstract class BlessingDamageMixin
{
    @Inject(method = "onLivingHurt", at = @At("RETURN"), cancellable = true)
    private static void blessingofpandora$positiveDamage(LivingEntity target, DamageSource source, float amount,
                                                        CallbackInfoReturnable<Float> cir)
    {
        cir.setReturnValue(BlessingBenefits.modifyDamage(target, source, cir.getReturnValue()));
    }
}
