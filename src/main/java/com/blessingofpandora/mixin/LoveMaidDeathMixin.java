package com.blessingofpandora.mixin;

import com.blessingofpandora.compat.love.LoveProgressEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LoveMaidDeathMixin
{
    @Shadow protected boolean dead;
    @Inject(method = "die", at = @At("RETURN"))
    private void blessingofpandora$actualDeath(DamageSource source, CallbackInfo ci)
    {
        if (dead) LoveProgressEvents.recordDeath((LivingEntity) (Object) this);
    }
}
