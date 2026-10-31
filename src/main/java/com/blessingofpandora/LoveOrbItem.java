package com.blessingofpandora;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/** A link to the owner's charms, never a second inventory of copied items. */
public final class LoveOrbItem extends Item implements ICurioItem
{
    public LoveOrbItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack)
    {
        return "pandora_charm".equals(slotContext.identifier())
                && (!(slotContext.entity() instanceof net.minecraft.server.level.ServerPlayer player)
                || com.blessingofpandora.compat.love.LoveProgressEvents.canUse(player, stack));
    }

    @Override public boolean onEntityItemUpdate(ItemStack stack, net.minecraft.world.entity.item.ItemEntity entity)
    {
        com.blessingofpandora.compat.love.LoveProgressEvents.updateDrop(entity);
        return entity.isRemoved();
    }

    @Override public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected)
    {
        if (entity instanceof net.minecraft.server.level.ServerPlayer player)
        {
            var ledger = com.blessingofpandora.compat.love.LoveLedger.get(player.server);
            java.util.UUID owner = com.blessingofpandora.compat.love.LoveProgressEvents.owner(stack);
            if (ledger.isBanned(player.getUUID()) || owner != null && ledger.isBanned(owner)) stack.setCount(0);
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable(LoveOrbRegistration.isEternal(stack)
                        ? "tooltip.blessingofpandora.love.eternal_summary" : "tooltip.blessingofpandora.love.summary")
                .withStyle(style -> style.withColor(0xD5C5CD)));
        String maidName = stack.hasTag() ? stack.getTag().getString("blessingofpandora:love_maid_name") : "";
        if (!maidName.isBlank())
            tooltip.add(Component.translatable("item.blessingofpandora.love.bound", maidName)
                    .withStyle(style -> style.withColor(0xE39AB5)));
        tooltip.add(Component.translatable("tooltip.blessingofpandora.love.rules")
                .withStyle(style -> style.withColor(0xB7A38B)));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.blessingofpandora.love.open_hud",
                        Component.literal("Alt").withStyle(style -> style.withColor(0xEDB0C8)))
                .withStyle(ChatFormatting.GRAY));
    }
}
