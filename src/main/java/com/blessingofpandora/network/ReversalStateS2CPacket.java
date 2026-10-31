package com.blessingofpandora.network;

import com.blessingofpandora.client.ClientReversalPermission;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class ReversalStateS2CPacket
{
    private final UUID playerId;
    private final boolean[] allowed;
    private final int sixthCurseState;
    private final long sixthAbsorbedExperience;
    private final long firstKillFailures;
    private final long fifthKillFailures;

    public ReversalStateS2CPacket(
            UUID playerId,
            boolean[] allowed,
            int sixthCurseState,
            long sixthAbsorbedExperience,
            long firstKillFailures,
            long fifthKillFailures)
    {
        this.playerId = playerId;
        this.allowed = allowed;
        this.sixthCurseState = sixthCurseState;
        this.sixthAbsorbedExperience = sixthAbsorbedExperience;
        this.firstKillFailures = Math.max(0L, firstKillFailures);
        this.fifthKillFailures = Math.max(0L, fifthKillFailures);
    }

    public static void encode(ReversalStateS2CPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeUUID(message.playerId);
        buffer.writeVarInt(message.allowed.length);
        for (boolean value : message.allowed)
        {
            buffer.writeBoolean(value);
        }
        buffer.writeVarInt(message.sixthCurseState);
        buffer.writeLong(message.sixthAbsorbedExperience);
        buffer.writeLong(message.firstKillFailures);
        buffer.writeLong(message.fifthKillFailures);
    }

    public static ReversalStateS2CPacket decode(FriendlyByteBuf buffer)
    {
        UUID playerId = buffer.readUUID();
        int size = buffer.readVarInt();
        if (size != 7)
        {
            throw new IllegalArgumentException("Invalid reversal permission count: " + size);
        }
        boolean[] allowed = new boolean[size];
        for (int i = 0; i < size; i++)
        {
            allowed[i] = buffer.readBoolean();
        }
        int sixthCurseState = buffer.readVarInt();
        long sixthAbsorbedExperience = buffer.readLong();
        long firstKillFailures = buffer.readLong();
        long fifthKillFailures = buffer.readLong();
        return new ReversalStateS2CPacket(
                playerId, allowed, sixthCurseState, sixthAbsorbedExperience,
                firstKillFailures, fifthKillFailures);
    }

    public static void handle(
            ReversalStateS2CPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient())
            {
                ClientReversalPermission.setStates(
                        message.allowed,
                        message.sixthCurseState,
                        message.sixthAbsorbedExperience,
                        message.firstKillFailures,
                        message.fifthKillFailures);
            }
        });
        context.setPacketHandled(true);
    }
}
