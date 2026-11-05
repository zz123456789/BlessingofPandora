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
    // @Pseudo targets are not remapped by Mixin's annotation processor. Name both
    // the development and Forge 1.20.1 runtime methods explicitly, including their
    // descriptors. The required injection still fails loudly if neither exists.
    @Inject(method = {"hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            "m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"},
            at = @At("HEAD"), cancellable = true, remap = false)
    private void blessingofpandora$ignoreOwnerAttack(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir)
    {
        if (EternalLoveCombat.blocksAttack((EntityMaid)(Object)this, source)) cir.setReturnValue(false);
    }

    @Inject(method = {"die(Lnet/minecraft/world/damagesource/DamageSource;)V",
            "m_6667_(Lnet/minecraft/world/damagesource/DamageSource;)V"},
            at = @At("HEAD"), cancellable = true, remap = false)
    private void blessingofpandora$rescueBeforeMaidDeath(DamageSource source, CallbackInfo ci)
    {
        if (EternalLoveCombat.preventDeath((EntityMaid)(Object)this, source)) ci.cancel();
    }
}
