package com.blessingofpandora;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/** Uses Curse of Pandora's one-time advancement reward and its existing delivery handling. */
public final class StartingGiftLootModifier extends LootModifier
{
    public static final Codec<StartingGiftLootModifier> CODEC = RecordCodecBuilder.create(
            instance -> codecStart(instance).apply(instance, StartingGiftLootModifier::new));

    public StartingGiftLootModifier(LootItemCondition[] conditions)
    {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context)
    {
        boolean necklace = BlessingOfPandoraConfig.GIVE_STARTING_NECKLACE.get();
        boolean curses = BlessingOfPandoraConfig.GIVE_STARTING_CURSES.get();
        if (necklace && curses) return generatedLoot;

        ObjectArrayList<ItemStack> result = new ObjectArrayList<>();
        for (ItemStack stack : generatedLoot)
        {
            if (!PandoraItemUtil.isPandoraNecklace(stack))
            {
                result.add(stack);
                continue;
            }
            for (CompoundTag gift : StartingGiftContents.configure(stack.save(new CompoundTag()), necklace, curses))
            {
                ItemStack item = ItemStack.of(gift);
                if (!item.isEmpty()) result.add(item);
            }
        }
        return result;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec()
    {
        return CODEC;
    }
}
