package com.blessingofpandora.network;

import com.blessingofpandora.BlessingDefinitions;
import com.blessingofpandora.ReversalActions;
import com.blessingofpandora.ReversalAuthorization;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class ReversalActionC2SPacket
{
    public static final int CONVERT_TO_BLESSING = 0;
    public static final int REVERT_TO_CURSE = 1;

    private final UUID playerId;
    private final String definitionId;
    private final int action;

    public ReversalActionC2SPacket(
            UUID playerId, BlessingDefinitions definition, int action)
    {
        this(playerId, definition.id(), action);
    }

    private ReversalActionC2SPacket(
            UUID playerId, String definitionId, int action)
    {
        this.playerId = playerId;
        this.definitionId = definitionId;
        this.action = action;
    }

    public static void encode(
            ReversalActionC2SPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeUUID(message.playerId);
        buffer.writeUtf(message.definitionId);
        buffer.writeVarInt(message.action);
    }

    public static ReversalActionC2SPacket decode(FriendlyByteBuf buffer)
    {
        return new ReversalActionC2SPacket(
                buffer.readUUID(),
                buffer.readUtf(),
                buffer.readVarInt());
    }

    public static void handle(
            ReversalActionC2SPacket message,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !message.playerId.equals(player.getUUID())
                    || !ReversalAuthorization.canUseDebugControl(player))
            {
                return;
            }
            BlessingDefinitions definition = findDefinition(message.definitionId);
            if (definition == null)
            {
                return;
            }
            if (message.action == CONVERT_TO_BLESSING)
            {
                ReversalActions.convertWornCurse(player, definition);
            }
            else if (message.action == REVERT_TO_CURSE)
            {
                ReversalActions.revertWornBlessing(player, definition);
            }
        });
        context.setPacketHandled(true);
    }

    private static BlessingDefinitions findDefinition(String id)
    {
        for (BlessingDefinitions definition : BlessingDefinitions.values())
        {
            if (definition.id().equals(id))
            {
                return definition;
            }
        }
        return null;
    }
}
