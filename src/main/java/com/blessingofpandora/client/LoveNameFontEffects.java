/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Adapted from The Long Travail's DiaryFontEffects, DiaryText and
 * NameHaloKernel, commit f295c4ead89c0a99ef2f7ed9f56e9cbba42bfb34.
 * Changes: rose palette, isolated marker, mixed chat runs and low-sample halo.
 */
package com.blessingofpandora.client;

import com.blessingofpandora.BlessingOfPandora;
import com.blessingofpandora.LoveNameStyle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Draw-time material animation. Cached tooltip/chat Components remain live without a game tick. */
@Mod.EventBusSubscriber(modid = BlessingOfPandora.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LoveNameFontEffects
{
    private static final long EPOCH = System.nanoTime();
    private static final Map<FormattedCharSequence, List<Run>> LAYOUTS = new WeakHashMap<>();
    private static ShaderInstance shader;
    // The reference mod's LOW halo: two samples in each ring, with compensated opacity.
    private static final float[][] HALO_X = {{0.55F, -0.55F}, {-0.4209518F, 0.4209518F}};
    private static final float[][] HALO_Y = {{0, 0}, {1.0162675F, -1.0162675F}};
    private static final float[] HALO_OPACITY = {
            1F - (float) Math.pow(1F - 0.075F, 4), 1F - (float) Math.pow(1F - 0.035F, 4)};

    private LoveNameFontEffects() {}

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event)
    {
        shader = null;
        LAYOUTS.clear();
        try
        {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.parse("blessingofpandora:love_name"), DefaultVertexFormat.POSITION_COLOR_TEX),
                    loaded -> shader = loaded);
        }
        catch (IOException failure)
        {
            // An incompatible resource pack may replace the shader. Names still have their fixed pink fallback.
            LogUtils.getLogger().warn("Unable to load Love's flowing name shader; using the pink fallback", failure);
        }
    }

    public static boolean hasMarker(FormattedCharSequence text)
    {
        if (text instanceof CleanSequence) return false;
        return !text.accept((index, style, codePoint) -> !LoveNameStyle.MARKER.equals(style.getInsertion()));
    }

    public static boolean shaderLoaded() { return shader != null; }

    public static int draw(Font font, FormattedCharSequence text, float x, float y, int color,
            boolean shadow, Matrix4f pose, MultiBufferSource buffers, Font.DisplayMode mode,
            int background, int light)
    {
        List<Run> runs = LAYOUTS.computeIfAbsent(text, LoveNameFontEffects::prepare);
        float advance = 0;
        for (Run run : runs)
        {
            float origin = x + advance;
            if (run.flowing && shader != null)
            {
                int alpha = color >>> 24;
                if (alpha < 4) alpha = 255;
                MultiBufferSource haloBuffers = new MaterialBuffers(buffers, origin, true);
                Matrix4f haloPose = new Matrix4f();
                for (int ring = 0; ring < 2; ring++)
                {
                    int haloAlpha = Math.round(alpha * HALO_OPACITY[ring]);
                    if (haloAlpha < 4) continue;
                    for (int sample = 0; sample < 2; sample++)
                    {
                        haloPose.set(pose).translate(HALO_X[ring][sample], HALO_Y[ring][sample], 0);
                        font.drawInBatch(run.text, origin, y, haloAlpha << 24 | color & 0xFFFFFF,
                                false, haloPose, haloBuffers, mode, 0, light);
                    }
                }
                font.drawInBatch(run.text, origin, y, color, shadow, pose,
                        new MaterialBuffers(buffers, origin, false), mode, background, light);
            }
            else font.drawInBatch(run.text, origin, y, color, shadow, pose, buffers, mode, background, light);
            advance += font.getSplitter().stringWidth(run.text);
        }
        return (int) (x + advance) + (shadow ? 1 : 0);
    }

    private interface CleanSequence extends FormattedCharSequence {}
    private record Glyph(int index, Style style, int codePoint) {}
    private record GlyphSequence(Glyph[] glyphs) implements CleanSequence
    {
        @Override public boolean accept(FormattedCharSink sink)
        {
            for (Glyph glyph : glyphs)
                if (!sink.accept(glyph.index, glyph.style, glyph.codePoint)) return false;
            return true;
        }
    }
    private record Run(boolean flowing, GlyphSequence text) {}

    private static List<Run> prepare(FormattedCharSequence text)
    {
        List<Run> result = new ArrayList<>();
        List<Glyph> glyphs = new ArrayList<>();
        boolean[] flowing = {false};
        text.accept((index, style, codePoint) -> {
            boolean marked = LoveNameStyle.MARKER.equals(style.getInsertion());
            if (!glyphs.isEmpty() && marked != flowing[0])
            {
                result.add(new Run(flowing[0], new GlyphSequence(glyphs.toArray(Glyph[]::new))));
                glyphs.clear();
            }
            flowing[0] = marked;
            // Only our marker is removed. Click, hover, font, bold, italic and other mods' styles survive.
            glyphs.add(new Glyph(index, marked ? style.withInsertion(null) : style, codePoint));
            return true;
        });
        if (!glyphs.isEmpty()) result.add(new Run(flowing[0], new GlyphSequence(glyphs.toArray(Glyph[]::new))));
        return List.copyOf(result);
    }

    private static final class MaterialBuffers implements MultiBufferSource
    {
        private final MultiBufferSource original;
        private final float origin;
        private final boolean halo;
        // The map lives only for this draw, never retaining stale font atlases after resource reloads.
        private final Map<RenderType, RenderType> mapped = new IdentityHashMap<>();

        private MaterialBuffers(MultiBufferSource original, float origin, boolean halo)
        {
            this.original = original;
            this.origin = origin;
            this.halo = halo;
        }

        @Override public VertexConsumer getBuffer(RenderType type)
        {
            return original.getBuffer(mapped.computeIfAbsent(type, originalType -> new MaterialType(originalType, origin, halo)));
        }
    }

    private static final class MaterialType extends RenderType
    {
        private MaterialType(RenderType original, float origin, boolean halo)
        {
            super("blessingofpandora_love_name", original.format(), original.mode(), original.bufferSize(),
                    false, false, () -> setup(original, origin, halo), original::clearRenderState);
        }
    }

    private static void setup(RenderType original, float origin, boolean halo)
    {
        original.setupRenderState();
        if (shader == null) return;
        ShaderInstance originalShader = RenderSystem.getShader();
        String name = originalShader == null ? "" : originalShader.getName();
        // Preserve independent outline/background passes used by alternative font renderers.
        if (name.contains("stroke") || name.contains("background")) return;
        int atlas = name.contains("sdf") ? 2 : name.contains("intensity") ? 1 : 0;
        RenderSystem.setShader(() -> shader);
        shader.safeGetUniform("Seconds").set((System.nanoTime() - EPOCH) / 1_000_000_000F % 3600F);
        shader.safeGetUniform("Halo").set(halo ? 1F : 0F);
        shader.safeGetUniform("AtlasMode").set((float) atlas);
        shader.safeGetUniform("OriginX").set(origin);
    }
}
