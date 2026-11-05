package com.blessingofpandora.mixin;

import com.blessingofpandora.compat.love.EternalLoveFlight;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Abilities;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Both the flight-toggle authority and vanilla anti-floating check see the same effective permission. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class EternalLoveFlightServerMixin
{
    @Redirect(method = "handlePlayerAbilities", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/player/Abilities;mayfly:Z", opcode = Opcodes.GETFIELD), require = 1)
    private boolean blessingofpandora$allowFlightToggle(Abilities abilities)
    {
        return EternalLoveFlight.allowed(abilities);
    }

    @Redirect(method = "handleMovePlayer", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/player/Abilities;mayfly:Z", opcode = Opcodes.GETFIELD), require = 1)
    private boolean blessingofpandora$allowFloating(Abilities abilities)
    {
        return EternalLoveFlight.allowed(abilities);
    }
}
