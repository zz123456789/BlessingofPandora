package com.blessingofpandora;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class PandoraItemUtil
{
    private static final ResourceLocation PANDORA_NECKLACE_ID =
            ResourceLocation.parse("pandora:pandora_necklace");
    private static final ResourceLocation PANDORA_BRACELET_ID =
            ResourceLocation.parse("pandora:pandora_bracelet");

    private PandoraItemUtil()
    {
    }

    public static boolean isPandoraNecklace(ItemStack stack)
    {
        return stack.is(item(PANDORA_NECKLACE_ID));
    }

    public static boolean isPandoraBracelet(ItemStack stack)
    {
        return stack.is(item(PANDORA_BRACELET_ID));
    }

    public static boolean isPandoraHolder(ItemStack stack)
    {
        return isPandoraNecklace(stack) || isPandoraBracelet(stack);
    }

    public static boolean isWearingPandoraHolder(LivingEntity entity)
    {
        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> {
                    for (ICurioStacksHandler handler : inventory.getCurios().values())
                    {
                        IDynamicStackHandler stacks = handler.getStacks();
                        for (int i = 0; i < stacks.getSlots(); i++)
                        {
                            if (isPandoraHolder(stacks.getStackInSlot(i)))
                            {
                                return true;
                            }
                        }
                    }
                    return false;
                })
                .orElse(false);
    }

    private static Item item(ResourceLocation id)
    {
        return ForgeRegistries.ITEMS.getValue(id);
    }
}
