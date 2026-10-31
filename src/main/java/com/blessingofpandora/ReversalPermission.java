package com.blessingofpandora;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

public final class ReversalPermission
{
    private static final String REVERSAL_PERMISSIONS =
            "blessingofpandora:reversal_permissions";

    private ReversalPermission()
    {
    }

    public static boolean isAllowed(Player player, BlessingDefinitions definition)
    {
        CompoundTag tag = permissionData(player);
        return !tag.contains(definition.id()) || tag.getBoolean(definition.id());
    }

    public static boolean toggle(Player player, BlessingDefinitions definition)
    {
        boolean allowed = !isAllowed(player, definition);
        CompoundTag root = player.getPersistentData();
        CompoundTag tag = permissionData(player);
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        tag.putBoolean(definition.id(), allowed);
        persisted.put(REVERSAL_PERMISSIONS, tag);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
        return allowed;
    }

    public static boolean[] states(Player player)
    {
        CompoundTag tag = permissionData(player);
        BlessingDefinitions[] definitions = BlessingDefinitions.values();
        boolean[] states = new boolean[definitions.length];
        for (int i = 0; i < definitions.length; i++)
        {
            states[i] = !tag.contains(definitions[i].id())
                    || tag.getBoolean(definitions[i].id());
        }
        return states;
    }

    private static CompoundTag permissionData(Player player)
    {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (root.contains(REVERSAL_PERMISSIONS))
        {
            persisted.put(REVERSAL_PERMISSIONS,
                    root.getCompound(REVERSAL_PERMISSIONS));
            root.remove(REVERSAL_PERMISSIONS);
        }
        CompoundTag permissions = persisted.getCompound(REVERSAL_PERMISSIONS);
        if (!persisted.contains(REVERSAL_PERMISSIONS, Tag.TAG_COMPOUND))
        {
            persisted.put(REVERSAL_PERMISSIONS, permissions);
        }
        if (!root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND))
        {
            root.put(Player.PERSISTED_NBT_TAG, persisted);
        }
        return permissions;
    }
}
