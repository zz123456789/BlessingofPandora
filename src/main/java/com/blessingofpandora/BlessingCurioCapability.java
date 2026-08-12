package com.blessingofpandora;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID)
public final class BlessingCurioCapability
{
    private BlessingCurioCapability()
    {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attach(AttachCapabilitiesEvent<ItemStack> event)
    {
        ItemStack stack = event.getObject();
        if (!isCurseItem(stack))
        {
            return;
        }
        event.addCapability(
                ResourceLocation.parse(BlessingOfPandora.MODID + ":blessing_curio"),
                new Provider(stack));
    }

    private static boolean isCurseItem(ItemStack stack)
    {
        for (BlessingDefinitions definition : BlessingDefinitions.values())
        {
            Item curse = definition.curseItem();
            if (curse != null && stack.is(curse))
            {
                return true;
            }
        }
        return false;
    }

    private static class Provider implements ICapabilityProvider
    {
        private final ItemStack stack;

        Provider(ItemStack stack)
        {
            this.stack = stack;
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side)
        {
            if (capability == CuriosCapability.ITEM)
            {
                BlessingDefinitions definition = BlessingNbt.getBlessing(stack);
                if (definition != null)
                {
                    return CuriosCapability.ITEM.orEmpty(
                            capability,
                            LazyOptional.of(() -> new BlessingStackCurio(stack, definition)));
                }
            }
            return LazyOptional.empty();
        }
    }

    private record BlessingStackCurio(
            ItemStack stack, BlessingDefinitions definition) implements ICurio
    {
        @Override
        public ItemStack getStack()
        {
            return stack;
        }

        @Override
        public Multimap<Attribute, AttributeModifier> getAttributeModifiers(
                SlotContext slotContext, UUID uuid)
        {
            LivingEntity wearer = slotContext.entity();
            if (wearer == null || BlessingOwners.isUsableBy(stack, wearer))
            {
                return BlessingAttributes.get(definition, slotContext, uuid, stack);
            }
            return HashMultimap.create();
        }

        @Override
        public List<Component> getSlotsTooltip(List<Component> tooltips)
        {
            return BlessingNbt.withoutBlankLines(tooltips);
        }

        @Override
        public List<Component> getAttributesTooltip(List<Component> tooltips)
        {
            return List.of();
        }

        @Override
        public boolean canUnequip(SlotContext slotContext)
        {
            return true;
        }
    }
}
