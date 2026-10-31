package com.blessingofpandora.network;

import com.blessingofpandora.ReversalSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ReversalConfigS2CPacket(ReversalSettings settings)
{
    public static void encode(ReversalConfigS2CPacket message, FriendlyByteBuf buffer)
    {
        message.settings.encode(buffer);
    }

    public static ReversalConfigS2CPacket decode(FriendlyByteBuf buffer)
    {
        return new ReversalConfigS2CPacket(ReversalSettings.decode(buffer));
    }

    public static void handle(ReversalConfigS2CPacket message, Supplier<NetworkEvent.Context> supplier)
    {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ReversalSettings.setServerSettings(message.settings));
        context.setPacketHandled(true);
    }
}
