package com.blessingofpandora;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.SlotAttribute;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;

public final class BlessingAttributes
{
    private static final ResourceLocation REALITY_INDEX =
            ResourceLocation.parse("curseofpandora:reality_index");

    private BlessingAttributes()
    {
    }

    public static Multimap<Attribute, AttributeModifier> get(
            BlessingDefinitions definition, SlotContext slotContext, UUID uuid, ItemStack stack)
    {
        switch (definition)
        {
            case FREEDOM:
                return slotAndReality(
                        uuid, "blessing_of_freedom_reality",
                        "necklace", "blessing_of_freedom_necklace", 1.0);
            case INFINITY:
                return slotAndReality(
                        uuid, "blessing_of_infinity_reality",
                        "bracelet", "blessing_of_infinity_bracelet", 1.0);
            case SPIRIT:
                return luckAndReality(uuid);
            case STRENGTH:
                return strongAndReality(uuid);
            case FEARLESSNESS:
                return realityOnly(
                        uuid, "blessing_of_fearlessness_reality", 2.0);
            case REQUIEM:
                return slotAndReality(
                        uuid, "blessing_of_requiem_reality",
                        "charm", "blessing_of_requiem_charm", 2.0);
            case LIBERATION:
                return slotAndReality(
                        uuid, "blessing_of_liberation_reality",
                        "hands", "blessing_of_liberation_hands", 1.0);
            default:
                return HashMultimap.create();
        }
    }

    private static Multimap<Attribute, AttributeModifier> slotAndReality(
            UUID uuid, String realityName,
            String slotId, String slotName, double slotAmount)
    {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        addReality(modifiers, uuid, realityName);
        modifiers.put(SlotAttribute.getOrCreate(slotId), new AttributeModifier(
                uuid, slotName, slotAmount, AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    private static Multimap<Attribute, AttributeModifier> luckAndReality(UUID uuid)
    {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        addReality(modifiers, uuid, "blessing_of_spirit_reality");
        modifiers.put(Attributes.LUCK, new AttributeModifier(
                uuid, "blessing_of_spirit_luck", 10.0, AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    private static Multimap<Attribute, AttributeModifier> strongAndReality(UUID uuid)
    {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        addReality(modifiers, uuid, "blessing_of_strength_reality");
        modifiers.put(BlessingOfPandora.STRONG.get(), new AttributeModifier(
                uuid, "blessing_of_strength_strong", 0.5, AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    private static Multimap<Attribute, AttributeModifier> realityOnly(
            UUID uuid, String realityName, double amount)
    {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        addReality(modifiers, uuid, realityName, amount);
        return modifiers;
    }

    private static void addReality(
            Multimap<Attribute, AttributeModifier> modifiers, UUID uuid, String name)
    {
        addReality(modifiers, uuid, name, 1.0);
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
    }
}
