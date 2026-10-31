package com.blessingofpandora.network;

import com.blessingofpandora.BlessingReversals;
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
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && message.playerId.equals(player.getUUID()))
            {
                BlessingNetwork.syncConfig(player);
                BlessingReversals.syncReversalState(player);
            }
        });
        context.setPacketHandled(true);
    }
}
