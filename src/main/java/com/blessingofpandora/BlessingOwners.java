package com.blessingofpandora;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class BlessingOwners
{
    private static final String MAID_CLASS =
            "com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid";

    private BlessingOwners()
    {
    }

    public static String ownerId(ItemStack stack)
    {
        if (stack.hasTag() && stack.getTag().contains(BlessingNbt.OWNER_TAG))
        {
            return stack.getTag().getString(BlessingNbt.OWNER_TAG);
        }
        return "";
    }

    public static boolean isOwner(ItemStack stack, UUID uuid)
    {
        String owner = ownerId(stack);
        return owner.isEmpty() || uuid != null && owner.equals(uuid.toString());
    }

    public static boolean isUsableBy(ItemStack stack, LivingEntity wearer)
    {
        if (!BlessingNbt.isBlessing(stack))
        {
            return false;
        }
        String owner = ownerId(stack);
        if (owner.isEmpty())
        {
            return true;
        }
        UUID effectiveOwner = effectiveOwnerUuid(wearer);
        return effectiveOwner != null && owner.equals(effectiveOwner.toString());
    }

    public static UUID effectiveOwnerUuid(LivingEntity wearer)
    {
        if (wearer instanceof Player player)
        {
            return player.getUUID();
        }
        if (wearer instanceof TamableAnimal tamed)
        {
            return tamed.getOwnerUUID();
        }
        return null;
    }

    public static boolean isMaid(LivingEntity wearer)
    {
        return MAID_CLASS.equals(wearer.getClass().getName());
    }
}
