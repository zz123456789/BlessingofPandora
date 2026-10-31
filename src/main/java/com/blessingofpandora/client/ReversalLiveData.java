package com.blessingofpandora.client;

import com.blessingofpandora.BlessingDefinitions;
import com.blessingofpandora.BlessingNbt;
import com.blessingofpandora.CurseEnchantments;
import com.blessingofpandora.ReversalSettings;
import com.blessingofpandora.SeventhCursePreview;
import com.blessingofpandora.ReversalDescriptions;
import com.blessingofpandora.SixthCurseProgress;
import dev.xkmc.pandora.content.base.IPandoraHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

@OnlyIn(Dist.CLIENT)
public final class ReversalLiveData
{
    public static final int NOT_WORN = 0;
    public static final int WEARING_CURSE = 1;
    public static final int WEARING_BLESSING = 2;

    private ReversalLiveData()
    {
    }

    public static int wornState(BlessingDefinitions definition)
    {
        Player player = Minecraft.getInstance().player;
        if (player == null)
        {
            return NOT_WORN;
        }
        Item curse = definition.curseItem();
        ItemStack stack = findWornItem(player, curse);
        if (stack.isEmpty())
        {
            return NOT_WORN;
        }
        return BlessingNbt.isBlessing(stack, definition)
                ? WEARING_BLESSING
                : WEARING_CURSE;
    }

    public static Component liveLiberationStep()
    {
        return liveLiberationStep(null);
    }

    public static Component liveLiberationStep(InteractionHand inspectedHand)
    {
        Player player = Minecraft.getInstance().player;
        long curseCount = inspectedHand == null
                ? CurseEnchantments.count(findWornItem(player, BlessingDefinitions.LIBERATION.curseItem(), true))
                : SeventhCursePreview.count(player, inspectedHand);
        return liveLiberationStep(curseCount, inspectedHand != null);
    }

    static Component liveLiberationStep(long curseCount, boolean heldPreview)
    {
        ReversalSettings settings = ReversalSettings.display();
        // A chance above 100% always succeeds on the server, even with large configured bonuses.
        double chancePercent = Math.min(100.0, settings.seventhBaseChancePercent()
                + curseCount * settings.seventhCurseBonusPercent());
        return ReversalDescriptions.liveLiberationStep(
                curseCount, chancePercent, heldPreview);
    }

    public static Component liveRequiemStep()
    {
        if (ClientReversalPermission.sixthCurseState()
                == SixthCurseProgress.NOT_WEARING)
        {
            return ReversalDescriptions.liveRequiemNotWearing()
                    .copy()
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        }
        return ReversalDescriptions.liveRequiemStep(
                ClientReversalPermission.sixthAbsorbedExperience());
    }

    public static Component liveStateStep(BlessingDefinitions definition)
    {
        Player player = Minecraft.getInstance().player;
        if (player == null)
        {
            return Component.empty();
        }

        int state = wornState(definition);
        Item curse = definition.curseItem();
        if (curse == null)
        {
            return Component.empty();
        }

        if (state == WEARING_CURSE)
        {
            return Component.translatable(
                            "blessingofpandora.hud.state.wearing_curse",
                            new ItemStack(curse).getHoverName())
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC);
        }
        if (state == WEARING_BLESSING)
        {
            return Component.translatable(
                            "blessingofpandora.hud.state.wearing_blessing",
                            Component.translatable(definition.nameKey()))
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC);
        }
        return Component.translatable(
                        "blessingofpandora.hud.state.not_worn",
                        new ItemStack(curse).getHoverName())
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    }

    public static Component appendLiveState(
            Component base, BlessingDefinitions definition)
    {
        Component state = liveStateStep(definition);
        if (state.getString().isEmpty())
        {
            return base;
        }
        return base.copy().append(" ").append(state);
    }

    static ItemStack findWornItem(Player player, Item item)
    {
        return findWornItem(player, item, false);
    }

    private static ItemStack findWornItem(Player player, Item item, boolean curseOnly)
    {
        if (player == null || item == null) return ItemStack.EMPTY;
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return ItemStack.EMPTY;
        for (ICurioStacksHandler handler : inventory.getCurios().values())
        {
            IDynamicStackHandler stacks = handler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++)
            {
                ItemStack stack = stacks.getStackInSlot(i);
                if (matches(stack, item, curseOnly))
                {
                    return stack;
                }
            }
        }

        // Pandora's virtual-slot index can retain old holder coordinates after a
        // same-size move or when the size sync arrives before the holder stack.
        // Read the effective worn holder itself as a display-only fallback. Do not
        // resize/rebuild slots or include cosmetics, held items or ordinary storage.
        for (var entry : inventory.getCurios().entrySet())
        {
            if ("pandora_charm".equals(entry.getKey())) continue;
            IDynamicStackHandler stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++)
            {
                ItemStack worn = stacks.getStackInSlot(i);
                if (!(worn.getItem() instanceof IPandoraHolder holder)) continue;
                var contents = holder.getCap(worn).orElse(null);
                if (contents == null) continue;
                for (int slot = 0; slot < contents.getSlots(); slot++)
                {
                    ItemStack stack = contents.getStackInSlot(slot);
                    if (matches(stack, item, curseOnly)) return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean matches(ItemStack stack, Item item, boolean curseOnly)
    {
        return stack.is(item) && (!curseOnly || !BlessingNbt.isBlessing(stack));
    }

}
