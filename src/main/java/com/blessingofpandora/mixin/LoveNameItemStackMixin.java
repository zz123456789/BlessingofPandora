package com.blessingofpandora.mixin;

import com.blessingofpandora.LoveNameStyle;
import com.blessingofpandora.LoveOrbRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Public-side marker survives serialized names and cached chat without depending on client classes. */
@Mixin(value = ItemStack.class, priority = 250)
public abstract class LoveNameItemStackMixin
{
    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void blessingofpandora$flowingLoveName(CallbackInfoReturnable<Component> cir)
    {
        if (LoveOrbRegistration.isLoveFamily((ItemStack) (Object) this))
            cir.setReturnValue(LoveNameStyle.decorate(cir.getReturnValue()));
    }
}
