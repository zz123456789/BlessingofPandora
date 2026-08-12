package com.blessingofpandora;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

public class BlessingCurio implements ICurioItem
{
    private final Item curseItem;
    private final BlessingDefinitions definition;
    private final ICurioItem originalCurio;

    public BlessingCurio(Item curseItem, BlessingDefinitions definition)
    {
        this.curseItem = curseItem;
        this.definition = definition;
        this.originalCurio = curseItem instanceof ICurioItem ? (ICurioItem) curseItem : null;
    }

    public static void registerAll()
    {
        for (BlessingDefinitions definition : BlessingDefinitions.values())
        {
            Item curse = definition.curseItem();
            if (curse != null)
            {
                CuriosApi.registerCurio(curse, new BlessingCurio(curse, definition));
            }
        }
    }

    @Override
    public boolean hasCurioCapability(ItemStack stack)
    {
        return true;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack)
    {
        if (!BlessingNbt.isBlessing(stack) && originalCurio != null)
        {
            originalCurio.curioTick(slotContext, stack);
        }
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, UUID uuid, ItemStack stack)
    {
        if (BlessingNbt.isBlessing(stack))
        {
            LivingEntity wearer = slotContext.entity();
            if (wearer == null || BlessingOwners.isUsableBy(stack, wearer))
            {
                return BlessingAttributes.get(definition, slotContext, uuid, stack);
            }
            return HashMultimap.create();
        }
        if (originalCurio != null)
        {
            return originalCurio.getAttributeModifiers(slotContext, uuid, stack);
        }
        return HashMultimap.create();
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack)
    {
        if (BlessingNbt.isBlessing(stack))
        {
            return true;
        }
        return originalCurio == null || originalCurio.canEquip(slotContext, stack);
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack)
    {
        return originalCurio == null || originalCurio.canUnequip(slotContext, stack);
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack)
    {
        if (!BlessingNbt.isBlessing(stack) && originalCurio != null)
        {
            originalCurio.onEquip(slotContext, prevStack, stack);
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack)
    {
        if (!BlessingNbt.isBlessing(stack) && originalCurio != null)
        {
            originalCurio.onUnequip(slotContext, newStack, stack);
        }
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, ItemStack stack)
    {
        if (BlessingNbt.isBlessing(stack))
        {
            return BlessingNbt.withoutBlankLines(tooltips);
        }
        if (originalCurio != null)
        {
            return originalCurio.getSlotsTooltip(tooltips, stack);
        }
        return ICurioItem.super.getSlotsTooltip(tooltips, stack);
    }

    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltips, ItemStack stack)
    {
        if (BlessingNbt.isBlessing(stack))
        {
            return List.of();
        }
        if (originalCurio != null)
        {
            return originalCurio.getAttributesTooltip(tooltips, stack);
        }
        return ICurioItem.super.getAttributesTooltip(tooltips, stack);
    }
}
