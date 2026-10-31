package com.blessingofpandora.client;

import com.blessingofpandora.BlessingDefinitions;
import com.blessingofpandora.ReversalDescriptions;
import com.blessingofpandora.network.BlessingNetwork;
import com.blessingofpandora.network.ReversalToggleC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ReversalHudScreen extends ReversalHudScreenBase
{
    private final BlessingDefinitions single;
    private final InteractionHand inspectedHand;

    public ReversalHudScreen()
    {
        super(
                Component.translatable("blessingofpandora.hud.title"),
                BlessingDefinitions.values());
        this.single = null;
        this.inspectedHand = null;
    }

    public ReversalHudScreen(BlessingDefinitions definition, InteractionHand inspectedHand)
    {
        super(
                Component.translatable(
                        "blessingofpandora.hud.permission.title",
                        Component.translatable(definition.nameKey())),
                new BlessingDefinitions[] { definition });
        this.single = definition;
        this.inspectedHand = inspectedHand;
    }

    public boolean isPermissionMode()
    {
        return true;
    }

    public BlessingDefinitions singleDefinition()
    {
        return single;
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
                    definition, ReversalLiveData.liveLiberationStep(inspectedHand));
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
        return Component.translatable(
                ClientReversalPermission.isAllowed(definitions[index].ordinal())
                        ? "blessingofpandora.hud.button.allowed"
                        : "blessingofpandora.hud.button.denied");
    }

    @Override
    protected boolean buttonEnabled(int index)
    {
        return true;
    }

    @Override
    protected void onButtonPressed(int index)
    {
        UUID playerId = Minecraft.getInstance().player == null
                ? null
                : Minecraft.getInstance().player.getUUID();
        if (playerId == null)
        {
            return;
        }
        BlessingNetwork.CHANNEL.sendToServer(
                new ReversalToggleC2SPacket(
                        playerId, definitions[index]));
    }
}
