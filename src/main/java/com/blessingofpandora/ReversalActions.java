package com.blessingofpandora;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class ReversalActions
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private ReversalActions()
    {
    }

    public static boolean convertWornCurse(
            Player player, BlessingDefinitions definition)
    {
        if (!ReversalPermission.isAllowed(player, definition))
        {
            BlessingLogs.info(LOGGER, "[BlessingOfPandora] HUD conversion blocked by permission: blessing={}, player={}",
                    definition.id(), player.getScoreboardName());
            return false;
        }
        Item curse = definition.curseItem();
        if (curse == null)
        {
            return false;
        }

        boolean[] converted = {false};
        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            for (ICurioStacksHandler handler : inventory.getCurios().values())
            {
                IDynamicStackHandler stacks = handler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++)
                {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (stack.is(curse) && !BlessingNbt.isBlessing(stack))
                    {
                        ItemStack blessingStack = stack.copy();
                        BlessingNbt.convertToBlessing(
                                blessingStack, definition, player.getUUID());
                        stacks.setStackInSlot(i, blessingStack);
                        BlessingAdvancements.onCurseConverted(player, definition);
                        PandoraProgress.onCurseConverted(player);
                        if (definition == BlessingDefinitions.FREEDOM
                                || definition == BlessingDefinitions.FEARLESSNESS)
                        {
                            KillReversalProgress.reset(player, definition);
                            BlessingReversals.syncReversalState(player);
                        }
                        if (definition == BlessingDefinitions.REQUIEM)
                        {
                            SixthCurseProgress.markCompleted(player);
                            SixthCurseProgress.reset(player);
                            BlessingReversals.syncReversalState(player);
                        }
                        announceConversion(player, curse, definition);
                        BlessingLogs.info(LOGGER, "[BlessingOfPandora] HUD conversion succeeded: blessing={}, player={}",
                                definition.id(), player.getScoreboardName());
                        converted[0] = true;
                        return;
                    }
                }
            }
        });
        return converted[0];
    }

    public static boolean revertWornBlessing(
            Player player, BlessingDefinitions definition)
    {
        Item curse = definition.curseItem();
        if (curse == null)
        {
            return false;
        }

        boolean[] reverted = {false};
        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            for (ICurioStacksHandler handler : inventory.getCurios().values())
            {
                IDynamicStackHandler stacks = handler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++)
                {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (stack.is(curse) && BlessingNbt.isBlessing(stack, definition)
                            && BlessingOwners.isOwner(stack, player.getUUID()))
                    {
                        ItemStack curseStack = stack.copy();
                        BlessingNbt.revertToCurse(curseStack);
                        stacks.setStackInSlot(i, curseStack);
                        PandoraProgress.onCurseReverted(player);
                        announceRevert(player, curse, definition);
                        BlessingLogs.info(LOGGER, "[BlessingOfPandora] HUD revert succeeded: blessing={}, player={}",
                                definition.id(), player.getScoreboardName());
                        reverted[0] = true;
                        return;
                    }
                }
            }
        });
        return reverted[0];
    }

    private static void announceConversion(
            Player player, Item curse, BlessingDefinitions definition)
    {
        BlessingNotices.show(
                player,
                Component.translatable(
                                "blessingofpandora.message.converted",
                                new ItemStack(curse).getHoverName(),
                                Component.translatable(definition.nameKey())
                                        .withStyle(ChatFormatting.GOLD))
                        .withStyle(ChatFormatting.GOLD));
    }

    private static void announceRevert(
            Player player, Item curse, BlessingDefinitions definition)
    {
        BlessingNotices.show(
                player,
                Component.translatable(
                                "blessingofpandora.message.reverted",
                                Component.translatable(definition.nameKey())
                                        .withStyle(ChatFormatting.GOLD),
                                new ItemStack(curse).getHoverName())
                        .withStyle(ChatFormatting.GOLD));
    }
}
