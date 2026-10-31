package com.blessingofpandora.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Typed access to the vanilla controller field; contains no optional maid API. */
@Mixin(Mob.class)
public interface LoveMobMoveControlAccessor
{
    @Accessor("moveControl") void blessingofpandora$setMoveControl(MoveControl control);
}
