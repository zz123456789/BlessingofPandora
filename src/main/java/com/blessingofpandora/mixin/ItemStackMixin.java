package com.blessingofpandora.mixin;

import com.blessingofpandora.BlessingNbt;
import com.blessingofpandora.BlessingOwners;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin
{
    @Inject(method = "getHoverName", at = @At("HEAD"), cancellable = true)
    private void blessingofpandora$scrambleWrongOwner(CallbackInfoReturnable<Component> cir)
    {
        ItemStack stack = (ItemStack) (Object) this;
        if (!BlessingNbt.isBlessing(stack) || Minecraft.getInstance().player == null)
        {
            return;
        }
        if (!BlessingOwners.isOwner(stack, Minecraft.getInstance().player.getUUID()))
        {
            cir.setReturnValue(Component.literal("????????????????????")
                    .withStyle(ChatFormatting.OBFUSCATED));
        }
    }
}
