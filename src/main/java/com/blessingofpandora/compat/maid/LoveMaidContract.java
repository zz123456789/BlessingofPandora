package com.blessingofpandora.compat.maid;

import com.blessingofpandora.LoveOrbRegistration;
import com.blessingofpandora.PandoraItemUtil;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidTamedEvent;
import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Map;
import java.util.WeakHashMap;

/** Loaded only when Touhou Little Maid is installed. No polling, reflection or world scans. */
public final class LoveMaidContract
{
    public static final String FIRST_CONTRACT_SEEN = "blessingofpandora:first_maid_contract_seen";
    public static final String LOVE_REWARDED = "blessingofpandora:love_orb_rewarded";
    private static final ResourceLocation FIRST_CONTRACT =
            ResourceLocation.parse("touhou_little_maid:base/tamed_maid");
    // The maid grants its advancement immediately before posting MaidTamedEvent.
    // A same-tick token proves this was a first completion, including in existing saves.
    private static final Map<ServerPlayer, Long> FIRST_CONTRACT_TICK = new WeakHashMap<>();
    private static boolean registered;

    private LoveMaidContract() {}

    public static void register()
    {
        if (registered) return;
        registered = true;
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(LoveMaidContract::onAdvancement);
        bus.addListener(LoveMaidContract::onTamed);
        bus.addListener(LoveMaidContract::onLogin);
        bus.addListener(LoveMaidContract::onClone);
        bus.addListener(LoveMaidContract::onLogout);
        bus.addListener(LoveMaidContract::onServerStopped);
    }

    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !FIRST_CONTRACT.equals(event.getAdvancement().getId())) return;
        CompoundTag persisted = persisted(player);
        if (persisted.getBoolean(FIRST_CONTRACT_SEEN)) return;
        // Consume the first opportunity even if the necklace is not worn, or a
        // command granted the advancement. Revoking it cannot mint another orb.
        putFlag(player, FIRST_CONTRACT_SEEN);
        FIRST_CONTRACT_TICK.put(player, player.serverLevel().getGameTime());
    }

    public static void onTamed(MaidTamedEvent event)
    {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        // Acquiring another maid by ownership conversion is still another
        // contract for jealousy; first-reward eligibility is checked separately.
        com.blessingofpandora.compat.love.LoveProgressEvents.onOtherTame(player, event.getMaid().getUUID());
        Long firstTick = FIRST_CONTRACT_TICK.remove(player);
        putFlag(player, FIRST_CONTRACT_SEEN);
        if (event.isOwnerConversion() || firstTick == null
                || firstTick.longValue() != player.serverLevel().getGameTime()
                || persisted(player).getBoolean(LOVE_REWARDED)
                || !isWearingNecklace(player)) return;

        ItemStack reward = LoveOrbRegistration.createStack();
        if (reward.isEmpty()) return;
        com.blessingofpandora.compat.love.LoveProgressEvents.bindReward(player, event.getMaid(), reward);
        if (reward.isEmpty()) return;
        // Mark before handing out the item, preventing re-entrant reward callbacks.
        putFlag(player, LOVE_REWARDED);
        player.getInventory().add(reward);
        if (!reward.isEmpty()) player.drop(reward, false);
        player.sendSystemMessage(Component.translatable(
                "message.blessingofpandora.love_orb.received"));
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        FIRST_CONTRACT_TICK.remove(player);
        Advancement advancement = player.server.getAdvancements().getAdvancement(FIRST_CONTRACT);
        if (advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone())
        {
            // A player who contracted a maid before installing this version is
            // ineligible; a later contract must not be mistaken for their first.
            putFlag(player, FIRST_CONTRACT_SEEN);
        }
    }

    public static void onClone(PlayerEvent.Clone event)
    {
        FIRST_CONTRACT_TICK.remove(event.getOriginal());
        FIRST_CONTRACT_TICK.remove(event.getEntity());
        CompoundTag original = persisted(event.getOriginal());
        if (original.getBoolean(FIRST_CONTRACT_SEEN)) putFlag(event.getEntity(), FIRST_CONTRACT_SEEN);
        if (original.getBoolean(LOVE_REWARDED)) putFlag(event.getEntity(), LOVE_REWARDED);
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        FIRST_CONTRACT_TICK.remove(event.getEntity());
    }

    public static void onServerStopped(ServerStoppedEvent event)
    {
        FIRST_CONTRACT_TICK.clear();
    }

    private static boolean isWearingNecklace(Player player)
    {
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return false;
        for (var handler : inventory.getCurios().values())
        {
            var stacks = handler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++)
            {
                ItemStack stack = stacks.getStackInSlot(i);
                if (!stack.isEmpty() && PandoraItemUtil.isPandoraNecklace(stack)) return true;
            }
        }
        return false;
    }

    private static CompoundTag persisted(Player player)
    {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static void putFlag(Player player, String key)
    {
        CompoundTag data = persisted(player);
        data.putBoolean(key, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, data);
    }
}
