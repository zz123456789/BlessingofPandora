package com.blessingofpandora.network;

import com.blessingofpandora.BlessingDefinitions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class ReversalToggleC2SPacket
{
    private static final BlessingDefinitions[] DEFINITIONS = BlessingDefinitions.values();
    private final UUID playerId;
    private final String definitionId;

    public ReversalToggleC2SPacket(UUID playerId, BlessingDefinitions definition)
    {
        this(playerId, definition.id());
    }

    private ReversalToggleC2SPacket(UUID playerId, String definitionId)
    {
        this.playerId = playerId;
        this.definitionId = definitionId;
    }

    public static void encode(ReversalToggleC2SPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeUUID(message.playerId);
        buffer.writeUtf(message.definitionId);
    }

    public static ReversalToggleC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new ReversalToggleC2SPacket(
                buffer.readUUID(), buffer.readUtf(32));
    }

    public static void handle(
            ReversalToggleC2SPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        ReversalClientRequests.toggle(context.getSender(), message.playerId,
                findDefinition(message.definitionId));
        context.setPacketHandled(true);
    }

    private static BlessingDefinitions findDefinition(String id)
    {
        for (BlessingDefinitions definition : DEFINITIONS)
        {
            if (definition.id().equals(id))
            {
                return definition;
            }
        }
        return null;
    }
}
