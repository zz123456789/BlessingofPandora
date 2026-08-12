package com.blessingofpandora;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID)
public class BlessingOfPandoraEvents
{
    private static final ResourceLocation CURSE_OF_INERTIA =
            ResourceLocation.parse("curseofpandora:curse_of_inertia");
    private static final ResourceLocation CURSE_OF_PROXIMITY =
            ResourceLocation.parse("curseofpandora:curse_of_proximity");
    private static final Map<UUID, Double> FALL_STARTS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        Player player = event.player;
        if (player.level().isClientSide)
        {
            return;
        }

        UUID id = player.getUUID();
        if (player.level().dimension() != Level.OVERWORLD)
        {
            FALL_STARTS.remove(id);
            return;
        }

        double y = player.getY();
        boolean falling = !player.onGround() && player.getDeltaMovement().y < 0.0;
        Double startY = FALL_STARTS.get(id);

        if (falling && y >= 666.0)
        {
            startY = y;
        }

        if (startY != null && y <= -60.0)
        {
            convertWornProximity(player);
            FALL_STARTS.remove(id);
            return;
        }

        if (falling && startY != null)
        {
            FALL_STARTS.put(id, startY);
        }
        else
        {
            FALL_STARTS.remove(id);
        }
    }

    @SubscribeEvent
    public static void onWitherKilled(LivingDeathEvent event)
    {
        if (event.getEntity().getType() != EntityType.WITHER)
        {
            return;
        }
        if (event.getSource().getDirectEntity() instanceof Player player)
        {
            if (!player.level().isClientSide
                    && player.getMainHandItem().isEmpty()
                    && player.hasEffect(MobEffects.DAMAGE_BOOST))
            {
                convertWornCurse(player);
            }
        }
    }

    private static void convertWornCurse(Player player)
    {
        Item curse = ForgeRegistries.ITEMS.getValue(CURSE_OF_INERTIA);
        if (curse == null)
        {
            return;
        }

        CuriosApi.getCuriosInventory(player).ifPresent(inventory ->
                inventory.getStacksHandler("pandora_charm").ifPresent(handler -> {
                    IDynamicStackHandler stacks = handler.getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++)
                    {
                        ItemStack stack = stacks.getStackInSlot(i);
                        if (stack.is(curse) && !BlessingNbt.isBlessing(stack))
                        {
                            ItemStack blessingStack = stack.copy();
                            BlessingNbt.convertToBlessing(
                                    blessingStack, BlessingDefinitions.FREEDOM, player.getUUID());
                            stacks.setStackInSlot(i, blessingStack);
                            PandoraProgress.onCurseConverted(player);
                            announceConversion(player, curse, BlessingDefinitions.FREEDOM);
                            break;
                        }
                    }
                }));
    }

    private static void convertWornProximity(Player player)
    {
        Item curse = ForgeRegistries.ITEMS.getValue(CURSE_OF_PROXIMITY);
        if (curse == null)
        {
            return;
        }

        CuriosApi.getCuriosInventory(player).ifPresent(inventory ->
                inventory.getStacksHandler("pandora_charm").ifPresent(handler -> {
                    IDynamicStackHandler stacks = handler.getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++)
                    {
                        ItemStack stack = stacks.getStackInSlot(i);
                        if (stack.is(curse) && !BlessingNbt.isBlessing(stack))
                        {
                            ItemStack blessingStack = stack.copy();
                            BlessingNbt.convertToBlessing(
                                    blessingStack, BlessingDefinitions.INFINITY, player.getUUID());
                            stacks.setStackInSlot(i, blessingStack);
                            PandoraProgress.onCurseConverted(player);
                            announceConversion(player, curse, BlessingDefinitions.INFINITY);
                            break;
                        }
                    }
                }));
    }

    private static void announceConversion(
            Player player, Item curse, BlessingDefinitions blessing)
    {
        Component message = Component.translatable(
                        "blessingofpandora.message.converted",
                        new ItemStack(curse).getHoverName(),
                        Component.translatable(blessing.nameKey()).withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.GOLD);
        BlessingNotices.show(player, message);
    }
}
