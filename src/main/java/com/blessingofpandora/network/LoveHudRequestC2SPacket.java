package com.blessingofpandora.network;

import com.blessingofpandora.compat.love.LoveProgressEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Read-only, sender-scoped refresh. No UUID, item NBT or progression supplied by the client. */
public record LoveHudRequestC2SPacket()
{
    public static void encode(LoveHudRequestC2SPacket packet, FriendlyByteBuf buffer) {}
    public static LoveHudRequestC2SPacket decode(FriendlyByteBuf buffer) { return new LoveHudRequestC2SPacket(); }
    public static void handle(LoveHudRequestC2SPacket packet, Supplier<NetworkEvent.Context> supplier)
    {
        var context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) LoveProgressEvents.requestSync(context.getSender());
        });
        context.setPacketHandled(true);
    }
}
