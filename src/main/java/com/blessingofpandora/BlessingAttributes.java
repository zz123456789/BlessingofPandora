package com.blessingofpandora;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;

public final class BlessingAttributes
{
    private static final ResourceLocation REALITY_INDEX =
            ResourceLocation.parse("curseofpandora:reality_index");
    private static final ResourceLocation SPELL_TOLERANCE =
            ResourceLocation.parse("curseofpandora:spell_tolerance");

    private BlessingAttributes()
    {
    }

    public static Multimap<Attribute, AttributeModifier> get(
            BlessingDefinitions definition, SlotContext slotContext, UUID uuid, ItemStack stack)
    {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        addReality(modifiers, uuid, definition.id() + "_reality",
                definition == BlessingDefinitions.FEARLESSNESS ? 2.0 : 1.0);
        // Extra slots have a stable equipment lifecycle in BlessingSlots. Returning a
        // SlotAttribute here would revoke them whenever Curios observes any NBT change.
        return modifiers;
    }

    private static void addReality(
            Multimap<Attribute, AttributeModifier> modifiers,
            UUID uuid, String name, double amount)
    {
        Attribute reality = ForgeRegistries.ATTRIBUTES.getValue(REALITY_INDEX);
        if (reality != null)
        {
            modifiers.put(reality, new AttributeModifier(
                    uuid, name, amount, AttributeModifier.Operation.ADDITION));
        }
        Attribute spellTolerance = ForgeRegistries.ATTRIBUTES.getValue(SPELL_TOLERANCE);
        if (spellTolerance != null)
        {
            modifiers.put(spellTolerance, new AttributeModifier(
                    uuid, name + "_spell_tolerance", 1.0, AttributeModifier.Operation.ADDITION));
        }
    }
}
