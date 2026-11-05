package com.blessingofpandora.mixin;

import com.blessingofpandora.compat.love.EternalLoveFlight;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.world.entity.player.Abilities;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Ordinary ability synchronization presents the combined grant without changing its backing field. */
@Mixin(ClientboundPlayerAbilitiesPacket.class)
public abstract class EternalLoveFlightPacketMixin
{
    @Redirect(method = "<init>(Lnet/minecraft/world/entity/player/Abilities;)V", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/player/Abilities;mayfly:Z", opcode = Opcodes.GETFIELD), require = 1)
    private boolean blessingofpandora$effectiveFlight(Abilities abilities)
    {
        return EternalLoveFlight.allowed(abilities);
    }
}
