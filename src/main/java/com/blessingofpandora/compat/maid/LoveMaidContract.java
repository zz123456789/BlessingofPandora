package com.blessingofpandora.compat.maid;

import com.blessingofpandora.LoveOrbRegistration;
import com.blessingofpandora.PandoraItemUtil;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidTamedEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.blessingofpandora.compat.love.LoveLedger;
import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

/** Loaded only when Touhou Little Maid is installed. No polling, reflection or world scans. */
public final class LoveMaidContract
{
    public static final String FIRST_CONTRACT_SEEN = "blessingofpandora:first_maid_contract_seen";
    public static final String LOVE_REWARDED = "blessingofpandora:love_orb_rewarded";
    public static final String FIRST_MAID = "blessingofpandora:first_maid_uuid";
    public static final String FIRST_MAID_NAME = "blessingofpandora:first_maid_name";
    public static final String CONTRACT_SCHEMA = "blessingofpandora:love_contract_schema";
    public static final String LEGACY_RECOVERY_ELIGIBLE = "blessingofpandora:love_legacy_recovery_eligible";
    private static final ResourceLocation FIRST_CONTRACT =
            ResourceLocation.parse("touhou_little_maid:base/tamed_maid");
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
        bus.addListener(LoveMaidContract::onCurioChange);
        bus.addListener(LoveMaidContract::onRespawn);
        bus.addListener(EventPriority.LOWEST, LoveMaidContract::onInteract);
    }

    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !FIRST_CONTRACT.equals(event.getAdvancement().getId())) return;
        CompoundTag persisted = persisted(player);
        if (persisted.getBoolean(FIRST_CONTRACT_SEEN)) return;
        // Upstream also awards this advancement on ownership conversion. Keep
        // it as legacy history only; it never consumes a natural-tame reward.
        putFlag(player, FIRST_CONTRACT_SEEN);
    }

    public static void onTamed(MaidTamedEvent event)
    {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        // Acquiring another maid by ownership conversion is still another
        // contract for jealousy; first-reward eligibility is checked separately.
        com.blessingofpandora.compat.love.LoveProgressEvents.onOtherTame(player, event.getMaid().getUUID());
        CompoundTag data = persisted(player);
        if (event.isOwnerConversion() || data.getBoolean(LOVE_REWARDED)) return;
        if (!data.hasUUID(FIRST_MAID))
        {
            LoveLedger ledger = LoveLedger.get(player.server);
            // Unknown pre-fix history requires the explicit legacy choice, not
            // silently calling a later maid the first. New saves instead use
            // this actual natural-tame event, independent of advancement state.
            if (data.getBoolean(LEGACY_RECOVERY_ELIGIBLE)
                    || ledger.get(player.getUUID()) != null || ledger.isBanned(player.getUUID())) return;
            data.putBoolean(FIRST_CONTRACT_SEEN, true);
            data.putUUID(FIRST_MAID, event.getMaid().getUUID());
            data.putString(FIRST_MAID_NAME, event.getMaid().getName().getString());
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, data);
        }
        tryGiveReward(player);
    }

    /** Event-driven deferred gift; the original maid need not be loaded or nearby. */
    public static void tryGiveReward(ServerPlayer player)
    {
        giveReward(player, "message.blessingofpandora.love_orb.received");
    }

    private static boolean giveReward(ServerPlayer player, String message)
    {
        CompoundTag data = persisted(player);
        if (data.getBoolean(LOVE_REWARDED) || !data.hasUUID(FIRST_MAID)
                || !player.isAlive() || !isWearingNecklace(player)) return false;

        ItemStack reward = LoveOrbRegistration.createStack();
        if (reward.isEmpty()) return false;
        com.blessingofpandora.compat.love.LoveProgressEvents.bindReward(
                player, data.getUUID(FIRST_MAID), data.getString(FIRST_MAID_NAME), reward);
        if (reward.isEmpty()) return false;
        // Mark before handing out the item, preventing re-entrant reward callbacks.
        putFlag(player, LOVE_REWARDED);
        player.getInventory().add(reward);
        if (!reward.isEmpty()) player.drop(reward, false);
        player.sendSystemMessage(Component.translatable(message));
        return true;
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Advancement advancement = player.server.getAdvancements().getAdvancement(FIRST_CONTRACT);
        boolean completed = advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
        CompoundTag data = persisted(player);
        if (data.getInt(CONTRACT_SCHEMA) < 1)
        {
            // Freeze migration eligibility on this player's first login after the fix.
            // Later advancement awards or ownership conversions are not old-save history.
            LoveLedger ledger = LoveLedger.get(player.server);
            boolean missed = (data.getBoolean(FIRST_CONTRACT_SEEN) || completed)
                    && !data.getBoolean(LOVE_REWARDED) && !data.hasUUID(FIRST_MAID)
                    && ledger.get(player.getUUID()) == null && !ledger.isBanned(player.getUUID());
            data.putBoolean(LEGACY_RECOVERY_ELIGIBLE, missed);
            data.putInt(CONTRACT_SCHEMA, 1);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, data);
        }
        if (completed)
        {
            // A historical advancement alone contains no maid UUID. Never bind
            // a later tame as the first merely because the player updated.
            putFlag(player, FIRST_CONTRACT_SEEN);
        }
        tryGiveReward(player);
    }

    public static void onClone(PlayerEvent.Clone event)
    {
        CompoundTag original = persisted(event.getOriginal());
        if (original.getBoolean(FIRST_CONTRACT_SEEN)) putFlag(event.getEntity(), FIRST_CONTRACT_SEEN);
        if (original.getBoolean(LOVE_REWARDED)) putFlag(event.getEntity(), LOVE_REWARDED);
        if (original.contains(CONTRACT_SCHEMA))
        {
            CompoundTag data = persisted(event.getEntity());
            data.putInt(CONTRACT_SCHEMA, original.getInt(CONTRACT_SCHEMA));
            data.putBoolean(LEGACY_RECOVERY_ELIGIBLE, original.getBoolean(LEGACY_RECOVERY_ELIGIBLE));
            event.getEntity().getPersistentData().put(Player.PERSISTED_NBT_TAG, data);
        }
        if (original.hasUUID(FIRST_MAID))
        {
            CompoundTag data = persisted(event.getEntity());
            data.putUUID(FIRST_MAID, original.getUUID(FIRST_MAID));
            data.putString(FIRST_MAID_NAME, original.getString(FIRST_MAID_NAME));
            event.getEntity().getPersistentData().put(Player.PERSISTED_NBT_TAG, data);
        }
    }

    private static void onCurioChange(CurioChangeEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player
                && PandoraItemUtil.isPandoraNecklace(event.getTo())) tryGiveReward(player);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player) tryGiveReward(player);
    }

    /** 1.7.3 stored only a boolean for missed gifts. Let its owner explicitly select a maid once. */
    public static void onInteract(PlayerInteractEvent.EntityInteract event)
    {
        if (event.isCanceled() || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()
                || !(event.getTarget() instanceof EntityMaid maid) || !maid.isAlive()
                || !maid.isTame() || !maid.isOwnedBy(player)) return;
        CompoundTag data = persisted(player);
        if (data.getInt(CONTRACT_SCHEMA) < 1 || !data.getBoolean(LEGACY_RECOVERY_ELIGIBLE)
                || data.getBoolean(LOVE_REWARDED) || data.hasUUID(FIRST_MAID) || !isWearingNecklace(player)) return;
        LoveLedger ledger = LoveLedger.get(player.server);
        if (ledger.isBanned(player.getUUID()) || ledger.get(player.getUUID()) != null) return;
        data.putBoolean(LEGACY_RECOVERY_ELIGIBLE, false);
        data.putBoolean(FIRST_CONTRACT_SEEN, true);
        data.putUUID(FIRST_MAID, maid.getUUID());
        data.putString(FIRST_MAID_NAME, maid.getName().getString());
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, data);
        if (giveReward(player, "message.blessingofpandora.love_orb.recovered"))
        {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        // Eligibility is entirely persisted; no same-tick advancement token remains.
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
