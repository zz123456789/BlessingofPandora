package com.blessingofpandora;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;

public final class PandoraBraceletItem
{
    private static final ResourceLocation PANDORA_BRACELET_ID =
            ResourceLocation.parse("pandora:pandora_bracelet");

    public static final String CHAPTER_TAG = "blessingofpandora:chapter";
    public static final String CHAPTER_PROLOGUE = "prologue";
    public static final String CHAPTER_EPILOGUE = "epilogue";

    private PandoraBraceletItem()
    {
    }

    public static ItemStack create(UUID owner, String chapter)
    {
        Item item = ForgeRegistries.ITEMS.getValue(PANDORA_BRACELET_ID);
        if (item == null || item == Items.AIR)
        {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putString(CHAPTER_TAG, chapter);
        stack.getOrCreateTag().putString(BlessingNbt.OWNER_TAG, owner.toString());
        return stack;
    }

    public static boolean isPandoraBracelet(ItemStack stack)
    {
        Item item = ForgeRegistries.ITEMS.getValue(PANDORA_BRACELET_ID);
        return !stack.isEmpty() && item != null && stack.is(item);
    }

    public static boolean isOwner(ItemStack stack, UUID uuid)
    {
        if (!stack.hasTag() || !stack.getTag().contains(BlessingNbt.OWNER_TAG))
        {
            return true;
        }
        return uuid != null && stack.getTag().getString(BlessingNbt.OWNER_TAG)
                .equals(uuid.toString());
    }

    public static void addPandoraPoem(List<Component> tooltip, ItemStack stack)
    {
        String chapter = stack.hasTag()
                ? stack.getTag().getString(CHAPTER_TAG)
                : "";
        int index = Math.min(1, tooltip.size());
        if (CHAPTER_PROLOGUE.equals(chapter))
        {
            index = addPoem(tooltip, index, "✦ 序诗 ✦", new String[] {
                    "缪斯啊，且翻转这七重铁砧！",
                    "教苦涩的律法在锻打中甘甜，",
                    "令捆缚之轭化作飞升的梯磴。"
            });
        }
        else if (CHAPTER_EPILOGUE.equals(chapter))
        {
            index = addPoem(tooltip, index, "✦ 终章 ✦", new String[] {
                    "此乃七重倒悬的冠冕，七道逆流的河；",
                    "捆锁成了臂弯，伤痛作了眼睑。",
                    "我们负着整座现实的重轭下跪——",
                    "却在低头的瞬间，触到了光的根源。"
            });
        }
        else
        {
            return;
        }
        tooltip.add(index, Component.literal("✦ ──────── ✦")
                .withStyle(ChatFormatting.GOLD));
    }

    private static int addPoem(
            List<Component> tooltip, int index, String title, String[] lines)
    {
        tooltip.add(index++, Component.literal(title)
                .withStyle(ChatFormatting.GOLD)
                .withStyle(ChatFormatting.BOLD));

        TextColor silver = TextColor.fromRgb(0xC8D8E8);
        for (int i = 1; i < lines.length; i++)
        {
            tooltip.add(index++, Component.literal(lines[i])
                    .withStyle(style -> style.withColor(silver)));
        }
        return index;
    }
}
