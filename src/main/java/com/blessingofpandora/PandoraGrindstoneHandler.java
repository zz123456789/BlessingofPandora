package com.blessingofpandora;

import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.event.GrindstoneEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID)
public final class PandoraGrindstoneHandler
{
    private PandoraGrindstoneHandler()
    {
    }

    @SubscribeEvent
    public static void onPlaceItem(GrindstoneEvent.OnPlaceItem event)
    {
        int equipmentSlot = equipmentSlot(event.getTopItem(), event.getBottomItem());
        if (equipmentSlot < 0)
        {
            return;
        }
        event.setOutput(EnchantedBookItem.createForEnchantment(
                new EnchantmentInstance(BlessingOfPandora.PANDORA_CURSE.get(), 1)));
        // This recipe transfers the curse; it does not also award disenchanting XP.
        event.setXp(0);
    }

    @SubscribeEvent
    public static void onTakeItem(GrindstoneEvent.OnTakeItem event)
    {
        int equipmentSlot = equipmentSlot(event.getTopItem(), event.getBottomItem());
        if (equipmentSlot < 0)
        {
            return;
        }
        ItemStack equipment = (equipmentSlot == 0
                ? event.getTopItem() : event.getBottomItem()).copy();
        ItemStack books = (equipmentSlot == 0
                ? event.getBottomItem() : event.getTopItem()).copy();
        equipment.removeTagKey("Enchantments");
        equipment.removeTagKey("StoredEnchantments");
        equipment.setRepairCost(0);
        books.shrink(1);
        event.setNewTopItem(equipmentSlot == 0 ? equipment : books);
        event.setNewBottomItem(equipmentSlot == 0 ? books : equipment);
        event.setXp(0);
    }

    private static int equipmentSlot(ItemStack top, ItemStack bottom)
    {
        if (bottom.is(Items.BOOK) && isCursedEquipment(top))
        {
            return 0;
        }
        if (top.is(Items.BOOK) && isCursedEquipment(bottom))
        {
            return 1;
        }
        return -1;
    }

    private static boolean isCursedEquipment(ItemStack stack)
    {
        // Only extract a physically stored curse, so an item's permanent/dynamic
        // enchantment cannot be used to produce an unlimited supply of books.
        return !stack.isEmpty() && stack.getCount() == 1
                && !stack.is(Items.BOOK) && !stack.is(Items.ENCHANTED_BOOK)
                && EnchantmentHelper.getTagEnchantmentLevel(
                        BlessingOfPandora.PANDORA_CURSE.get(), stack) > 0;
    }
}
