package com.blessingofpandora.compat.love;

import com.blessingofpandora.LoveOrbRegistration;
import com.blessingofpandora.network.BlessingNetwork;
import com.blessingofpandora.network.LoveProgressS2CPacket;
import dev.xkmc.pandora.content.base.IPandoraHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

import java.lang.ref.WeakReference;
import java.util.*;

/** Event-driven progression; timers never serialize progress into the equipped item every tick. */
public final class LoveProgressEvents
{
    public static final String OWNER = "blessingofpandora:love_owner";
    public static final String MAID = "blessingofpandora:love_maid";
    public static final String NAME = "blessingofpandora:love_maid_name";
    private static final ResourceLocation MAID_TYPE = ResourceLocation.parse("touhou_little_maid:maid");
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, WeakReference<LivingEntity>> LOADED_MAIDS = new HashMap<>();
    private static final Set<ItemEntity> DROPS = Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean registered;

    private static final class Session
    {
        boolean worn, equipped, romance;
        long lastTick = Long.MIN_VALUE, nextRomanceCheck, nextSync, nextRequest;
        net.minecraft.core.BlockPos playerPos, maidPos;
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        LoveProgressS2CPacket lastSent;
    }
    private LoveProgressEvents() {}

    public static void register()
    {
        if (registered || !LoveOrbRegistration.isAvailable()) return;
        registered = true;
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(EventPriority.LOWEST, LoveProgressEvents::onPlayerTick);
        bus.addListener(EventPriority.LOWEST, LoveProgressEvents::onChat);
        bus.addListener(LoveProgressEvents::onJoin);
        bus.addListener(LoveProgressEvents::onLeave);
        bus.addListener(LoveProgressEvents::onToss);
        bus.addListener(EventPriority.HIGHEST, LoveProgressEvents::onPickup);
        bus.addListener(LoveProgressEvents::onLogin);
        bus.addListener(LoveProgressEvents::onLogout);
        bus.addListener((PlayerEvent.Clone event) -> SESSIONS.remove(event.getEntity().getUUID()));
        bus.addListener(LoveProgressEvents::onServerTick);
        bus.addListener(LoveProgressEvents::onServerStopped);
    }

    public static boolean isMaid(Entity entity)
    {
        return MAID_TYPE.equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
    }

    public static UUID owner(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().hasUUID(OWNER) ? stack.getTag().getUUID(OWNER) : null;
    }

    public static boolean canUse(ServerPlayer player, ItemStack stack)
    {
        if (!LoveOrbRegistration.isLoveFamily(stack)) return false;
        LoveLedger ledger = LoveLedger.get(player.server);
        UUID owner = owner(stack);
        return !ledger.isBanned(player.getUUID()) && (owner == null
                || owner.equals(player.getUUID()) && !ledger.isBanned(owner));
    }

    public static void bindReward(ServerPlayer player, LivingEntity maid, ItemStack stack)
    {
        LoveLedger ledger = LoveLedger.get(player.server);
        LoveLedger.Bond bond = ledger.bind(player, maid.getUUID(), maid.getName().getString());
        if (bond == null) { stack.setCount(0); return; }
        stamp(stack, player.getUUID(), bond);
        LOADED_MAIDS.put(maid.getUUID(), new WeakReference<>(maid));
        sync(player);
    }

    private static void stamp(ItemStack stack, UUID owner, LoveLedger.Bond bond)
    {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(OWNER, owner);
        tag.putUUID(MAID, bond.maid);
        tag.putString(NAME, bond.maidName);
    }

    private static boolean matches(ItemStack stack, UUID owner, LoveLedger.Bond bond)
    {
        return LoveOrbRegistration.isLoveFamily(stack) && owner.equals(owner(stack)) && stack.hasTag()
                && stack.getTag().hasUUID(MAID) && bond.maid.equals(stack.getTag().getUUID(MAID));
    }

    /** Called by the existing owner equipment refresh; no per-maid inventory scan. */
    public static void observeWorn(ServerPlayer player, List<ItemStack> slots)
    {
        LoveLedger ledger = LoveLedger.get(player.server);
        LoveLedger.Bond bond = ledger.get(player.getUUID());
        Session session = SESSIONS.computeIfAbsent(player.getUUID(), ignored -> new Session());
        boolean previousEquipment = session.equipped;
        session.equipped = false;
        if (player.isAlive())
            for (ItemStack stack : slots) if (canUse(player, stack)) { session.equipped = true; break; }
        session.worn = false;
        if (bond != null && !ledger.isBanned(player.getUUID()) && player.isAlive())
            for (ItemStack stack : slots) if (matches(stack, player.getUUID(), bond)) { session.worn = true; break; }
        if (session.worn && !bond.started) { bond.started = true; ledger.setDirty(); sync(player); }
        if (!session.worn) { session.romance = false; session.nextRomanceCheck = 0; }
        EternalLoveEffects.observe(player, session.worn, bond);
        if (previousEquipment != session.equipped) sync(player);
    }

    /** Physical link status is separate from whether a valid first-maid bond can accumulate progress. */
    public static boolean isWearingLove(ServerPlayer player)
    {
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.equipped && player.isAlive() && !player.isRemoved()
                && !LoveLedger.get(player.server).isBanned(player.getUUID());
    }

    public static void stopWearing(ServerPlayer player)
    {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        boolean changed = session.equipped || session.worn;
        session.equipped = session.worn = session.romance = false;
        session.nextRomanceCheck = 0;
        if (changed) sync(player);
    }

    /** A HUD refresh requests only the sender's state; the client cannot choose a subject or change progress. */
    public static void requestSync(ServerPlayer player)
    {
        if (!LoveOrbRegistration.isAvailable()) return;
        Session session = SESSIONS.computeIfAbsent(player.getUUID(), ignored -> new Session());
        long now = player.server.overworld().getGameTime();
        if (now < session.nextRequest) return;
        session.nextRequest = now + 10;
        LoveSharing.refresh(player);
        // Opening a page needs a response even when its client cache was reset.
        session.lastSent = null;
        sync(player);
    }

    public static boolean isWearingBound(ServerPlayer player)
    {
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.worn && player.isAlive() && !player.isRemoved()
                && !LoveLedger.get(player.server).isBanned(player.getUUID());
    }

    public static void tickPlayer(ServerPlayer player)
    {
        EternalLoveEffects.tick(player);
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        long time = player.server.overworld().getGameTime();
        if (session.lastTick == time) return; // Multiple orbs and event calls never multiply play time.
        session.lastTick = time;
        LoveLedger ledger = LoveLedger.get(player.server);
        LoveLedger.Bond bond = ledger.get(player.getUUID());
        if (bond == null || !isWearingBound(player)) return;
        if (!bond.evolved)
        {
            bond.wornTicks = Math.min(LoveLedger.ETERNITY_TICKS, bond.wornTicks + 1);
            LivingEntity maid = loadedMaid(bond.maid);
            boolean together = maid != null && maid.isAlive() && maid.level() == player.level()
                    && maid instanceof TamableAnimal tame && player.getUUID().equals(tame.getOwnerUUID());
            if (!together) { session.romance = false; session.nextRomanceCheck = 0; }
            else if (time >= session.nextRomanceCheck || !player.blockPosition().equals(session.playerPos)
                    || !maid.blockPosition().equals(session.maidPos) || player.level().dimension() != session.dimension)
            {
                session.nextRomanceCheck = time + 20;
                session.playerPos = player.blockPosition();
                session.maidPos = maid.blockPosition();
                session.dimension = player.level().dimension();
                session.romance = player.level().getBiome(player.blockPosition()).is(Biomes.FLOWER_FOREST)
                        && maid.level().getBiome(maid.blockPosition()).is(Biomes.FLOWER_FOREST);
            }
            if (session.romance) bond.romanceTicks = Math.min(LoveLedger.ROMANCE_TICKS + 1, bond.romanceTicks + 1);
            ledger.setDirty();
            tryEvolve(player, bond, ledger);
        }
        if (time >= session.nextSync) { session.nextSync = time + 20; sync(player); }
    }

    static LivingEntity loadedMaid(UUID id)
    {
        WeakReference<LivingEntity> ref = LOADED_MAIDS.get(id);
        LivingEntity entity = ref == null ? null : ref.get();
        if (entity == null || entity.isRemoved()) { LOADED_MAIDS.remove(id); return null; }
        return entity;
    }

    /** At the existing final-health-damage hook, after armor/absorption/blessing protection. */
    public static void recordDamage(LivingEntity target, DamageSource source, float finalAmount)
    {
        if (target.level().isClientSide || !(finalAmount > 0) || !Float.isFinite(finalAmount)
                || !(source.getEntity() instanceof TamableAnimal maid) || !isMaid(maid)
                || !(maid.getOwner() instanceof ServerPlayer player) || !isWearingBound(player)) return;
        LoveLedger ledger = LoveLedger.get(player.server);
        LoveLedger.Bond bond = ledger.get(player.getUUID());
        if (bond == null || bond.evolved || !bond.maid.equals(maid.getUUID()) || target == maid) return;
        double actual = Math.min(finalAmount, Math.max(0, target.getHealth()));
        if (actual <= 0) return;
        bond.damage = Math.min(LoveLedger.DAMAGE_TARGET, bond.damage + actual);
        ledger.setDirty();
        tryEvolve(player, bond, ledger);
    }

    public static void onChessWin(ServerPlayer player, UUID maid, String game)
    {
        LoveSharing.refresh(player);
        if (!isWearingBound(player)) return;
        LoveLedger ledger = LoveLedger.get(player.server);
        LoveLedger.Bond bond = ledger.get(player.getUUID());
        if (bond == null || bond.evolved || !bond.maid.equals(maid)) return;
        int bit = switch (game) { case "gomoku" -> 1; case "cchess", "xiangqi" -> 2; case "wchess", "chess" -> 4; default -> 0; };
        if (bit == 0 || (bond.games & bit) != 0) return;
        bond.games |= bit;
        ledger.setDirty();
        tryEvolve(player, bond, ledger);
        sync(player);
    }

    public static void onOtherTame(ServerPlayer player, UUID maid)
    {
        LoveLedger ledger = LoveLedger.get(player.server);
        LoveLedger.Bond bond = ledger.get(player.getUUID());
        if (bond != null && !bond.started) LoveSharing.refresh(player);
        if (bond == null || !bond.started || bond.banned || bond.maid.equals(maid)) return;
        bond.resetProgress();
        ledger.setDirty();
        prepareInventory(player);
        LoveSharing.refresh(player);
        player.sendSystemMessage(Component.translatable("message.blessingofpandora.love.jealousy"));
        sync(player);
    }

    public static void handleChat(ServerPlayer player, String message)
    {
        LoveLedger ledger = LoveLedger.get(player.server);
        LoveLedger.Bond bond = ledger.get(player.getUUID());
        if (bond != null) LoveSharing.refresh(player);
        if (bond == null || !bond.started || bond.banned) return;
        LoveVow.Result result = LoveVow.classify(message);
        if (result == LoveVow.Result.BETRAYAL)
        {
            ledger.ban(player.getUUID());
            prepareInventory(player);
            LoveSharing.logout(player);
            Session session = SESSIONS.get(player.getUUID());
            if (session != null) session.worn = session.romance = false;
            for (ItemEntity drop : List.copyOf(DROPS)) updateDrop(drop);
            player.sendSystemMessage(Component.translatable("message.blessingofpandora.love.broken"));
            sync(player);
        }
        else if (result == LoveVow.Result.PROMISE && isWearingBound(player) && !bond.oath)
        {
            bond.oath = true;
            ledger.setDirty();
            tryEvolve(player, bond, ledger);
            sync(player);
        }
    }

    /** Must be called only after LivingEntity.die actually sets its dead flag. */
    public static void recordDeath(LivingEntity maid)
    {
        if (maid.level().isClientSide || !isMaid(maid) || maid.getServer() == null) return;
        LoveLedger ledger = LoveLedger.get(maid.getServer());
        ledger.recordDeath(maid.getUUID());
        if (maid instanceof TamableAnimal tame && tame.getOwner() instanceof ServerPlayer player) sync(player);
    }

    private static void tryEvolve(ServerPlayer player, LoveLedger.Bond bond, LoveLedger ledger)
    {
        if (bond.evolved || !isWearingBound(player) || !bond.complete(ledger.isDead(bond.maid))) return;
        bond.evolved = true;
        ledger.setDirty();
        prepareInventory(player);
        LoveSharing.refresh(player);
        player.sendSystemMessage(Component.translatable("message.blessingofpandora.love.evolved"));
        sync(player);
    }

    /** Walk only the player's actual inventory and Pandora containers, never all world inventories. */
    public static void prepareInventory(ServerPlayer player)
    {
        LoveLedger ledger = LoveLedger.get(player.server);
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++)
        {
            ItemStack old = inventory.getItem(i), next = normalize(old, player.getUUID(), ledger);
            if (next != old) inventory.setItem(i, next);
        }
        ItemStack carried = player.containerMenu.getCarried();
        ItemStack normalized = normalize(carried, player.getUUID(), ledger);
        if (normalized != carried) player.containerMenu.setCarried(normalized);
        var curios = CuriosApi.getCuriosInventory(player).orElse(null);
        if (curios != null)
        {
            // Normalize physical holders, then inherited direct slots. Never add or remove slot types.
            for (var entry : new ArrayList<>(curios.getCurios().entrySet()))
            {
                if (entry.getKey().equals(LoveSharing.SLOT)) continue;
                var stacks = entry.getValue().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++)
                {
                    ItemStack old = stacks.getStackInSlot(i), next = normalize(old, player.getUUID(), ledger);
                    if (next != old) stacks.setStackInSlot(i, next);
                }
            }
            var handler = curios.getCurios().get(LoveSharing.SLOT);
            if (handler != null)
            {
                var stacks = handler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++)
                {
                    ItemStack old = stacks.getStackInSlot(i), next = normalize(old, player.getUUID(), ledger);
                    if (next != old) stacks.setStackInSlot(i, next);
                }
            }
        }
    }

    public static ItemStack normalize(ItemStack stack, UUID holder, LoveLedger ledger)
    {
        if (stack.isEmpty()) return stack;
        if (LoveOrbRegistration.isLoveFamily(stack))
        {
            UUID owner = owner(stack);
            if (holder != null && ledger.isBanned(holder) || owner != null && ledger.isBanned(owner)) return ItemStack.EMPTY;
            if (owner == null) owner = holder;
            LoveLedger.Bond bond = owner == null ? null : ledger.get(owner);
            if (bond == null) return stack; // Unbound creative copies cannot invent a first-maid bond.
            if (LoveOrbRegistration.isEternal(stack) != bond.evolved)
            {
                ItemStack converted = bond.evolved ? LoveOrbRegistration.createEternalStack() : LoveOrbRegistration.createStack();
                if (stack.hasTag()) converted.setTag(stack.getTag().copy());
                stamp(converted, owner, bond);
                return converted;
            }
            if (!matches(stack, owner, bond))
            {
                ItemStack corrected = stack.copy();
                stamp(corrected, owner, bond);
                return corrected;
            }
        }
        else if (stack.getItem() instanceof IPandoraHolder)
        {
            if (!hasFamilyInside(stack)) return stack;
            List<ItemStack> contents = IPandoraHolder.getItems(stack);
            boolean changed = false;
            for (int i = 0; i < contents.size(); i++)
            {
                ItemStack old = contents.get(i);
                // Pandora cannot contain other holders normally; only direct family contents are needed.
                if (!LoveOrbRegistration.isLoveFamily(old)) continue;
                ItemStack next = normalize(old, holder, ledger);
                if (next != old) { contents.set(i, next); changed = true; }
            }
            if (changed)
            {
                ItemStack copy = stack.copy();
                IPandoraHolder.setItems(copy, contents);
                return copy;
            }
        }
        return stack;
    }

    public static void updateDrop(ItemEntity entity)
    {
        if (entity.level().isClientSide || entity.isRemoved() || entity.getServer() == null) return;
        UUID holder = entity.getOwner() instanceof ServerPlayer player ? player.getUUID() : null;
        ItemStack original = entity.getItem();
        ItemStack next = normalize(original, holder, LoveLedger.get(entity.getServer()));
        if (next.isEmpty()) entity.discard();
        else if (next != original) entity.setItem(next);
    }

    public static void sync(ServerPlayer player)
    {
        Session session = SESSIONS.computeIfAbsent(player.getUUID(), ignored -> new Session());
        LoveProgressS2CPacket packet = snapshot(player);
        if (packet.equals(session.lastSent)) return;
        session.lastSent = packet;
        BlessingNetwork.sendToPlayer(player, packet);
    }

    public static LoveProgressS2CPacket snapshot(ServerPlayer player)
    {
        return LoveProgressS2CPacket.capture(player.getUUID(), LoveLedger.get(player.server), isWearingLove(player));
    }

    private static boolean hasFamilyInside(ItemStack stack)
    {
        if (!stack.hasTag()) return false;
        var contents = stack.getTag().getList("PandoraCharms", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < contents.size(); i++)
        {
            String id = contents.getCompound(i).getString("id");
            if (id.equals("blessingofpandora:love_orb") || id.equals("blessingofpandora:eternal_love_orb")) return true;
        }
        return false;
    }

    private static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) tickPlayer(player);
    }
    private static void onChat(ServerChatEvent event) { if (!event.isCanceled()) handleChat(event.getPlayer(), event.getRawText()); }
    private static void onJoin(EntityJoinLevelEvent event)
    {
        if (event.getLevel().isClientSide()) return;
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity living && isMaid(entity))
        {
            LOADED_MAIDS.put(entity.getUUID(), new WeakReference<>(living));
            EternalLoveEffects.maidJoined(living);
        }
        if (entity instanceof ItemEntity drop && (LoveOrbRegistration.isLoveFamily(drop.getItem()) || drop.getItem().getItem() instanceof IPandoraHolder))
        {
            DROPS.add(drop);
            updateDrop(drop);
            if (drop.isRemoved()) { DROPS.remove(drop); event.setCanceled(true); }
        }
    }
    private static void onLeave(EntityLeaveLevelEvent event)
    {
        if (event.getLevel().isClientSide()) return;
        Entity entity = event.getEntity();
        if (isMaid(entity))
        {
            WeakReference<LivingEntity> current = LOADED_MAIDS.get(entity.getUUID());
            if (current != null && current.get() == entity) LOADED_MAIDS.remove(entity.getUUID());
            if (entity instanceof LivingEntity living) EternalLoveEffects.maidLeft(living);
        }
        if (entity instanceof ItemEntity item) DROPS.remove(item);
    }
    private static void onToss(ItemTossEvent event)
    {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        ItemStack next = normalize(event.getEntity().getItem(), player.getUUID(), LoveLedger.get(player.server));
        if (next.isEmpty()) { event.getEntity().discard(); event.setCanceled(true); }
        else if (next != event.getEntity().getItem()) event.getEntity().setItem(next);
    }
    private static void onPickup(EntityItemPickupEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack next = normalize(event.getItem().getItem(), player.getUUID(), LoveLedger.get(player.server));
        if (next.isEmpty()) { event.getItem().discard(); event.setCanceled(true); }
        else if (next != event.getItem().getItem()) event.getItem().setItem(next);
    }
    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player) { prepareInventory(player); sync(player); }
    }
    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) { SESSIONS.remove(event.getEntity().getUUID()); }
    private static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && event.getServer().getTickCount() % 20 == 0)
            for (ItemEntity drop : List.copyOf(DROPS)) { if (drop.isRemoved()) DROPS.remove(drop); else updateDrop(drop); }
    }
    private static void onServerStopped(ServerStoppedEvent event)
    {
        EternalLoveEffects.serverStopped();
        SESSIONS.clear(); LOADED_MAIDS.clear(); DROPS.clear();
    }
}
