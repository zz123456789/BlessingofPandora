package com.blessingofpandora.network;

import com.blessingofpandora.BlessingOfPandora;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class BlessingNetwork
{
    // 1.7.3 adds authoritative Love progression and equipped eternal-ability state.
    private static final String PROTOCOL_VERSION = "7";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BlessingOfPandora.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int nextId = 0;

    private BlessingNetwork()
    {
    }

    public static void register()
    {
        CHANNEL.messageBuilder(LoveHudRequestC2SPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(LoveHudRequestC2SPacket::encode)
                .decoder(LoveHudRequestC2SPacket::decode)
                .consumerMainThread(LoveHudRequestC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(EternalLoveStateS2CPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EternalLoveStateS2CPacket::encode)
                .decoder(EternalLoveStateS2CPacket::decode)
                .consumerMainThread(EternalLoveStateS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(LoveProgressS2CPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LoveProgressS2CPacket::encode)
                .decoder(LoveProgressS2CPacket::decode)
                .consumerMainThread(LoveProgressS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(ReversalConfigS2CPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ReversalConfigS2CPacket::encode)
                .decoder(ReversalConfigS2CPacket::decode)
                .consumerMainThread(ReversalConfigS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(ReversalToggleC2SPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ReversalToggleC2SPacket::encode)
                .decoder(ReversalToggleC2SPacket::decode)
                .consumerNetworkThread(ReversalToggleC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(ReversalStateS2CPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ReversalStateS2CPacket::encode)
                .decoder(ReversalStateS2CPacket::decode)
                .consumerMainThread(ReversalStateS2CPacket::handle)
                .add();
        CHANNEL.messageBuilder(ReversalRequestC2SPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ReversalRequestC2SPacket::encode)
                .decoder(ReversalRequestC2SPacket::decode)
                .consumerNetworkThread(ReversalRequestC2SPacket::handle)
                .add();
        CHANNEL.messageBuilder(ReversalActionC2SPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ReversalActionC2SPacket::encode)
                .decoder(ReversalActionC2SPacket::decode)
                .consumerMainThread(ReversalActionC2SPacket::handle)
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, Object message)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void syncConfig(ServerPlayer player)
    {
        sendToPlayer(player, new ReversalConfigS2CPacket(
                com.blessingofpandora.ReversalSettings.capture()));
    }
}
