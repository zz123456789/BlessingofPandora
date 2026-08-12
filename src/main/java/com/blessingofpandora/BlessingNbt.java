package com.blessingofpandora;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.List;

public final class BlessingNbt
{
    public static final String BLESSING_TAG = "blessingofpandora:blessing";
    public static final String BLESSING_ID_TAG = "blessingofpandora:blessing_id";
    public static final String OWNER_TAG = "blessingofpandora:owner";
    public static final int HIDE_ENCHANTMENTS = 1;

    private BlessingNbt()
    {
    }

    public static boolean isBlessing(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().getBoolean(BLESSING_TAG);
    }

    public static boolean isBlessing(ItemStack stack, BlessingDefinitions definition)
    {
        return definition.matches(stack);
    }

    public static String blessingId(ItemStack stack)
    {
        if (stack.hasTag() && stack.getTag().contains(BLESSING_ID_TAG))
        {
            return stack.getTag().getString(BLESSING_ID_TAG);
        }
        return "";
    }

    public static BlessingDefinitions getBlessing(ItemStack stack)
    {
        if (!isBlessing(stack))
        {
            return null;
        }
        String id = blessingId(stack);
        for (BlessingDefinitions definition : BlessingDefinitions.values())
        {
            if (definition.id().equals(id))
            {
                return definition;
            }
        }
        for (BlessingDefinitions definition : BlessingDefinitions.values())
        {
            Item curse = definition.curseItem();
            if (curse != null && stack.is(curse))
            {
                return definition;
            }
        }
        return null;
    }

    public static ItemStack createStack(BlessingDefinitions definition)
    {
        Item curse = definition.curseItem();
        if (curse == null)
        {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(curse);
        convertToBlessing(stack, definition);
        return stack;
    }

    public static void convertToBlessing(ItemStack stack, BlessingDefinitions definition)
    {
        convertToBlessing(stack, definition, null);
    }

    public static void convertToBlessing(
            ItemStack stack, BlessingDefinitions definition, java.util.UUID owner)
    {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(BLESSING_TAG, true);
        tag.putString(BLESSING_ID_TAG, definition.id());
        if (owner != null)
        {
            tag.putString(OWNER_TAG, owner.toString());
        }
        tag.putInt("CustomModelData", definition.modelData());
        addEnchantGlint(tag);
        stack.setHoverName(
                Component.translatable(definition.nameKey()).withStyle(ChatFormatting.GOLD));
    }

    public static boolean isWearingBlessing(
            LivingEntity wearer, BlessingDefinitions definition)
    {
        return CuriosApi.getCuriosInventory(wearer)
                .map(inventory -> {
                    for (ICurioStacksHandler handler : inventory.getCurios().values())
                    {
                        IDynamicStackHandler stacks = handler.getStacks();
                        for (int i = 0; i < stacks.getSlots(); i++)
                        {
                            if (isBlessing(stacks.getStackInSlot(i), definition)
                                    && BlessingOwners.isUsableBy(
                                    stacks.getStackInSlot(i), wearer))
                            {
                                return true;
                            }
                        }
                    }
                    return false;
                })
                .orElse(false);
    }

    public static boolean isWearingCurse(LivingEntity wearer, Item item)
    {
        if (item == null)
        {
            return false;
        }
        return CuriosApi.getCuriosInventory(wearer)
                .map(inventory -> {
                    for (ICurioStacksHandler handler : inventory.getCurios().values())
                    {
                        IDynamicStackHandler stacks = handler.getStacks();
                        for (int i = 0; i < stacks.getSlots(); i++)
                        {
                            ItemStack stack = stacks.getStackInSlot(i);
                            if (stack.is(item) && !isBlessing(stack))
                            {
                                return true;
                            }
                        }
                    }
                    return false;
                })
                .orElse(false);
    }

    public static List<Component> withoutBlankLines(List<Component> tooltips)
    {
        List<Component> result = new ArrayList<>();
        for (Component component : tooltips)
        {
            if (!component.getString().isEmpty())
            {
                result.add(component);
            }
        }
        return result;
    }

    private static void addEnchantGlint(CompoundTag tag)
    {
        if (!tag.contains("Enchantments", 9))
        {
            ListTag enchantments = new ListTag();
            CompoundTag enchantment = new CompoundTag();
            enchantment.putString("id", "blessingofpandora:glint");
            enchantment.putInt("lvl", 1);
            enchantments.add(enchantment);
            tag.put("Enchantments", enchantments);
        }
        tag.putInt("HideFlags", tag.getInt("HideFlags") | HIDE_ENCHANTMENTS);
    }
}
