package com.blessingofpandora.mixin;

import com.blessingofpandora.compat.maid.LoveMaidChess;
import com.github.tartaricacid.touhoulittlemaid.api.game.gomoku.GomokuCodec;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityGomoku", remap = false)
public abstract class LoveMaidGomokuStateMixin
{
    @Inject(method = "setStateData", at = @At("RETURN"), remap = false)
    private void blessingofpandora$newPosition(GomokuCodec.StateData data, CallbackInfo ci)
    {
        LoveMaidChess.onNewRound((BlockEntity)(Object)this);
    }

    @Inject(method = "clickWithDebug", at = @At("RETURN"), remap = false)
    private void blessingofpandora$newDebugPosition(CallbackInfo ci)
    {
        LoveMaidChess.onNewRound((BlockEntity)(Object)this);
    }
}
