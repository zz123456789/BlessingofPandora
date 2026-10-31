package com.blessingofpandora;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public final class SixthCurseProgress
{
    public static final int NOT_WEARING = 0;
    public static final int WEARING_CURSE = 1;
    public static final int WEARING_COMPLETED_BLESSING = 2;
    public static final int WEARING_BLESSING = 3;

    private static final String COMPLETED_TAG =
            "blessingofpandora:requiem_completed";
    private static final String PROGRESS = "blessingofpandora:requiem_progress";
    private static final String LAST_STATE = "blessingofpandora:requiem_last_state";

    private SixthCurseProgress()
    {
    }

    public static long get(Player player)
    {
        return getStored(player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG));
    }

    public static long add(Player player, long amount)
    {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        long value = addStored(persisted, amount, BlessingOfPandoraConfig.sixthExperience());
        root.put(Player.PERSISTED_NBT_TAG, persisted);
        return value;
    }

    public static boolean reset(Player player)
    {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        boolean existed = persisted.contains(PROGRESS);
        persisted.remove(PROGRESS);
        return existed;
    }

    static long getStored(CompoundTag persisted)
    {
        return Math.max(0L, persisted.getLong(PROGRESS));
    }

    static long addStored(CompoundTag persisted, long amount, long target)
    {
        long current = Math.min(target, getStored(persisted));
        long value = current + Math.min(Math.max(0L, amount), target - current);
        persisted.putLong(PROGRESS, value);
        return value;
    }

    public static void markCompleted(Player player)
    {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putBoolean(COMPLETED_TAG, true);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    public static boolean isCompleted(Player player)
    {
        return player.getPersistentData()
                .getCompound(Player.PERSISTED_NBT_TAG)
                .getBoolean(COMPLETED_TAG);
    }

    public static int state(Player player)
    {
        Item curse = BlessingDefinitions.REQUIEM.curseItem();
        if (curse != null && BlessingNbt.isWearingCurse(player, curse))
        {
            return WEARING_CURSE;
        }
        // Re-read through Curios: inherited holders can schedule another update
        // from the first scan's resize or equip callbacks.
        if (BlessingNbt.isWearingBlessing(player, BlessingDefinitions.REQUIEM))
        {
            return isCompleted(player)
                    ? WEARING_COMPLETED_BLESSING
                    : WEARING_BLESSING;
        }
        return NOT_WEARING;
    }

    public static boolean stateChanged(Player player)
    {
        int current = state(player);
        CompoundTag root = player.getPersistentData();
        boolean changed = !root.contains(LAST_STATE) || root.getInt(LAST_STATE) != current;
        if (changed || !root.contains(LAST_STATE, Tag.TAG_INT)) root.putInt(LAST_STATE, current);
        return changed;
    }

    public static long displayValue(Player player)
    {
        if (state(player) == WEARING_COMPLETED_BLESSING)
        {
            return BlessingOfPandoraConfig.sixthExperience();
        }
        return get(player);
    }
}
