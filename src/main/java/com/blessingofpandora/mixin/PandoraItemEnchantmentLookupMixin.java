package com.blessingofpandora.mixin;

import com.blessingofpandora.BlessingOfPandora;
import com.blessingofpandora.PandoraCurseEnchantment;
import com.blessingofpandora.PandoraCurseHandler;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.common.extensions.IForgeItem;
import org.spongepowered.asm.mixin.Mixin;

/** Only accelerate Item's inherited default; a custom item's override still wins virtual dispatch. */
@Mixin(Item.class)
public abstract class PandoraItemEnchantmentLookupMixin implements IForgeItem
{
    @Override
    public int getEnchantmentLevel(ItemStack stack, Enchantment enchantment)
    {
        if (enchantment instanceof PandoraCurseEnchantment && BlessingOfPandora.PANDORA_CURSE.isPresent()
                && enchantment == BlessingOfPandora.PANDORA_CURSE.get())
            return PandoraCurseHandler.storedEnchantmentLevel(stack);
        return IForgeItem.super.getEnchantmentLevel(stack, enchantment);
    }
}
