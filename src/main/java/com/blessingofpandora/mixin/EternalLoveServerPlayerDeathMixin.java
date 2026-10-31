package com.blessingofpandora.mixin;

import com.blessingofpandora.compat.love.EternalLoveCombat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ServerPlayer overrides die and publishes death messages before delegating. */
@Mixin(ServerPlayer.class)
public abstract class EternalLoveServerPlayerDeathMixin
{
    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void blessingofpandora$rescueBeforePlayerDeath(DamageSource source, CallbackInfo ci)
    {
        if (EternalLoveCombat.preventDeath((ServerPlayer)(Object)this, source)) ci.cancel();
    }
}
