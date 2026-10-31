package com.blessingofpandora.mixin;

import com.blessingofpandora.BlessingNames;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackBlessingNameMixin
{
    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void blessingofpandora$restoreBlessingDefaultName(CallbackInfoReturnable<Component> cir)
    {
        Component original = cir.getReturnValue();
        Component resolved = BlessingNames.resolveDefaultName((ItemStack) (Object) this, original);
        if (resolved != original) cir.setReturnValue(resolved);
    }
}
