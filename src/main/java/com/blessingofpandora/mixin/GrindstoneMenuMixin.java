package com.blessingofpandora.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin
{
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("RETURN"))
    private void blessingofpandora$allowBooks(
            int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci)
    {
        GrindstoneMenu menu = (GrindstoneMenu) (Object) this;
        for (int i = 0; i < 2; i++)
        {
            Slot original = menu.getSlot(i);
            Slot input = new Slot(original.container, original.getContainerSlot(), original.x, original.y)
            {
                @Override
                public boolean mayPlace(ItemStack stack)
                {
                    return stack.is(Items.BOOK) || original.mayPlace(stack);
                }
            };
            input.index = original.index;
            menu.slots.set(i, input);
        }
    }
}
