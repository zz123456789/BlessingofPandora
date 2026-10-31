package com.blessingofpandora;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class PandoraProgress
{
    private static final String PROLOGUE_GRANTED = "blessingofpandora:prologue_granted";
    private static final String EPILOGUE_GRANTED = "blessingofpandora:epilogue_granted";
    private static final String COUNT_TAG = "blessingofpandora:cursed_to_blessed";
    private static final int ALL_CURSES = (1 << BlessingDefinitions.values().length) - 1;

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
        saveCount(player);
    }

    public static void onCurseConverted(Player player)
    {
        if (persistedData(player).getBoolean(EPILOGUE_GRANTED))
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
        saveCount(player);
    }

    public static void onCurseReverted(Player player)
    {
        if (persistedData(player).getBoolean(EPILOGUE_GRANTED))
        {
            return;
        }
        AttributeInstance attribute =
                player.getAttribute(BlessingOfPandora.CURSED_TO_BLESSED.get());
        if (attribute == null)
        {
            return;
        }
        attribute.setBaseValue(Math.max(0.0, attribute.getBaseValue() - 1.0));
        saveCount(player);
    }

    public static void restoreCount(Player player)
    {
        CompoundTag data = persistedData(player);
        AttributeInstance attribute = player.getAttribute(BlessingOfPandora.CURSED_TO_BLESSED.get());
        if (attribute == null) return;
        attribute.setBaseValue(restoredCount(data, attribute.getBaseValue()));
        saveCount(player);
    }

    static double restoredCount(CompoundTag data, double previous)
    {
        if (data.getBoolean(EPILOGUE_GRANTED)) return 7.0;
        double value = data.contains(COUNT_TAG) ? data.getDouble(COUNT_TAG) : previous;
        return Double.isFinite(value) ? Math.max(0.0, Math.min(7.0, value)) : 0.0;
    }

    public static void copyCount(Player original, Player replacement)
    {
        CompoundTag data = persistedData(replacement);
        AttributeInstance attribute = original.getAttribute(BlessingOfPandora.CURSED_TO_BLESSED.get());
        if (attribute != null) data.putDouble(COUNT_TAG, attribute.getBaseValue());
        restoreCount(replacement);
    }

    private static void saveCount(Player player)
    {
        AttributeInstance attribute = player.getAttribute(BlessingOfPandora.CURSED_TO_BLESSED.get());
        if (attribute == null) return;
        CompoundTag data = persistedData(player);
        if (data.getBoolean(EPILOGUE_GRANTED)) attribute.setBaseValue(7.0);
        double count = attribute.getBaseValue();
        if (!data.contains(COUNT_TAG, Tag.TAG_DOUBLE)
                || Double.compare(data.getDouble(COUNT_TAG), count) != 0)
        {
            data.putDouble(COUNT_TAG, count);
        }
    }

    private static void grantPrologueIfNeeded(Player player)
    {
        CompoundTag data = persistedData(player);
        if (data.getBoolean(PROLOGUE_GRANTED)
                || !hasAllCurses(player))
        {
            return;
        }

        data.putBoolean(PROLOGUE_GRANTED, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, data);
        grant(player, PandoraBraceletItem.create(
                player.getUUID(), PandoraBraceletItem.CHAPTER_PROLOGUE));
    }

    private static void grantEpilogueIfReady(Player player)
    {
        CompoundTag data = persistedData(player);
        if (data.getBoolean(EPILOGUE_GRANTED))
        {
            return;
        }

        AttributeInstance attribute =
                player.getAttribute(BlessingOfPandora.CURSED_TO_BLESSED.get());
        if (attribute == null || attribute.getBaseValue() < 7.0)
        {
            return;
        }

        data.putBoolean(EPILOGUE_GRANTED, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, data);
        grant(player, PandoraBraceletItem.create(
                player.getUUID(), PandoraBraceletItem.CHAPTER_EPILOGUE));
    }

    private static CompoundTag persistedData(Player player)
    {
        CompoundTag root = player.getPersistentData();
        CompoundTag data = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (root.contains(PROLOGUE_GRANTED))
        {
            data.putBoolean(PROLOGUE_GRANTED, root.getBoolean(PROLOGUE_GRANTED));
            root.remove(PROLOGUE_GRANTED);
        }
        if (root.contains(EPILOGUE_GRANTED))
        {
            data.putBoolean(EPILOGUE_GRANTED, root.getBoolean(EPILOGUE_GRANTED));
            root.remove(EPILOGUE_GRANTED);
        }
        if (!root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND))
        {
            root.put(Player.PERSISTED_NBT_TAG, data);
        }
        return data;
    }

    private static boolean hasAllCurses(Player player)
    {
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return false;
        int found = 0;
        for (ICurioStacksHandler handler : inventory.getCurios().values())
        {
            IDynamicStackHandler stacks = handler.getStacks();
            // Both handler and inherited slot reads can advance holder updates.
            // Preserve every read even after all seven kinds have been found.
            for (int i = 0; i < stacks.getSlots(); i++)
            {
                ItemStack stack = stacks.getStackInSlot(i);
                if (found == ALL_CURSES || stack.isEmpty() || BlessingNbt.isBlessing(stack)) continue;
                BlessingDefinitions definition = BlessingDefinitions.fromCurseItem(stack);
                if (definition != null)
                {
                    found |= 1 << definition.ordinal();
                }
            }
        }
        return found == ALL_CURSES;
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
    }
}
