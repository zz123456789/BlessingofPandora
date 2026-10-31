package com.blessingofpandora.mixin;

import com.blessingofpandora.BlessingOfPandora;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends net.minecraft.world.inventory.ItemCombinerMenu
{
    protected AnvilMenuMixin(net.minecraft.world.inventory.MenuType<?> type, int id,
                             net.minecraft.world.entity.player.Inventory inventory,
                             net.minecraft.world.inventory.ContainerLevelAccess access) {
        super(type, id, inventory, access);
    }
    @Shadow @Final private DataSlot cost;
    @Inject(method = "createResult", at = @At("RETURN"))
    private void blessingofpandora$setCurseAdditionCost(CallbackInfo ci)
    {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack left = menu.getSlot(0).getItem();
        ItemStack result = menu.getSlot(2).getItem();
        if (left.isEmpty() || result.isEmpty())
        {
            return;
        }

        Enchantment pandoraCurse = BlessingOfPandora.PANDORA_CURSE.get();
        Map<Enchantment, Integer> before = EnchantmentHelper.getEnchantments(left);
        if (before.getOrDefault(pandoraCurse, 0) <= 0)
        {
            return;
        }

        Map<Enchantment, Integer> after = EnchantmentHelper.getEnchantments(result);
        boolean addedCurse = after.entrySet().stream().anyMatch(entry -> {
            Enchantment enchantment = entry.getKey();
            return enchantment != pandoraCurse
                    && enchantment.isCurse()
                    && entry.getValue() > before.getOrDefault(enchantment, 0);
        });
        if (addedCurse)
        {
            cost.set(1);
        }
    }
}
