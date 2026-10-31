package com.blessingofpandora;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Contains no maid API types so it is safe to load without the optional mod. */
public final class LoveOrbRegistration
{
    public static final String MAID_MOD_ID = "touhou_little_maid";
    private static RegistryObject<Item> loveOrb;
    private static RegistryObject<Item> eternalLoveOrb;

    private LoveOrbRegistration() {}

    /** Invoke before the item DeferredRegister is attached to the mod bus. */
    public static void register(DeferredRegister<Item> items)
    {
        if (loveOrb == null && ModList.get().isLoaded(MAID_MOD_ID))
        {
            loveOrb = items.register("love_orb", () -> new LoveOrbItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
            eternalLoveOrb = items.register("eternal_love_orb", () -> new LoveOrbItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
        }
    }

    /** The only entry point that loads classes referring to the optional maid API. */
    public static void initCompat()
    {
        if (isAvailable() && ModList.get().isLoaded(MAID_MOD_ID))
        {
            com.blessingofpandora.compat.maid.LoveMaidContract.register();
            com.blessingofpandora.compat.love.LoveProgressEvents.register();
            com.blessingofpandora.compat.love.EternalLoveEffects.register();
        }
    }

    public static boolean isAvailable()
    {
        return loveOrb != null && loveOrb.isPresent();
    }

    public static ItemStack createStack()
    {
        return isAvailable() ? new ItemStack(loveOrb.get()) : ItemStack.EMPTY;
    }

    public static boolean isLoveOrb(ItemStack stack)
    {
        return isAvailable() && !stack.isEmpty() && stack.is(loveOrb.get());
    }

    public static boolean isEternal(ItemStack stack)
    {
        return eternalLoveOrb != null && eternalLoveOrb.isPresent() && stack.is(eternalLoveOrb.get());
    }
    public static boolean isLoveFamily(ItemStack stack) { return isLoveOrb(stack) || isEternal(stack); }
    public static ItemStack createEternalStack()
    {
        return eternalLoveOrb != null && eternalLoveOrb.isPresent() ? new ItemStack(eternalLoveOrb.get()) : ItemStack.EMPTY;
    }
}
