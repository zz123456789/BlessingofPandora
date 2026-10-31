package com.blessingofpandora.mixin;

import com.blessingofpandora.compat.love.EternalLoveCombat;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AttributeInstance.class)
public abstract class EternalLoveArmorAttributeMixin
{
    @Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
    private void blessingofpandora$constantEternalArmor(CallbackInfoReturnable<Double> cir)
    {
        cir.setReturnValue(EternalLoveCombat.armorValue((AttributeInstance)(Object)this, cir.getReturnValue()));
    }
}
