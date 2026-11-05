package com.blessingofpandora.mixin;

import com.blessingofpandora.SoulSculptingHandler;
import com.blessingofpandora.StrongAttributeHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restore only validated local bonuses, including saves written before this fix. */
@Mixin(LivingEntity.class)
public abstract class BlessingHealthLoadMixin
{
    @Inject(method = "readAdditionalSaveData", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private void blessingofpandora$restoreHealthBonusesBeforeClamp(CompoundTag tag, CallbackInfo ci)
    {
        LivingEntity entity = (LivingEntity)(Object)this;
        // Both source attributes and active effects are loaded before this call.
        // Keep bonuses transient so removed conditions cannot persist as stale buffs.
        StrongAttributeHandler.restoreLoadedAttributes(entity);
        SoulSculptingHandler.restoreLoadedAttributes(entity);
    }
}
