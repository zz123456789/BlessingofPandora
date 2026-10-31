package com.blessingofpandora;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

/** Active equipment only, with a saturation limit that also prevents integer overflow. */
public final class BlessingEnchantmentLevels
{
    public static final int MAX_USEFUL_LEVELS = 400;

    private BlessingEnchantmentLevels() {}

    public static int total(LivingEntity wearer)
    {
        int levels = com.blessingofpandora.compat.love.LoveSharing.enchantmentLevels(wearer);
        if (levels >= MAX_USEFUL_LEVELS) return MAX_USEFUL_LEVELS;
        for (ItemStack stack : wearer.getAllSlots())
        {
            levels += count(stack, MAX_USEFUL_LEVELS - levels);
            if (levels == MAX_USEFUL_LEVELS) return levels;
        }
        var inventory = CuriosApi.getCuriosInventory(wearer).orElse(null);
        if (inventory == null) return levels;
        for (var handler : inventory.getCurios().values())
        {
            var stacks = handler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++)
            {
                // Pandora's inherited stacks already include the contents of worn holders.
                levels += count(stacks.getStackInSlot(i), MAX_USEFUL_LEVELS - levels);
                if (levels == MAX_USEFUL_LEVELS) return levels;
            }
        }
        return levels;
    }

    private static int count(ItemStack stack, int remaining)
    {
        if (stack.isEmpty() || remaining <= 0) return 0;
        int count = 0;
        // Forge's map includes active item-provided enchantments and deduplicates each enchantment.
        // Do not re-add raw NBT: disabled enchantments and the fake blessing glint must not count.
        for (var entry : stack.getAllEnchantments().entrySet())
        {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0
                    || ForgeRegistries.ENCHANTMENTS.getKey(entry.getKey()) == null) continue;
            count += Math.min(entry.getValue(), remaining - count);
            if (count == remaining) break;
        }
        return count;
    }
}
