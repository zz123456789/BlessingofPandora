package com.blessingofpandora;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public final class CurseEnchantments
{
    private CurseEnchantments()
    {
    }

    /** Remove only registered curse entries, preserving ordinary enchantments and unrelated NBT. */
    public static void removeStoredCurses(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;
        for (String key : new String[]{"Enchantments", "StoredEnchantments"})
        {
            if (!tag.contains(key, Tag.TAG_LIST)) continue;
            ListTag entries = tag.getList(key, Tag.TAG_COMPOUND);
            for (int i = entries.size() - 1; i >= 0; i--)
            {
                ResourceLocation id = ResourceLocation.tryParse(entries.getCompound(i).getString("id"));
                Enchantment enchantment = id == null ? null : ForgeRegistries.ENCHANTMENTS.getValue(id);
                if (enchantment != null && enchantment.isCurse()) entries.remove(i);
            }
            if (entries.isEmpty()) tag.remove(key);
        }
    }

    public static long count(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return 0;
        }
        CurseCollector curses = new CurseCollector();
        // Forge also exposes enchantments supplied by items rather than stored in NBT.
        stack.getAllEnchantments().forEach(curses);
        // Count curses physically on the item even when an effective-enchantment
        // hook suppresses their effects. Do not modify the stack or its enchantments.
        CompoundTag tag = stack.getTag();
        if (tag != null)
        {
            if (tag.contains("Enchantments", Tag.TAG_LIST))
            {
                collectStored(tag.getList("Enchantments", Tag.TAG_COMPOUND), curses);
            }
            if (tag.contains("StoredEnchantments", Tag.TAG_LIST))
            {
                collectStored(tag.getList("StoredEnchantments", Tag.TAG_COMPOUND), curses);
            }
        }
        return curses.size();
    }

    private static void collectStored(ListTag entries, CurseCollector curses)
    {
        for (int i = 0; i < entries.size(); i++)
        {
            CompoundTag entry = entries.getCompound(i);
            if (entry.getInt("lvl") <= 0) continue;
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("id"));
            if (id == null)
            {
                continue;
            }
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
            if (enchantment != null && enchantment.isCurse())
            {
                curses.add(enchantment);
            }
        }
    }

    private static final class CurseCollector implements BiConsumer<Enchantment, Integer>
    {
        private Set<Enchantment> curses;

        @Override
        public void accept(Enchantment enchantment, Integer level)
        {
            if (enchantment != null && level != null && level > 0 && enchantment.isCurse())
            {
                add(enchantment);
            }
        }

        private void add(Enchantment enchantment)
        {
            if (curses == null) curses = new HashSet<>();
            curses.add(enchantment);
        }

        private int size()
        {
            return curses == null ? 0 : curses.size();
        }
    }
}
