package com.blessingofpandora.client;

import com.blessingofpandora.BlessingDefinitions;
import com.blessingofpandora.ReversalDescriptions;
import com.blessingofpandora.ReversalAuthorization;
import com.blessingofpandora.BlessingOwners;
import com.blessingofpandora.network.BlessingNetwork;
import com.blessingofpandora.network.ReversalActionC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ReversalActionHudScreen extends ReversalHudScreenBase
{
    public ReversalActionHudScreen()
    {
        super(
                Component.translatable("blessingofpandora.hud.title.action"),
                BlessingDefinitions.values());
    }

    @Override
    protected Component condition(int index)
    {
        BlessingDefinitions definition = definitions[index];
        Component base;
        if (definition == BlessingDefinitions.REQUIEM)
        {
            return ReversalDescriptions.condition(
                    definition, ReversalLiveData.liveRequiemStep());
        }
        else if (definition == BlessingDefinitions.LIBERATION)
        {
            base = ReversalDescriptions.condition(
                    definition, ReversalLiveData.liveLiberationStep());
        }
        else
        {
            base = ReversalDescriptions.condition(definition);
        }
        return ReversalLiveData.appendLiveState(base, definition);
    }

    @Override
    protected Component buttonLabel(int index)
    {
        int state = ReversalLiveData.wornState(definitions[index]);
        return Component.translatable(
                switch (state)
                {
                    case ReversalLiveData.WEARING_BLESSING ->
                            "blessingofpandora.hud.button.convert_to_curse";
                    case ReversalLiveData.WEARING_CURSE ->
                            "blessingofpandora.hud.button.convert_to_blessing";
                    default -> "blessingofpandora.hud.button.not_worn";
                });
    }

    @Override
    protected boolean buttonEnabled(int index)
    {
        var player = Minecraft.getInstance().player;
        if (player == null || !ReversalAuthorization.canUseDebugControl(player)) return false;
        int state = ReversalLiveData.wornState(definitions[index]);
        if (state == ReversalLiveData.NOT_WORN) return false;
        return state != ReversalLiveData.WEARING_BLESSING || BlessingOwners.isOwner(
                ReversalLiveData.findWornItem(player, definitions[index].curseItem()), player.getUUID());
    }

    @Override
    protected void onButtonPressed(int index)
    {
        if (!buttonEnabled(index)) return;
        UUID playerId = Minecraft.getInstance().player == null
                ? null
                : Minecraft.getInstance().player.getUUID();
        if (playerId == null)
        {
            return;
        }

        int state = ReversalLiveData.wornState(definitions[index]);
        if (state == ReversalLiveData.WEARING_BLESSING)
        {
            BlessingNetwork.CHANNEL.sendToServer(
                    new ReversalActionC2SPacket(
                            playerId,
                            definitions[index],
                            ReversalActionC2SPacket.REVERT_TO_CURSE));
        }
        else if (state == ReversalLiveData.WEARING_CURSE)
        {
            BlessingNetwork.CHANNEL.sendToServer(
                    new ReversalActionC2SPacket(
                            playerId,
                            definitions[index],
                            ReversalActionC2SPacket.CONVERT_TO_BLESSING));
        }
    }
}
