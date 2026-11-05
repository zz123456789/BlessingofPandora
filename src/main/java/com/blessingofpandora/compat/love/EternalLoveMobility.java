package com.blessingofpandora.compat.love;

import com.blessingofpandora.BlessingOfPandora;
import com.blessingofpandora.compat.maid.EternalLoveMaidFlight;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Owner-scoped flight and bounded chunk tickets. No inventory/world scans or reflection. */
public final class EternalLoveMobility
{
    public static final int OLD_CHUNK_LIFETIME = 600;
    // At most one movement per game tick and a 600-tick window: <= 600 old tickets plus one current.
    public static final int MAX_OLD_CHUNKS = OLD_CHUNK_LIFETIME;
    private static final String FLIGHT_GRANTED = "blessingofpandora:eternal_flight_granted";
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static boolean registered, validationRegistered;

    private record Place(ServerLevel level, long chunk) {}
    private static final class State
    {
        final ServerPlayer player;
        final UUID maid;
        Place current;
        final LinkedHashMap<Place, Long> old = new LinkedHashMap<>();
        WeakReference<LivingEntity> entity = new WeakReference<>(null);
        long nextRecovery, recoveryUntil, nextExpiry = Long.MAX_VALUE, lastTick = Long.MIN_VALUE;
        Place recovery;
        State(ServerPlayer player, UUID maid)
        {
            this.player = player;
            this.maid = maid;
        }
    }

    private EternalLoveMobility() {}

    /** Called on the common setup work queue before levels load. */
    public static void register()
    {
        if (registered) return;
        registered = true;
        registerTicketValidation();
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(EternalLoveMobility::onEntityJoin);
        bus.addListener((ServerStoppingEvent event) -> serverStopped());
    }

    /** Also invoke without the optional maid mod, so uninstalling it cannot retain stale tickets. */
    public static void registerTicketValidation()
    {
        if (validationRegistered) return;
        validationRegistered = true;
        ForgeChunkManager.setForcedChunkLoadingCallback(BlessingOfPandora.MODID, (level, helper) -> {
            // These tickets are session-scoped: nobody is equipped/online while the level loads.
            // Remove saved tickets before Forge reinstates them; online owners rebuild their own.
            for (UUID owner : helper.getEntityTickets().keySet()) helper.removeAllTickets(owner);
        });
        // Borrowed flight may also remain in a player save after a crash followed by uninstalling Maid.
        MinecraftForge.EVENT_BUS.addListener(EternalLoveMobility::onLogin);
    }

    public static void tick(ServerPlayer player, LivingEntity candidate)
    {
        LoveLedger.Bond bond = LoveLedger.get(player.server).get(player.getUUID());
        if (bond == null || !bond.evolved || bond.banned || !player.isAlive()) { clear(player); return; }
        State state = STATES.get(player.getUUID());
        if (state != null && (state.player != player || !state.maid.equals(bond.maid)))
        { clear(state.player); state = null; }
        if (state == null) { state = new State(player, bond.maid); STATES.put(player.getUUID(), state); }
        grantFlight(state);
        long now = player.server.overworld().getGameTime();
        if (state.lastTick == now) return;
        state.lastTick = now;
        expireOld(state, now);
        LivingEntity maid = valid(candidate, state) ? candidate : recover(state, now);
        LivingEntity previous = state.entity.get();
        if (previous != maid && previous != null) EternalLoveMaidFlight.clear(previous);
        if (previous != maid) state.entity = new WeakReference<>(maid);
        if (maid == null)
        {
            if (state.current != null) leaveCurrent(state, now);
            return;
        }
        Place recovery = state.recovery;
        state.recovery = null;
        // Maid dimension changes replace the entity; the helper returns the actual destination entity.
        LivingEntity moved = EternalLoveMaidFlight.tick(player, maid);
        if (moved != null && valid(moved, state)) { maid = moved; state.entity = new WeakReference<>(maid); }
        ServerLevel maidLevel = (ServerLevel) maid.level();
        long maidChunk = maid.chunkPosition().toLong();
        Place place = state.current != null && state.current.level == maidLevel && state.current.chunk == maidChunk
                ? state.current : new Place(maidLevel, maidChunk);
        if (recovery != null && !recovery.equals(place) && !recovery.equals(state.current) && !state.old.containsKey(recovery))
            force(state, recovery, false);
        boolean changed = !place.equals(state.current);
        keepCurrent(state, place, now);
        if (changed) Locations.get(player.server).remember(player.getUUID(), state.maid, place);
    }

    private static boolean valid(LivingEntity maid, State state)
    {
        return maid != null && maid.isAlive() && !maid.isRemoved() && maid.level() instanceof ServerLevel
                && state.maid.equals(maid.getUUID()) && maid instanceof TamableAnimal tame
                && state.player.getUUID().equals(tame.getOwnerUUID());
    }

    /** A single remembered chunk is tried, never every dimension/chunk/entity. */
    private static LivingEntity recover(State state, long now)
    {
        LivingEntity known = state.entity.get();
        if (valid(known, state)) return known;
        if (state.recovery != null)
        {
            var found = state.recovery.level.getEntity(state.maid);
            if (found instanceof LivingEntity living && valid(living, state)) return living;
            if (now < state.recoveryUntil) return null;
            if (!state.recovery.equals(state.current) && !state.old.containsKey(state.recovery))
                force(state, state.recovery, false);
            state.recovery = null;
            state.nextRecovery = now + 1200;
        }
        if (now < state.nextRecovery) return null;
        Location location = Locations.get(state.player.server).entries.get(state.player.getUUID());
        if (location == null || !state.maid.equals(location.maid)) return null;
        ServerLevel level = state.player.server.getLevel(location.dimension);
        if (level == null) { state.nextRecovery = now + 1200; return null; }
        state.recovery = new Place(level, location.chunk);
        force(state, state.recovery, true);
        state.recoveryUntil = now + 100;
        var found = level.getEntity(state.maid);
        return found instanceof LivingEntity living && valid(living, state) ? living : null;
    }

    private static void grantFlight(State state)
    {
        if (EternalLoveFlight.acquire(state.player.getAbilities())) syncAbilities(state);
    }

    private static void syncAbilities(State state)
    { state.player.onUpdateAbilities(); }

    private static void keepCurrent(State state, Place place, long now)
    {
        if (place.equals(state.current)) return;
        leaveCurrent(state, now);
        boolean already = state.old.remove(place) != null || place.equals(state.recovery);
        state.current = place;
        if (!already) force(state, place, true);
    }

    private static void leaveCurrent(State state, long now)
    {
        if (state.current == null) return;
        state.old.remove(state.current);
        state.old.put(state.current, now + OLD_CHUNK_LIFETIME);
        state.nextExpiry = Math.min(state.nextExpiry, now + OLD_CHUNK_LIFETIME);
        state.current = null;
    }

    private static void expireOld(State state, long now)
    {
        if (state.old.isEmpty() || now < state.nextExpiry) return;
        state.nextExpiry = Long.MAX_VALUE;
        Iterator<Map.Entry<Place, Long>> iterator = state.old.entrySet().iterator();
        while (iterator.hasNext())
        {
            var entry = iterator.next();
            if (now < entry.getValue()) { state.nextExpiry = entry.getValue(); break; }
            iterator.remove();
            if (!entry.getKey().equals(state.current) && !entry.getKey().equals(state.recovery))
                force(state, entry.getKey(), false);
        }
    }

    private static void force(State state, Place place, boolean add)
    {
        ChunkPos chunk = new ChunkPos(place.chunk);
        ForgeChunkManager.forceChunk(place.level, BlessingOfPandora.MODID, state.player.getUUID(),
                chunk.x, chunk.z, add, true);
    }

    public static void clear(ServerPlayer player)
    {
        State state = STATES.get(player.getUUID());
        if (state == null || state.player != player) return;
        STATES.remove(player.getUUID());
        LivingEntity maid = state.entity.get();
        if (maid != null) EternalLoveMaidFlight.clear(maid);
        if (state.current != null) force(state, state.current, false);
        for (Place place : state.old.keySet()) force(state, place, false);
        if (state.recovery != null) force(state, state.recovery, false);
        var abilities = player.getAbilities();
        EternalLoveFlight.release(abilities);
        if (!player.isCreative() && !player.isSpectator() && !abilities.mayfly)
        {
            abilities.flying = false;
            player.fallDistance = 0;
        }
        syncAbilities(state);
        player.getPersistentData().remove(FLIGHT_GRANTED);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !player.getPersistentData().getBoolean(FLIGHT_GRANTED)) return;
        // Legacy versions wrote borrowed flight into the vanilla field. New
        // independent leases are never persisted into mayfly and need no such cleanup.
        if (!player.isCreative() && !player.isSpectator())
        { player.getAbilities().mayfly = false; player.getAbilities().flying = false; player.onUpdateAbilities(); }
        player.getPersistentData().remove(FLIGHT_GRANTED);
    }

    private static void onEntityJoin(EntityJoinLevelEvent event)
    {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living)
            EternalLoveMaidFlight.restoreSavedGravity(living);
    }

    public static void serverStopped()
    {
        for (State state : java.util.List.copyOf(STATES.values())) clear(state.player);
        STATES.clear();
        EternalLoveFlight.clear();
        if (registered) EternalLoveMaidFlight.clearAll();
    }

    private record Location(UUID maid, ResourceKey<Level> dimension, long chunk) {}
    /** Last known coordinates, not persistent force tickets or entity references. */
    public static final class Locations extends SavedData
    {
        private static final String FILE = "blessingofpandora_eternal_maid_locations";
        private final Map<UUID, Location> entries = new HashMap<>();
        private static Locations get(MinecraftServer server)
        { return server.overworld().getDataStorage().computeIfAbsent(Locations::load, Locations::new, FILE); }
        private void remember(UUID owner, UUID maid, Place place)
        {
            Location next = new Location(maid, place.level.dimension(), place.chunk);
            if (!next.equals(entries.put(owner, next))) setDirty();
        }
        public static Locations load(CompoundTag tag)
        {
            Locations data = new Locations();
            ListTag entries = tag.getList("Locations", Tag.TAG_COMPOUND);
            for (int i = 0; i < entries.size(); i++)
            {
                CompoundTag entry = entries.getCompound(i);
                ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("Dimension"));
                if (dimension != null && entry.hasUUID("Owner") && entry.hasUUID("Maid"))
                    data.entries.put(entry.getUUID("Owner"), new Location(entry.getUUID("Maid"),
                            ResourceKey.create(Registries.DIMENSION, dimension), entry.getLong("Chunk")));
            }
            return data;
        }
        @Override public CompoundTag save(CompoundTag tag)
        {
            ListTag values = new ListTag();
            entries.forEach((owner, location) -> {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("Owner", owner); entry.putUUID("Maid", location.maid);
                entry.putString("Dimension", location.dimension.location().toString());
                entry.putLong("Chunk", location.chunk); values.add(entry);
            });
            tag.put("Locations", values);
            return tag;
        }
    }
}
