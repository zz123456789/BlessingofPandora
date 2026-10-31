package com.blessingofpandora;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

/** Player-owned pity counters; removing an item never removes earned progress. */
public final class KillReversalProgress
{
    private static final String PROGRESS = "blessingofpandora:kill_reversal_failures";

    private KillReversalProgress()
    {
    }

    public static long failures(Player player, BlessingDefinitions definition)
    {
        if (!isKillReversal(definition)) return 0L;
        return Math.max(0L, player.getPersistentData()
                .getCompound(Player.PERSISTED_NBT_TAG)
                .getCompound(PROGRESS).getLong(definition.id()));
    }

    public static double chancePercent(double base, double bonus, long failures)
    {
        double safeBase = Double.isFinite(base) ? Math.max(0.0, base) : 0.0;
        double safeBonus = Double.isFinite(bonus) ? Math.max(0.0, bonus) : 0.0;
        return Math.min(100.0, safeBase + safeBonus * Math.max(0L, failures));
    }

    public static long recordFailure(Player player, BlessingDefinitions definition)
    {
        if (!isKillReversal(definition)) return 0L;
        long current = failures(player, definition);
        long next = current == Long.MAX_VALUE ? current : current + 1L;
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag progress = persisted.getCompound(PROGRESS);
        progress.putLong(definition.id(), next);
        persisted.put(PROGRESS, progress);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
        return next;
    }

    public static boolean reset(Player player, BlessingDefinitions definition)
    {
        if (!isKillReversal(definition)) return false;
        CompoundTag progress = player.getPersistentData()
                .getCompound(Player.PERSISTED_NBT_TAG).getCompound(PROGRESS);
        boolean existed = progress.contains(definition.id());
        progress.remove(definition.id());
        return existed;
    }

    public static void copy(Player original, Player replacement)
    {
        CompoundTag source = original.getPersistentData()
                .getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag root = replacement.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (source.contains(PROGRESS, Tag.TAG_COMPOUND))
        {
            persisted.put(PROGRESS, source.getCompound(PROGRESS).copy());
        }
        else
        {
            persisted.remove(PROGRESS);
        }
        root.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    /** Called once for a completed kill matching this curse's configured target. */
    public static boolean tryConversion(Player player, BlessingDefinitions definition)
    {
        if (player.level().isClientSide || !isKillReversal(definition)
                || !ReversalPermission.isAllowed(player, definition)
                || !BlessingNbt.isWearingCurse(player, definition.curseItem()))
        {
            return false;
        }
        double base = definition == BlessingDefinitions.FREEDOM
                ? BlessingOfPandoraConfig.firstBaseChancePercent()
                : BlessingOfPandoraConfig.fifthBaseChancePercent();
        double bonus = definition == BlessingDefinitions.FREEDOM
                ? BlessingOfPandoraConfig.firstFailureBonusPercent()
                : BlessingOfPandoraConfig.fifthFailureBonusPercent();
        double chance = chancePercent(base, bonus, failures(player, definition));
        if (player.getRandom().nextDouble() * 100.0 < chance
                && ReversalActions.convertWornCurse(player, definition))
        {
            return true;
        }
        recordFailure(player, definition);
        BlessingReversals.syncReversalState(player);
        return false;
    }

    private static boolean isKillReversal(BlessingDefinitions definition)
    {
        return definition == BlessingDefinitions.FREEDOM
                || definition == BlessingDefinitions.FEARLESSNESS;
    }
}
