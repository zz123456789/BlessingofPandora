package com.blessingofpandora.mixin;

import com.blessingofpandora.compat.love.EternalLoveCombat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stop the maid's own attack/death events before they count an attack or consume a charm. */
@Pseudo
@Mixin(targets = "com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid", remap = false)
public abstract class EternalLoveMaidCombatMixin
{
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true, remap = true)
    private void blessingofpandora$ignoreOwnerAttack(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir)
    {
        if (EternalLoveCombat.blocksAttack((EntityMaid)(Object)this, source)) cir.setReturnValue(false);
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true, remap = true)
    private void blessingofpandora$rescueBeforeMaidDeath(DamageSource source, CallbackInfo ci)
    {
        if (EternalLoveCombat.preventDeath((EntityMaid)(Object)this, source)) ci.cancel();
    }
}
