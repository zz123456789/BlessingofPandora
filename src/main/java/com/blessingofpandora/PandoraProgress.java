package com.blessingofpandora;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.HashSet;
import java.util.Set;

public final class PandoraProgress
{
    private static final String PROLOGUE_GRANTED = "blessingofpandora:prologue_granted";
    private static final String EPILOGUE_GRANTED = "blessingofpandora:epilogue_granted";

    private PandoraProgress()
    {
    }

    public static void onPlayerTick(Player player)
    {
        if (player.level().isClientSide)
        {
            return;
        }
        grantPrologueIfNeeded(player);
        grantEpilogueIfReady(player);
    }

    public static void onCurseConverted(Player player)
    {
        if (player.getPersistentData().getBoolean(EPILOGUE_GRANTED))
        {
            return;
        }

        AttributeInstance attribute =
                player.getAttribute(BlessingOfPandora.CURSED_TO_BLESSED.get());
        if (attribute == null)
        {
            return;
        }

        double value = attribute.getBaseValue();
        double next = Math.min(7.0, value + 1.0);
        attribute.setBaseValue(next);
        grantEpilogueIfReady(player);
    }

    private static void grantPrologueIfNeeded(Player player)
    {
        if (player.getPersistentData().getBoolean(PROLOGUE_GRANTED)
                || !hasAllCurses(player))
        {
            return;
        }

        player.getPersistentData().putBoolean(PROLOGUE_GRANTED, true);
        grant(player, PandoraBraceletItem.create(
                player.getUUID(), PandoraBraceletItem.CHAPTER_PROLOGUE));
    }

    private static void grantEpilogueIfReady(Player player)
    {
        if (player.getPersistentData().getBoolean(EPILOGUE_GRANTED))
        {
            return;
        }

        AttributeInstance attribute =
                player.getAttribute(BlessingOfPandora.CURSED_TO_BLESSED.get());
        if (attribute == null || attribute.getBaseValue() < 7.0)
        {
            return;
        }

        player.getPersistentData().putBoolean(EPILOGUE_GRANTED, true);
        grant(player, PandoraBraceletItem.create(
                player.getUUID(), PandoraBraceletItem.CHAPTER_EPILOGUE));
    }

    private static boolean hasAllCurses(Player player)
    {
        Set<Item> wornCurses = new HashSet<>();
        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            for (ICurioStacksHandler handler : inventory.getCurios().values())
            {
                IDynamicStackHandler stacks = handler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++)
                {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (!stack.isEmpty() && !BlessingNbt.isBlessing(stack))
                    {
                        wornCurses.add(stack.getItem());
                    }
                }
            }
        });

        for (BlessingDefinitions definition : BlessingDefinitions.values())
        {
            Item curse = definition.curseItem();
            if (curse == null || !wornCurses.contains(curse))
            {
                return false;
            }
        }
        return true;
    }

    private static void grant(Player player, ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return;
        }
        if (!player.getInventory().add(stack))
        {
            player.drop(stack, false);
        }
        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                        "blessingofpandora.message.bracelet_granted",
                        stack.getDisplayName()),
                true);
    }
}
