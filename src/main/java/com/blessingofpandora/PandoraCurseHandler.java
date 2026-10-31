package com.blessingofpandora;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID)
public final class PandoraCurseHandler
{
    private static final UUID MAX_HEALTH_MODIFIER =
            UUID.fromString("b1e55b1e-4d52-4a54-9e1e-000000000002");
    private static final double MAX_HEALTH_PER_ITEM = -2.0;

    private PandoraCurseHandler()
    {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide)
        {
            return;
        }

        Player player = event.player;
        int count = countPandoraCurseItems(player);
        applyMaxHealthModifier(player, count);
    }

    private static int countPandoraCurseItems(Player player)
    {
        int count = 0;
        if (hasPandoraCurse(player.getMainHandItem()))
        {
            count++;
        }
        if (hasPandoraCurse(player.getOffhandItem()))
        {
            count++;
        }
        for (ItemStack stack : player.getArmorSlots())
        {
            if (hasPandoraCurse(stack))
            {
                count++;
            }
        }

        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return count;
        for (ICurioStacksHandler handler : inventory.getCurios().values())
        {
            IDynamicStackHandler stacks = handler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++)
            {
                if (hasPandoraCurse(stacks.getStackInSlot(i)))
                {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean hasPandoraCurse(ItemStack stack)
    {
        return EnchantmentHelper.getItemEnchantmentLevel(
                BlessingOfPandora.PANDORA_CURSE.get(), stack) > 0;
    }

    private static void applyMaxHealthModifier(Player player, int count)
    {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null)
        {
            return;
        }

        double amount = MAX_HEALTH_PER_ITEM * count;
        AttributeModifier existing = maxHealth.getModifier(MAX_HEALTH_MODIFIER);
        if (existing != null && existing.getAmount() == amount)
        {
            return;
        }
        if (existing != null)
        {
            maxHealth.removeModifier(MAX_HEALTH_MODIFIER);
        }
        if (amount != 0.0)
        {
            maxHealth.addTransientModifier(new AttributeModifier(
                    MAX_HEALTH_MODIFIER,
                    "blessingofpandora:pandora_curse_max_health",
                    amount,
                    AttributeModifier.Operation.ADDITION));
        }
    }
}
