package com.blessingofpandora;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The seventh curse accepts every curse book, independent of equipment categories. */
@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID)
public final class SeventhCurseAnvil
{
    private SeventhCurseAnvil() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAnvilUpdate(AnvilUpdateEvent event)
    {
        ItemStack left = event.getLeft();
        if (left.isEmpty() || BlessingNbt.isBlessing(left)
                || BlessingDefinitions.LIBERATION.curseItem() == null
                || !left.is(BlessingDefinitions.LIBERATION.curseItem())
                || !event.getRight().is(Items.ENCHANTED_BOOK)) return;

        var enchantments = EnchantmentHelper.getEnchantments(left);
        boolean changed = false;
        for (var entry : EnchantmentHelper.getEnchantments(event.getRight()).entrySet())
        {
            var enchantment = entry.getKey();
            int incoming = entry.getValue();
            if (enchantment == null || !enchantment.isCurse() || incoming <= 0) continue;
            int old = enchantments.getOrDefault(enchantment, 0);
            int level = Math.min(enchantment.getMaxLevel(), old == incoming ? incoming + 1 : Math.max(old, incoming));
            if (level > old)
            {
                enchantments.put(enchantment, level);
                changed = true;
            }
        }
        if (!changed) return;
        ItemStack output = left.copy();
        EnchantmentHelper.setEnchantments(enchantments, output);
        String name = event.getName();
        if (name != null)
        {
            if (name.isBlank()) output.resetHoverName();
            else output.setHoverName(Component.literal(name));
        }
        event.setOutput(output);
        event.setCost(1);
        event.setMaterialCost(1);
    }
}
