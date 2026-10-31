package com.blessingofpandora.mixin;

import com.blessingofpandora.BlessingBenefits;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public abstract class BlessingPlayerDamageMixin
{
    // Player overrides actuallyHurt and has its own remaining-health-damage local.
    @ModifyVariable(method = "actuallyHurt", ordinal = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getCombatTracker()Lnet/minecraft/world/damagesource/CombatTracker;"))
    private float blessingofpandora$healthDamage(float amount, DamageSource source, float originalAmount)
    {
        return BlessingBenefits.finalizeDamage((Player) (Object) this, source, amount);
    }
}
