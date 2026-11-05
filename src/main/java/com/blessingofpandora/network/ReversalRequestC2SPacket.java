package com.blessingofpandora.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class ReversalRequestC2SPacket
{
    private final UUID playerId;

    public ReversalRequestC2SPacket(UUID playerId)
    {
        this.playerId = playerId;
    }

    public static void encode(ReversalRequestC2SPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeUUID(message.playerId);
    }

    public static ReversalRequestC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new ReversalRequestC2SPacket(buffer.readUUID());
    }

    public static void handle(
            ReversalRequestC2SPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        ReversalClientRequests.request(context.getSender(), message.playerId);
        context.setPacketHandled(true);
    }
}
