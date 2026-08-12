package com.blessingofpandora;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID)
public final class SoulSculptingHandler
{
    private SoulSculptingHandler()
    {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide)
        {
            return;
        }

        Player player = event.player;
        Item curse = BlessingDefinitions.REQUIEM.curseItem();
        boolean wearingBrokenSoul = curse != null && BlessingNbt.isWearingCurse(player, curse);
        if (!wearingBrokenSoul)
        {
            return;
        }

        float healthRatio = player.getMaxHealth() <= 0.0f
                ? 1.0f
                : player.getHealth() / player.getMaxHealth();
        MobEffectInstance current =
                player.getEffect(BlessingOfPandora.SOUL_SCULPTING.get());
        if (healthRatio >= 0.75f)
        {
            if (current != null)
            {
                player.removeEffect(BlessingOfPandora.SOUL_SCULPTING.get());
            }
            return;
        }

        int amplifier = healthRatio < 0.25f ? 4 : healthRatio < 0.50f ? 2 : 0;
        if (current == null
                || current.getAmplifier() != amplifier
                || current.getDuration() < 100)
        {
            player.removeEffect(BlessingOfPandora.SOUL_SCULPTING.get());
            player.addEffect(new MobEffectInstance(
                    BlessingOfPandora.SOUL_SCULPTING.get(),
                    100, amplifier, false, true, true));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event)
    {
        if (event.isCanceled() || event.getEntity().level().isClientSide)
        {
            return;
        }

        if (event.getEntity() instanceof Player player)
        {
            MobEffectInstance effect =
                    player.getEffect(BlessingOfPandora.SOUL_SCULPTING.get());
            if (effect != null)
            {
                event.setAmount(event.getAmount() * (1.0f - reductionFor(effect.getAmplifier())));
            }
        }
    }

    private static float reductionFor(int amplifier)
    {
        return switch (amplifier)
        {
            case 0 -> 0.05f;
            case 1 -> 0.07f;
            case 2 -> 0.10f;
            case 3 -> 0.15f;
            default -> 0.20f;
        };
    }
}
