package com.blessingofpandora;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class PandoraCurseEnchantment extends Enchantment
{
    public static final EnchantmentCategory ALL_ITEMS = EnchantmentCategory.create(
            "blessingofpandora_any", item -> true);

    public PandoraCurseEnchantment(Rarity rarity, EquipmentSlot[] slots)
    {
        super(rarity, ALL_ITEMS, slots);
    }

    @Override
    public int getMinCost(int level)
    {
        return 25;
    }

    @Override
    public int getMaxCost(int level)
    {
        return 50;
    }

    @Override
    public int getMaxLevel()
    {
        return 1;
    }

    @Override
    public boolean isTreasureOnly()
    {
        return true;
    }

    @Override
    public boolean isCurse()
    {
        return true;
    }

}
