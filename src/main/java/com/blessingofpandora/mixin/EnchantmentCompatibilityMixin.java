package com.blessingofpandora.mixin;

import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class EnchantmentCompatibilityMixin
{
    @Inject(method = "isCompatibleWith", at = @At("HEAD"), cancellable = true)
    private void blessingofpandora$allowCurseCombinations(
            Enchantment other, CallbackInfoReturnable<Boolean> cir)
    {
        Enchantment current = (Enchantment) (Object) this;
        if (current != other && current.isCurse() && other.isCurse())
        {
            cir.setReturnValue(true);
        }
    }
}
