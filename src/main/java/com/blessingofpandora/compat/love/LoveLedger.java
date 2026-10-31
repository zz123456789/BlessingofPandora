package com.blessingofpandora.compat.love;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** World-authoritative bond and sanctions. Item NBT never decides progress or eligibility. */
public final class LoveLedger extends SavedData
{
    public static final long ETERNITY_TICKS = 24L * 60 * 60 * 20;
    public static final long ROMANCE_TICKS = 30L * 60 * 20;
    public static final double DAMAGE_TARGET = 1_314_520;
    private static final String FILE = "blessingofpandora_love_bonds";
    private final Map<UUID, Bond> bonds = new HashMap<>();
    private final Set<UUID> deadMaids = new HashSet<>();
    private final Set<UUID> bannedPlayers = new HashSet<>();

    public static final class Bond
    {
        public final UUID maid;
        public final String maidName;
        public long wornTicks, romanceTicks;
        public double damage;
        public int games;
        public boolean oath, started, banned, evolved;

        public Bond(UUID maid, String maidName)
        {
            this.maid = maid;
            this.maidName = maidName;
        }

        public boolean complete(boolean maidHasDied)
        {
            return !banned && !maidHasDied && wornTicks >= ETERNITY_TICKS
                    && romanceTicks > ROMANCE_TICKS && damage >= DAMAGE_TARGET && games == 7 && oath;
        }

        /** History, identity and irreversible sanctions deliberately survive jealousy resets. */
        public void resetProgress()
        {
            wornTicks = romanceTicks = 0;
            damage = 0;
            games = 0;
            oath = evolved = false;
        }
    }

    public static LoveLedger get(MinecraftServer server)
    {
        return server.overworld().getDataStorage().computeIfAbsent(LoveLedger::load, LoveLedger::new, FILE);
    }

    public Bond get(UUID player) { return bonds.get(player); }
    public boolean isDead(UUID maid) { return deadMaids.contains(maid); }
    public boolean isBanned(UUID player) { return bannedPlayers.contains(player); }

    public Bond bind(ServerPlayer player, UUID maid, String name)
    {
        if (isBanned(player.getUUID())) return null;
        Bond existing = bonds.get(player.getUUID());
        if (existing != null) return existing;
        Bond bond = new Bond(maid, name);
        bonds.put(player.getUUID(), bond);
        setDirty();
        return bond;
    }

    public void recordDeath(UUID maid)
    {
        if (deadMaids.add(maid)) setDirty();
    }

    public void ban(UUID player)
    {
        bannedPlayers.add(player);
        Bond bond = bonds.get(player);
        if (bond != null) { bond.banned = true; bond.resetProgress(); }
        setDirty();
    }

    public static LoveLedger load(CompoundTag tag)
    {
        LoveLedger ledger = new LoveLedger();
        ListTag entries = tag.getList("bonds", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++)
        {
            CompoundTag entry = entries.getCompound(i);
            if (!entry.hasUUID("owner") || !entry.hasUUID("maid")) continue;
            Bond bond = new Bond(entry.getUUID("maid"), entry.getString("name"));
            bond.wornTicks = Math.max(0, Math.min(ETERNITY_TICKS, entry.getLong("time")));
            bond.romanceTicks = Math.max(0, Math.min(ROMANCE_TICKS + 1, entry.getLong("romance")));
            bond.damage = entry.getDouble("damage");
            if (!Double.isFinite(bond.damage) || bond.damage < 0) bond.damage = 0;
            bond.damage = Math.min(DAMAGE_TARGET, bond.damage);
            bond.games = entry.getInt("games") & 7;
            bond.oath = entry.getBoolean("oath");
            bond.started = entry.getBoolean("started");
            bond.banned = entry.getBoolean("banned");
            bond.evolved = entry.getBoolean("evolved") && !bond.banned;
            UUID owner = entry.getUUID("owner");
            ledger.bonds.put(owner, bond);
            if (bond.banned) ledger.bannedPlayers.add(owner);
        }
        readIds(tag.getList("deaths", Tag.TAG_COMPOUND), ledger.deadMaids);
        readIds(tag.getList("bans", Tag.TAG_COMPOUND), ledger.bannedPlayers);
        return ledger;
    }

    private static void readIds(ListTag list, Set<UUID> result)
    {
        for (int i = 0; i < list.size(); i++)
            if (list.getCompound(i).hasUUID("id")) result.add(list.getCompound(i).getUUID("id"));
    }

    @Override public CompoundTag save(CompoundTag tag)
    {
        ListTag entries = new ListTag();
        bonds.forEach((owner, bond) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("owner", owner);
            entry.putUUID("maid", bond.maid);
            entry.putString("name", bond.maidName);
            entry.putLong("time", bond.wornTicks);
            entry.putLong("romance", bond.romanceTicks);
            entry.putDouble("damage", bond.damage);
            entry.putInt("games", bond.games);
            entry.putBoolean("oath", bond.oath);
            entry.putBoolean("started", bond.started);
            entry.putBoolean("banned", bond.banned);
            entry.putBoolean("evolved", bond.evolved);
            entries.add(entry);
        });
        tag.put("bonds", entries);
        tag.put("deaths", writeIds(deadMaids));
        tag.put("bans", writeIds(bannedPlayers));
        return tag;
    }

    private static ListTag writeIds(Set<UUID> ids)
    {
        ListTag result = new ListTag();
        for (UUID id : ids) { CompoundTag tag = new CompoundTag(); tag.putUUID("id", id); result.add(tag); }
        return result;
    }
}
