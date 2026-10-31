package com.blessingofpandora.mixin;

import com.blessingofpandora.BlessingBenefitMath;
import com.blessingofpandora.BlessingBenefits;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AttributeInstance.class)
public abstract class BlessingMovementAttributeMixin
{
    @Inject(method = "calculateValue", at = @At("HEAD"), cancellable = true)
    private void blessingofpandora$ignoreMovementPenalties(CallbackInfoReturnable<Double> cir)
    {
        AttributeInstance attribute = (AttributeInstance) (Object) this;
        if (attribute.getAttribute() == Attributes.MOVEMENT_SPEED
                && attribute.getModifier(BlessingBenefits.IGNORE_SLOWNESS) != null)
        {
            // Read raw modifiers only: no getValue(), entity, equipment or capability lookup here.
            // Filtering also handles -100% reductions without division by zero or deleting modifiers.
            cir.setReturnValue(BlessingBenefitMath.valueWithout(attribute, null, true));
        }
    }
}
