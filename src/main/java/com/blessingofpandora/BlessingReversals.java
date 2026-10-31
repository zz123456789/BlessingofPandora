package com.blessingofpandora;

import com.mojang.logging.LogUtils;
import com.blessingofpandora.network.BlessingNetwork;
import com.blessingofpandora.network.ReversalStateS2CPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID)
public class BlessingReversals
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation CURSE_OF_FLESH =
            ResourceLocation.parse("curseofpandora:curse_of_flesh");
    private static final ResourceLocation CURSE_OF_METABOLISM =
            ResourceLocation.parse("curseofpandora:curse_of_metabolism");
    private static final ResourceLocation CURSE_OF_PRUDENCE =
            ResourceLocation.parse("curseofpandora:curse_of_prudence");
    private static final ResourceLocation CURSE_OF_SPELL =
            ResourceLocation.parse("curseofpandora:curse_of_spell");

    public static long getExperienceProgress(Player player)
    {
        return SixthCurseProgress.get(player);
    }

    public static void resetExperienceProgress(Player player)
    {
        if (SixthCurseProgress.reset(player))
        {
            syncReversalState(player);
        }
    }

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

        updateExperienceAbsorption(player);
        tryRandomSpellConversion(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onLightningStrike(EntityStruckByLightningEvent event)
    {
        // Let protection/resurrection listeners veto the trigger before granting a blessing.
        if (event.isCanceled() || event.getEntity().level().isClientSide)
        {
            return;
        }
        MobEffect effect = ReversalConfigUtil.effect(
                BlessingOfPandoraConfig.thirdEffect());
        if (event.getEntity() instanceof Player player
                && effect != null
                && player.hasEffect(effect)
                && isWearing(player, getCurse(CURSE_OF_FLESH)))
        {
            BlessingLogs.info(LOGGER, "[BlessingOfPandora] Third Curse condition met: player={}, effect={}",
                    player.getScoreboardName(), effect.getDescriptionId());
            convertWornCurse(
                    player, getCurse(CURSE_OF_FLESH), BlessingDefinitions.SPIRIT);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onMobKilled(LivingDeathEvent event)
    {
        if (event.isCanceled() || event.getEntity().level().isClientSide) return;
        Player player = ReversalCombatCredit.player(event.getSource());
        if (player != null)
        {
            LivingEntity killed = event.getEntity();
            Item curseOfMetabolism = getCurse(CURSE_OF_METABOLISM);
            MobEffect fourthEffect = ReversalConfigUtil.effect(
                    BlessingOfPandoraConfig.fourthEffect());
            if (isWearing(player, curseOfMetabolism)
                    && ReversalConfigUtil.entityMatches(
                            killed, BlessingOfPandoraConfig.fourthMob())
                    && fourthEffect != null
                    && killed.hasEffect(fourthEffect)
                    && killed.getMaxHealth() >= BlessingOfPandoraConfig.fourthMaxHealth())
            {
                BlessingLogs.info(LOGGER, "[BlessingOfPandora] Fourth Curse condition met: player={}, mob={}, health={}, effect={}",
                        player.getScoreboardName(),
                        killed.getType().getDescriptionId(),
                        killed.getMaxHealth(),
                        fourthEffect.getDescriptionId());
                convertWornCurse(
                        player, curseOfMetabolism, BlessingDefinitions.STRENGTH);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onFifthMobKilled(LivingDeathEvent event)
    {
        if (!event.isCanceled() && !event.getEntity().level().isClientSide
                && ReversalConfigUtil.entityMatches(
                        event.getEntity(), BlessingOfPandoraConfig.fifthMob()))
        {
            Player player = ReversalCombatCredit.player(event.getSource());
            if (player != null)
            {
                KillReversalProgress.tryConversion(player, BlessingDefinitions.FEARLESSNESS);
            }
        }
    }

    private static void updateExperienceAbsorption(Player player)
    {
        if (SixthCurseProgress.stateChanged(player))
        {
            syncReversalState(player);
        }
        int state = SixthCurseProgress.state(player);
        if (state == SixthCurseProgress.NOT_WEARING)
        {
            if (SixthCurseProgress.reset(player))
            {
                syncReversalState(player);
            }
            return;
        }
        if (state != SixthCurseProgress.WEARING_CURSE)
        {
            return;
        }
        if (!ReversalPermission.isAllowed(player, BlessingDefinitions.REQUIEM))
        {
            return;
        }
        if (!player.hasEffect(BlessingOfPandora.SOUL_SCULPTING.get()))
        {
            return;
        }

        long target = BlessingOfPandoraConfig.sixthExperience();
        long progress = SixthCurseProgress.get(player);
        if (progress >= target)
        {
            convertWornCurse(player, getCurse(CURSE_OF_PRUDENCE), BlessingDefinitions.REQUIEM);
            return;
        }
        long before = availableExperience(player);
        int drain = ExperienceMath.drain(before, progress, target);
        if (drain == 0) return;
        player.giveExperiencePoints(-drain);
        long actual = Math.max(0, before - availableExperience(player));
        if (actual <= 0)
        {
            return;
        }

        long absorbed = SixthCurseProgress.add(player, actual);
        syncReversalState(player);
        if (absorbed >= BlessingOfPandoraConfig.sixthExperience())
        {
            convertWornCurse(
                    player, getCurse(CURSE_OF_PRUDENCE),
                    BlessingDefinitions.REQUIEM);
        }
    }

    private static long availableExperience(Player player)
    {
        return ExperienceMath.available(player.experienceLevel,
                player.experienceProgress, player.getXpNeededForNextLevel());
    }

    public static void syncReversalState(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            BlessingNetwork.sendToPlayer(
                    serverPlayer,
                    new ReversalStateS2CPacket(
                            player.getUUID(),
                            ReversalPermission.states(player),
                            SixthCurseProgress.state(player),
                            SixthCurseProgress.displayValue(player),
                            KillReversalProgress.failures(player, BlessingDefinitions.FREEDOM),
                            KillReversalProgress.failures(player, BlessingDefinitions.FEARLESSNESS)));
        }
    }

    private static void tryRandomSpellConversion(Player player)
    {
        Item curse = getCurse(CURSE_OF_SPELL);
        if (!isWearing(player, curse))
        {
            return;
        }
        if (!ReversalPermission.isAllowed(player, BlessingDefinitions.LIBERATION))
        {
            return;
        }

        // Keep this second Curios read: holder callbacks may have queued an update.
        ItemStack curseStack = findWornCurseStack(player, curse);
        if (curseStack.isEmpty())
        {
            return;
        }
        long curseCount = CurseEnchantments.count(curseStack);
        double chance = BlessingOfPandoraConfig.seventhBaseChance()
                + curseCount * BlessingOfPandoraConfig.seventhCurseBonus();
        if (player.getRandom().nextDouble() < chance)
        {
            convertWornCurse(player, curse, BlessingDefinitions.LIBERATION);
        }
    }

    private static Item getCurse(ResourceLocation id)
    {
        return ForgeRegistries.ITEMS.getValue(id);
    }

    private static boolean isWearing(Player player, Item item)
    {
        return BlessingNbt.isWearingCurse(player, item);
    }

    public static ItemStack findWornCurseStack(Player player, Item item)
    {
        if (item == null)
        {
            return ItemStack.EMPTY;
        }
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return ItemStack.EMPTY;
        for (ICurioStacksHandler handler : inventory.getCurios().values())
        {
            IDynamicStackHandler stacks = handler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++)
            {
                ItemStack stack = stacks.getStackInSlot(i);
                if (stack.is(item) && !BlessingNbt.isBlessing(stack))
                {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static void convertWornCurse(Player player, Item curse, BlessingDefinitions blessing)
    {
        ReversalActions.convertWornCurse(player, blessing);
    }
}
