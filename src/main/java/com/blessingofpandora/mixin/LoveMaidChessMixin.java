package com.blessingofpandora.mixin;

import com.blessingofpandora.compat.maid.LoveMaidChess;
import com.github.tartaricacid.touhoulittlemaid.entity.favorability.Type;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Absent target is skipped when the optional Maid mod is not installed. */
@Pseudo
@Mixin(targets = "com.github.tartaricacid.touhoulittlemaid.entity.favorability.FavorabilityManager", remap = false)
public abstract class LoveMaidChessMixin
{
    @Shadow @Final private EntityMaid maid;

    @Inject(method = "apply(Lcom/github/tartaricacid/touhoulittlemaid/entity/favorability/Type;)V",
            at = @At("HEAD"), remap = false)
    private void blessingofpandora$recordChessWin(Type type, CallbackInfo ci)
    {
        // Before the favorability cooldown check: winning twice is still a win,
        // including after Love's progress has been reset by another contract.
        LoveMaidChess.onFavorability(maid, type);
    }
}
