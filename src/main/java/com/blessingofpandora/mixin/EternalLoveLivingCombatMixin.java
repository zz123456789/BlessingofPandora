package com.blessingofpandora.mixin;

import com.blessingofpandora.compat.love.EternalLoveCombat;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class EternalLoveLivingCombatMixin
{
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void blessingofpandora$ignoreBoundAttack(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir)
    {
        if (EternalLoveCombat.blocksAttack((LivingEntity)(Object)this, source)) cir.setReturnValue(false);
    }

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void blessingofpandora$rescueBeforeTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir)
    {
        if (EternalLoveCombat.preventDeath((LivingEntity)(Object)this, source)) cir.setReturnValue(true);
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void blessingofpandora$rescueBeforeDeath(DamageSource source, CallbackInfo ci)
    {
        if (EternalLoveCombat.preventDeath((LivingEntity)(Object)this, source)) ci.cancel();
    }

    @Inject(method = "getArmorValue", at = @At("RETURN"), cancellable = true)
    private void blessingofpandora$constantEternalArmor(CallbackInfoReturnable<Integer> cir)
    {
        cir.setReturnValue(EternalLoveCombat.armorValue((LivingEntity)(Object)this, cir.getReturnValue()));
    }
}
