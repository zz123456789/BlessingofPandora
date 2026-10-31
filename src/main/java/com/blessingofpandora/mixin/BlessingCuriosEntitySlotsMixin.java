package com.blessingofpandora.mixin;

import com.blessingofpandora.BlessingSlotRegistration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.type.ISlotType;

/** Runs at slot definition lookup, so initial creation, reload and saved inventory restoration agree. */
@Pseudo // Curios is required at runtime; its implementation class is absent from the compile-only API JAR.
@Mixin(targets = "top.theillusivec4.curios.common.data.CuriosEntityManager", remap = false)
public abstract class BlessingCuriosEntitySlotsMixin
{
    @Unique private static final AtomicLong blessingofpandora$revision = new AtomicLong();
    @Unique private long blessingofpandora$cachedRevision = -1;
    @Unique private Map<String, ISlotType> blessingofpandora$sourceSlots;
    @Unique private Map<String, ISlotType> blessingofpandora$playerSlots;
    @Unique private Map<String, Integer> blessingofpandora$sourceLegacySlots;
    @Unique private Map<String, Integer> blessingofpandora$legacyPlayerSlots;

    @Inject(method = "getEntitySlots", at = @At("RETURN"), cancellable = true)
    private void blessingofpandora$missingCharm(EntityType<?> entity,
            CallbackInfoReturnable<Map<String, ISlotType>> cir)
    {
        Map<String, ISlotType> source = cir.getReturnValue();
        if (entity != EntityType.PLAYER || source.containsKey("charm")) return;
        long revision = blessingofpandora$revision.get();
        if (source != blessingofpandora$sourceSlots || revision != blessingofpandora$cachedRevision)
        {
            blessingofpandora$sourceSlots = source;
            blessingofpandora$playerSlots = BlessingSlotRegistration.withMissingPlayerCharm(entity, source, this);
            blessingofpandora$cachedRevision = revision;
        }
        cir.setReturnValue(blessingofpandora$playerSlots);
    }

    // Empty maps may be shared across reloads; their identity cannot detect a changed global template.
    @Inject(method = "apply", at = @At("RETURN"), require = 0)
    private void blessingofpandora$reloaded(CallbackInfo ci)
    {
        blessingofpandora$revision.incrementAndGet();
    }

    @Inject(method = "applySyncPacket", at = @At("RETURN"), require = 0)
    private static void blessingofpandora$synced(CallbackInfo ci)
    {
        blessingofpandora$revision.incrementAndGet();
    }

    // 5.4.5 constructs client handlers through this integer-map accessor; newer Curios removed it.
    @Inject(method = "getClientSlots", at = @At("RETURN"), cancellable = true, require = 0)
    private void blessingofpandora$legacyMissingCharm(EntityType<?> entity,
            CallbackInfoReturnable<Map<String, Integer>> cir)
    {
        Map<String, Integer> source = cir.getReturnValue();
        if (entity != EntityType.PLAYER || source.containsKey("charm")) return;
        if (source != blessingofpandora$sourceLegacySlots)
        {
            blessingofpandora$sourceLegacySlots = source;
            Map<String, Integer> result = new HashMap<>(source);
            result.put("charm", 0);
            blessingofpandora$legacyPlayerSlots = Map.copyOf(result);
        }
        cir.setReturnValue(blessingofpandora$legacyPlayerSlots);
    }
}
