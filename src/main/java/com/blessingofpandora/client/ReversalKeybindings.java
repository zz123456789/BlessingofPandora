package com.blessingofpandora.client;

import com.blessingofpandora.BlessingOfPandora;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = BlessingOfPandora.MODID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ReversalKeybindings
{
    public static final KeyMapping OPEN_REVERSAL_HUD = new KeyMapping(
            "key.blessingofpandora.open",
            GLFW.GLFW_KEY_P,
            "key.categories.blessingofpandora");

    private ReversalKeybindings()
    {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event)
    {
        event.register(OPEN_REVERSAL_HUD);
    }
}
