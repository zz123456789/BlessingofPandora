package com.blessingofpandora;

import com.blessingofpandora.client.ReversalHudClient;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.DistExecutor;

@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID)
public final class ReversalInteractionHandler
{
    private ReversalInteractionHandler()
    {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event)
    {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown())
        {
            return;
        }

        ItemStack stack = event.getItemStack();
        boolean handled = false;
        if (PandoraItemUtil.isPandoraHolder(stack))
        {
            if (event.getEntity().level().isClientSide)
            {
                DistExecutor.safeRunWhenOn(
                        Dist.CLIENT, () -> ReversalHudClient::openGlobal);
            }
            handled = true;
        }
        else
        {
            BlessingDefinitions definition = curseDefinition(stack);
            if (definition != null)
            {
                if (event.getEntity().level().isClientSide)
                {
                    DistExecutor.safeRunWhenOn(
                            Dist.CLIENT,
                            () -> () -> ReversalHudClient.openPermission(definition, event.getHand()));
                }
                handled = true;
            }
        }

        if (handled)
        {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static BlessingDefinitions curseDefinition(ItemStack stack)
    {
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
}
