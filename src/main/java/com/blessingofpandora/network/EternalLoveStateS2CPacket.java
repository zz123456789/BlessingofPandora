package com.blessingofpandora.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

/** Only equipment/visibility transitions are synchronized; the shader remains entirely client-side. */
public record EternalLoveStateS2CPacket(int entityId, UUID entityUuid, boolean active)
{
    public static void encode(EternalLoveStateS2CPacket value, FriendlyByteBuf buf)
    {
        buf.writeVarInt(value.entityId); buf.writeUUID(value.entityUuid); buf.writeBoolean(value.active);
    }
    public static EternalLoveStateS2CPacket decode(FriendlyByteBuf buf)
    {
        return new EternalLoveStateS2CPacket(buf.readVarInt(), buf.readUUID(), buf.readBoolean());
    }
    public static void handle(EternalLoveStateS2CPacket value, Supplier<NetworkEvent.Context> supplier)
    {
        var context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient())
                com.blessingofpandora.client.EternalLoveClient.receive(value);
        });
        context.setPacketHandled(true);
    }
}
