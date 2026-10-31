package com.blessingofpandora.mixin;

import com.blessingofpandora.PandoraBraceletItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackRarityMixin
{
    @Inject(method = "getRarity", at = @At("HEAD"), cancellable = true)
    private void blessingofpandora$giftedBraceletRarity(CallbackInfoReturnable<Rarity> cir)
    {
        ItemStack stack = (ItemStack) (Object) this;
        if (PandoraBraceletItem.isGiftedBracelet(stack))
        {
            cir.setReturnValue(Rarity.EPIC);
        }
    }
}
