/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Adapted from The Long Travail's DiaryFontMixin, commit
 * f295c4ead89c0a99ef2f7ed9f56e9cbba42bfb34. See bundled third-party notice.
 */
package com.blessingofpandora.mixin;

import com.blessingofpandora.client.LoveNameFontEffects;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Independent marker: it neither intercepts nor replaces The Long Travail's own names. */
@Mixin(value = Font.class, priority = 3100)
public abstract class LoveNameFontMixin
{
    @Inject(method = "drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
            at = @At("HEAD"), cancellable = true)
    private void blessingofpandora$drawLoveComponent(Component text, float x, float y, int color,
            boolean shadow, Matrix4f pose, MultiBufferSource buffers, Font.DisplayMode mode,
            int background, int light, CallbackInfoReturnable<Integer> cir)
    {
        blessingofpandora$drawLove(text.getVisualOrderText(), x, y, color, shadow, pose,
                buffers, mode, background, light, cir);
    }

    @Inject(method = "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
            at = @At("HEAD"), cancellable = true)
    private void blessingofpandora$drawLoveSequence(FormattedCharSequence text, float x, float y, int color,
            boolean shadow, Matrix4f pose, MultiBufferSource buffers, Font.DisplayMode mode,
            int background, int light, CallbackInfoReturnable<Integer> cir)
    {
        blessingofpandora$drawLove(text, x, y, color, shadow, pose, buffers, mode, background, light, cir);
    }

    private void blessingofpandora$drawLove(FormattedCharSequence text, float x, float y, int color,
            boolean shadow, Matrix4f pose, MultiBufferSource buffers, Font.DisplayMode mode,
            int background, int light, CallbackInfoReturnable<Integer> cir)
    {
        if (!LoveNameFontEffects.hasMarker(text)) return;
        cir.setReturnValue(LoveNameFontEffects.draw((Font) (Object) this, text, x, y, color,
                shadow, pose, buffers, mode, background, light));
    }
}
