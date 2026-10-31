package com.blessingofpandora.mixin;

import com.blessingofpandora.compat.maid.LoveMaidChess;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
        "com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityCChess",
        "com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityWChess"
}, remap = false)
public abstract class LoveMaidChessEndgameMixin
{
    @Inject(method = "setEndgame", at = @At("RETURN"), remap = false)
    private void blessingofpandora$newEndgame(String fen, CallbackInfo ci)
    {
        LoveMaidChess.onNewRound((BlockEntity)(Object)this);
    }
}
