package com.blessingofpandora.client;

import com.blessingofpandora.BlessingOfPandora;
import com.blessingofpandora.LoveOrbRegistration;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Real rendered tooltips are the source of truth, including JEI and Curios; no inventory scan or reflection. */
@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID, value = Dist.CLIENT)
public final class LoveHudKeyHandler
{
    public static final String HINT_KEY = "tooltip.blessingofpandora.love.open_hud";
    private static final long FRESH_NANOS = 250_000_000L;
    private static final LoveHudHoldGesture HOLD = new LoveHudHoldGesture();
    private static long frame, hoverFrame = -1, hoverTime;
    private static Screen hoverScreen, holdScreen;
    private static ItemStack hovered = ItemStack.EMPTY, source = ItemStack.EMPTY, snapshot = ItemStack.EMPTY;
    private static double hoverX, hoverY, anchorX, anchorY;
    private static Pending pending;
    private record Pending(Screen parent, ItemStack stack) {}

    private LoveHudKeyHandler() {}

    @Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration
    {
        @SubscribeEvent public static void tooltips(RegisterClientTooltipComponentFactoriesEvent event)
        { event.register(LoveHudHoldTooltip.class, LoveHudHoldTooltip.Renderer::new); }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tooltipLayout(RenderTooltipEvent.GatherComponents event)
    {
        if (!LoveOrbRegistration.isLoveFamily(event.getItemStack())) return;
        int width = Math.max(40, Math.min(260, event.getScreenWidth() - 24));
        event.setMaxWidth(event.getMaxWidth() > 0 ? Math.min(width, event.getMaxWidth()) : width);
        var elements = event.getTooltipElements();
        for (int i = 0; i < elements.size(); i++)
        {
            var text = elements.get(i).left().orElse(null);
            if (!(text instanceof Component component)
                    || !(component.getContents() instanceof TranslatableContents translation)
                    || !HINT_KEY.equals(translation.getKey())) continue;
            // Insert our own component after our own hint; never remove or reorder another mod's lines.
            int barWidth = Math.min(width, Minecraft.getInstance().font.width(component));
            elements.add(i + 1, Either.right(new LoveHudHoldTooltip(Math.max(1, barWidth))));
            break;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderedTooltip(RenderTooltipEvent.Color event)
    {
        Minecraft mc = Minecraft.getInstance();
        hovered = event.getItemStack();
        if (!eligibleScreen(mc) || !LoveOrbRegistration.isLoveFamily(hovered))
        { hovered = ItemStack.EMPTY; hoverFrame = -1; return; }
        // Color is reached only after uncancelled Gather/Pre events, immediately before actual drawing.
        hoverScreen = mc.screen;
        hoverFrame = frame;
        hoverTime = System.nanoTime();
        hoverX = mouseX(mc);
        hoverY = mouseY(mc);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderTick(TickEvent.RenderTickEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.START)
        {
            frame++;
            hovered = ItemStack.EMPTY;
            hoverFrame = -1;
            return;
        }
        long now = System.nanoTime();
        if (!mc.isWindowActive()) { cancel(true); return; }
        boolean held = altDown(mc);
        if (!held)
        {
            cancel(false);
            HOLD.update(now, false, false);
            return;
        }
        boolean valid = freshHover(mc, now) && !editing(mc.screen);
        if (valid && !sameTarget())
        {
            cancel(false);
            holdScreen = hoverScreen;
            source = hovered;
            snapshot = hovered.copy();
            anchorX = hoverX;
            anchorY = hoverY;
        }
        if (!valid)
        {
            cancel(false);
            HOLD.update(now, false, held);
            return;
        }
        if (HOLD.update(now, true, held))
        {
            if (pending == null) pending = new Pending(hoverScreen, hovered.copy());
        }
        else pending = null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void clientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isWindowActive()) { cancel(true); return; }
        if (!altDown(mc)) { cancel(false); HOLD.update(System.nanoTime(), false, false); return; }
        Pending request = pending;
        if (request == null) return;
        if (request.parent != mc.screen || !freshHover(mc, System.nanoTime()) || editing(mc.screen)
                || !ItemStack.matches(request.stack, hovered))
        { cancel(false); return; }
        cancel(true);
        ForgeHooksClient.pushGuiLayer(mc, new LoveHudScreen(request.parent, request.stack.copy()).asGuiLayer());
    }

    @SubscribeEvent public static void screenClosed(ScreenEvent.Closing event)
    {
        cancel(altDown(Minecraft.getInstance()));
        hovered = ItemStack.EMPTY;
        hoverFrame = -1;
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event)
    {
        cancel(true);
        hovered = ItemStack.EMPTY;
        hoverScreen = null;
        hoverFrame = -1;
    }

    public static double holdFraction() { return sameTarget() ? HOLD.fraction() : 0; }

    private static boolean sameTarget()
    {
        return holdScreen == hoverScreen && ItemStack.matches(snapshot, hovered)
                && (source == hovered || Math.abs(anchorX - hoverX) < 0.5 && Math.abs(anchorY - hoverY) < 0.5);
    }

    private static boolean freshHover(Minecraft mc, long now)
    {
        return eligibleScreen(mc) && hoverScreen == mc.screen && hoverFrame == frame && !hovered.isEmpty()
                && now - hoverTime < FRESH_NANOS && Math.abs(mouseX(mc) - hoverX) < 0.5
                && Math.abs(mouseY(mc) - hoverY) < 0.5;
    }

    private static boolean eligibleScreen(Minecraft mc)
    {
        return mc.player != null && mc.screen != null && !(mc.screen instanceof LoveHudScreen)
                && !(mc.screen instanceof ReversalHudScreenBase);
    }

    private static boolean editing(GuiEventListener listener)
    {
        if (listener instanceof EditBox box) return box.isFocused();
        return listener instanceof ContainerEventHandler container && container.getFocused() != null
                && editing(container.getFocused());
    }

    private static void cancel(boolean untilRelease)
    {
        if (untilRelease) HOLD.consume(); else HOLD.resetProgress();
        pending = null;
        holdScreen = null;
        source = snapshot = ItemStack.EMPTY;
    }

    private static boolean altDown(Minecraft mc)
    {
        long window = mc.getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
    }

    private static double mouseX(Minecraft mc)
    { return mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth(); }
    private static double mouseY(Minecraft mc)
    { return mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight(); }
}
