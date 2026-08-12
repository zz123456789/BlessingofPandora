package com.blessingofpandora.mixin;

import dev.xkmc.pandora.content.core.InheritedItemStackHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = InheritedItemStackHandler.class, remap = false)
public abstract class PandoraInheritedStackHandlerMixin
{
    @Redirect(
            method = "getStackInSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/items/IItemHandlerModifiable;getStackInSlot(I)Lnet/minecraft/world/item/ItemStack;"),
            remap = false)
    private ItemStack blessingofpandora$guardUnderlyingSlot(
            IItemHandlerModifiable handler, int localSlot)
    {
        // Pandora API 在持有物收缩时会短暂访问旧组合索引，这里只做越界兜底。
        if (localSlot < 0 || localSlot >= handler.getSlots())
        {
            return ItemStack.EMPTY;
        }
        return handler.getStackInSlot(localSlot);
    }
}
