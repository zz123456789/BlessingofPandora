package com.blessingofpandora;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
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
public class BlessingReversals
{
    private static final long REQUIEM_XP_TOTAL = 4_000_000L;
    private static final int REQUIEM_XP_PER_TICK = 50_000;
    private static final double SPELL_INITIAL_CHANCE = 0.00003;
    private static final long SPELL_CHANCE_INTERVAL = 2000L;
    private static final double SPELL_CHANCE_STEP = 0.00001;

    private static final ResourceLocation CURSE_OF_FLESH =
            ResourceLocation.parse("curseofpandora:curse_of_flesh");
    private static final ResourceLocation CURSE_OF_METABOLISM =
            ResourceLocation.parse("curseofpandora:curse_of_metabolism");
    private static final ResourceLocation CURSE_OF_TENSION =
            ResourceLocation.parse("curseofpandora:curse_of_tension");
    private static final ResourceLocation CURSE_OF_PRUDENCE =
            ResourceLocation.parse("curseofpandora:curse_of_prudence");
    private static final ResourceLocation CURSE_OF_SPELL =
            ResourceLocation.parse("curseofpandora:curse_of_spell");

    private static final Map<UUID, Long> EXPERIENCE_PROGRESS = new HashMap<>();
    private static final Map<UUID, Long> SPELL_START_TICKS = new HashMap<>();

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

    @SubscribeEvent
    public static void onLightningStrike(EntityStruckByLightningEvent event)
    {
        if (event.getEntity() instanceof Player player
                && player.hasEffect(MobEffects.INVISIBILITY)
                && isWearing(player, getCurse(CURSE_OF_FLESH)))
        {
            convertWornCurse(
                    player, getCurse(CURSE_OF_FLESH), BlessingDefinitions.SPIRIT);
        }
    }

    @SubscribeEvent
    public static void onMobKilled(LivingDeathEvent event)
    {
        if (event.getSource().getEntity() instanceof Player player)
        {
            LivingEntity killed = event.getEntity();
            Item curseOfMetabolism = getCurse(CURSE_OF_METABOLISM);
            if (isWearing(player, curseOfMetabolism)
                    && killed.hasEffect(MobEffects.WEAKNESS)
                    && killed.getMaxHealth() >= 325.0F)
            {
                convertWornCurse(
                        player, curseOfMetabolism, BlessingDefinitions.STRENGTH);
            }

            if (killed.getType() == EntityType.WARDEN
                    && isWearing(player, getCurse(CURSE_OF_TENSION))
                    && isHoldingSculkShrieker(player))
            {
                convertWornCurse(
                        player, getCurse(CURSE_OF_TENSION), BlessingDefinitions.FEARLESSNESS);
            }
        }
    }

    private static void updateExperienceAbsorption(Player player)
    {
        UUID id = player.getUUID();
        Item curse = getCurse(CURSE_OF_PRUDENCE);
        if (!isWearing(player, curse))
        {
            EXPERIENCE_PROGRESS.remove(id);
            return;
        }
        if (!player.hasEffect(BlessingOfPandora.SOUL_SCULPTING.get()))
        {
            return;
        }

        long absorbed = EXPERIENCE_PROGRESS.getOrDefault(id, 0L);
        int currentExperience = getCurrentExperiencePoints(player);
        int drain = Math.min(
                REQUIEM_XP_PER_TICK,
                (int) Math.min(REQUIEM_XP_TOTAL - absorbed, currentExperience));
        if (drain <= 0)
        {
            return;
        }

        int before = getCurrentExperiencePoints(player);
        player.giveExperiencePoints(-drain);
        int actual = before - getCurrentExperiencePoints(player);
        if (actual <= 0)
        {
            return;
        }

        absorbed += actual;
        if (absorbed >= REQUIEM_XP_TOTAL)
        {
            EXPERIENCE_PROGRESS.remove(id);
            convertWornCurse(
                    player, curse, BlessingDefinitions.REQUIEM);
        }
        else
        {
            EXPERIENCE_PROGRESS.put(id, absorbed);
        }
    }

    private static int getCurrentExperiencePoints(Player player)
    {
        int total = 0;
        for (int level = 0; level < player.experienceLevel; level++)
        {
            total += xpRequiredForLevel(level);
        }
        total += (int) (player.experienceProgress * player.getXpNeededForNextLevel());
        return total;
    }

    private static int xpRequiredForLevel(int level)
    {
        if (level >= 30)
        {
            return 112 + (level - 30) * 9;
        }
        if (level >= 15)
        {
            return 37 + (level - 15) * 5;
        }
        return 7 + level * 2;
    }

    private static void tryRandomSpellConversion(Player player)
    {
        Item curse = getCurse(CURSE_OF_SPELL);
        UUID id = player.getUUID();
        long now = player.level().getGameTime();
        if (!isWearing(player, curse))
        {
            SPELL_START_TICKS.remove(id);
            return;
        }

        long start = SPELL_START_TICKS.getOrDefault(id, now);
        long elapsed = now - start;
        double chance = SPELL_INITIAL_CHANCE
                + (elapsed / SPELL_CHANCE_INTERVAL) * SPELL_CHANCE_STEP;
        if (player.getRandom().nextDouble() < chance)
        {
            SPELL_START_TICKS.remove(id);
            convertWornCurse(player, curse, BlessingDefinitions.LIBERATION);
        }
        else
        {
            SPELL_START_TICKS.put(id, start);
        }
    }

    private static boolean isHoldingSculkShrieker(Player player)
    {
        return player.getMainHandItem().is(Blocks.SCULK_SHRIEKER.asItem())
                || player.getOffhandItem().is(Blocks.SCULK_SHRIEKER.asItem());
    }

    private static Item getCurse(ResourceLocation id)
    {
        return ForgeRegistries.ITEMS.getValue(id);
    }

    private static boolean isWearing(Player player, Item item)
    {
        return BlessingNbt.isWearingCurse(player, item);
    }

    private static void convertWornCurse(Player player, Item curse, BlessingDefinitions blessing)
    {
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
                                    blessingStack, blessing, player.getUUID());
                            stacks.setStackInSlot(i, blessingStack);
                            PandoraProgress.onCurseConverted(player);
                            announceConversion(player, curse, blessing);
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
