package com.blessingofpandora.client;

import com.blessingofpandora.network.BlessingNetwork;
import com.blessingofpandora.network.ReversalRequestC2SPacket;
import com.blessingofpandora.BlessingDefinitions;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public final class ReversalHudClient
{
    private ReversalHudClient()
    {
    }

    public static void toggle()
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ReversalHudScreen)
        {
            minecraft.setScreen(null);
        }
        else
        {
            openGlobal();
        }
    }

    public static void openGlobal()
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ReversalHudScreen hud
                && !hud.isPermissionMode())
        {
            return;
        }
        minecraft.setScreen(new ReversalHudScreen());
        sendPermissionRequest(minecraft);
    }

    public static void toggleAction()
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ReversalActionHudScreen)
        {
            minecraft.setScreen(null);
        }
        else
        {
            minecraft.setScreen(new ReversalActionHudScreen());
        }
    }

    public static void openPermission(BlessingDefinitions definition, InteractionHand inspectedHand)
    {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new ReversalHudScreen(definition, inspectedHand));
        sendPermissionRequest(minecraft);
    }

    private static void sendPermissionRequest(Minecraft minecraft)
    {
        if (minecraft.player != null)
        {
            UUID playerId = minecraft.player.getUUID();
            BlessingNetwork.CHANNEL.sendToServer(
                    new ReversalRequestC2SPacket(playerId));
        }
    }
}
