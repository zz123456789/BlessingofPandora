package com.blessingofpandora.mixin;

import com.blessingofpandora.compat.love.EternalLoveFlight;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Preserve vanilla creative-flight fall protection for the independently owned lease. */
@Mixin(Player.class)
public abstract class EternalLoveFlightPlayerMixin
{
    @Redirect(method = "causeFallDamage", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/player/Abilities;mayfly:Z", opcode = Opcodes.GETFIELD), require = 1)
    private boolean blessingofpandora$effectiveFlightFallProtection(Abilities abilities)
    {
        return EternalLoveFlight.allowed(abilities);
    }
}
