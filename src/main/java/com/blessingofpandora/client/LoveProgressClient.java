package com.blessingofpandora.client;

import com.blessingofpandora.BlessingOfPandora;
import com.blessingofpandora.compat.love.LoveLedger;
import com.blessingofpandora.compat.love.LoveProgressEvents;
import com.blessingofpandora.network.LoveProgressS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID, value = Dist.CLIENT)
public final class LoveProgressClient
{
    private static LoveProgressS2CPacket state;
    private LoveProgressClient() {}
    public static void set(LoveProgressS2CPacket value) { state = value; }
    public static LoveProgressS2CPacket current() { return state; }
    public static void requestFresh()
    {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.getConnection() != null)
            com.blessingofpandora.network.BlessingNetwork.CHANNEL.sendToServer(
                    new com.blessingofpandora.network.LoveHudRequestC2SPacket());
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { state = null; }

    public static void append(ItemStack stack, List<Component> tooltip)
    {
        var player = Minecraft.getInstance().player;
        LoveProgressS2CPacket data = state;
        UUID owner = LoveProgressEvents.owner(stack);
        if (player == null || data == null || !player.getUUID().equals(data.owner())
                || owner == null || !owner.equals(data.owner()) || data.maid() == null)
        {
            tooltip.add(Component.translatable("item.blessingofpandora.love.unbound").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("item.blessingofpandora.love.bound", data.maidName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (data.evolved()) tooltip.add(Component.translatable("item.blessingofpandora.love.completed").withStyle(ChatFormatting.GOLD));
        add(tooltip, "eternity", time(data.time()), "24:00:00", data.time() >= LoveLedger.ETERNITY_TICKS);
        tooltip.add(Component.translatable(data.dead() ? "item.blessingofpandora.love.death_failed" : "item.blessingofpandora.love.death_clean")
                .withStyle(data.dead() ? ChatFormatting.RED : ChatFormatting.GREEN));
        add(tooltip, "battle", String.format(Locale.ROOT, "%.1f", data.damage()), "1314520", data.damage() >= LoveLedger.DAMAGE_TARGET);
        tooltip.add(Component.translatable("item.blessingofpandora.love.games", mark(data.games(), 1), mark(data.games(), 2), mark(data.games(), 4))
                .withStyle(data.games() == 7 ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        add(tooltip, "romance", time(data.romance()), ">00:30:00", data.romance() > LoveLedger.ROMANCE_TICKS);
        tooltip.add(Component.translatable("item.blessingofpandora.love.jealousy").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable(data.oath() ? "item.blessingofpandora.love.oath_done" : "item.blessingofpandora.love.oath_wait")
                .withStyle(data.oath() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.blessingofpandora.love.warning").withStyle(ChatFormatting.RED));
    }
    private static Component mark(int games, int bit)
    {
        return Component.translatable((games & bit) != 0 ? "item.blessingofpandora.love.yes" : "item.blessingofpandora.love.no");
    }
    private static void add(List<Component> lines, String key, String value, String target, boolean done)
    {
        lines.add(Component.translatable("item.blessingofpandora.love." + key, value, target)
                .withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GRAY));
    }
    private static String time(long ticks)
    {
        long seconds = Math.max(0, ticks) / 20;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
    }
}
