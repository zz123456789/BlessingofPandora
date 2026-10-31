package com.blessingofpandora.network;

import com.blessingofpandora.compat.love.LoveLedger;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public record LoveProgressS2CPacket(UUID owner, UUID maid, String maidName, long time, long romance,
                                   double damage, int games, boolean oath, boolean dead,
                                   boolean banned, boolean evolved, boolean started, boolean worn)
{
    public static LoveProgressS2CPacket capture(UUID owner, LoveLedger ledger)
    {
        return capture(owner, ledger, false);
    }
    public static LoveProgressS2CPacket capture(UUID owner, LoveLedger ledger, boolean worn)
    {
        LoveLedger.Bond bond = ledger.get(owner);
        if (bond == null) return new LoveProgressS2CPacket(owner, null, "", 0, 0, 0, 0,
                false, false, ledger.isBanned(owner), false, false, worn);
        String name = bond.maidName.length() > 256 ? bond.maidName.substring(0, 256) : bond.maidName;
        return new LoveProgressS2CPacket(owner, bond.maid, name, bond.wornTicks, bond.romanceTicks,
                bond.damage, bond.games, bond.oath, ledger.isDead(bond.maid), ledger.isBanned(owner), bond.evolved, bond.started, worn);
    }
    public static void encode(LoveProgressS2CPacket value, FriendlyByteBuf buf)
    {
        buf.writeUUID(value.owner);
        buf.writeBoolean(value.maid != null);
        if (value.maid != null) buf.writeUUID(value.maid);
        buf.writeUtf(value.maidName, 256);
        buf.writeLong(value.time); buf.writeLong(value.romance); buf.writeDouble(value.damage);
        buf.writeVarInt(value.games);
        buf.writeBoolean(value.oath); buf.writeBoolean(value.dead); buf.writeBoolean(value.banned);
        buf.writeBoolean(value.evolved); buf.writeBoolean(value.started);
        buf.writeBoolean(value.worn);
    }
    public static LoveProgressS2CPacket decode(FriendlyByteBuf buf)
    {
        UUID owner = buf.readUUID(), maid = buf.readBoolean() ? buf.readUUID() : null;
        return new LoveProgressS2CPacket(owner, maid, buf.readUtf(256), buf.readLong(), buf.readLong(),
                buf.readDouble(), buf.readVarInt() & 7, buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }
    public static void handle(LoveProgressS2CPacket value, Supplier<NetworkEvent.Context> supplier)
    {
        var context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient())
                com.blessingofpandora.client.LoveProgressClient.set(value);
        });
        context.setPacketHandled(true);
    }
}
