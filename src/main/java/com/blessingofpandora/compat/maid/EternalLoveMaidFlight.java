package com.blessingofpandora.compat.maid;

import com.blessingofpandora.mixin.LoveMobMoveControlAccessor;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 1.5.3 has ground/water navigation only; installs a real 3D navigator/controller while following in flight. */
public final class EternalLoveMaidFlight
{
    private static final String SAVED_GRAVITY = "blessingofpandora:eternal_previous_gravity";
    private static final Map<UUID, Flight> FLIGHTS = new HashMap<>();
    private static final class Flight
    {
        final EntityMaid maid;
        PathNavigation previousNavigation;
        MoveControl previousControl;
        final AirNavigation navigation;
        final AirMoveControl control;
        final boolean previousNoGravity;
        long nextPath, nextTeleport;
        BlockPos target;
        Flight(EntityMaid maid)
        {
            this.maid = maid;
            previousNavigation = maid.getNavigation(); previousControl = maid.getMoveControl();
            previousNoGravity = maid.isNoGravity();
            navigation = new AirNavigation(maid, maid.level());
            control = new AirMoveControl(maid);
            maid.getPersistentData().putBoolean(SAVED_GRAVITY, previousNoGravity);
        }
    }

    private EternalLoveMaidFlight() {}

    public static LivingEntity tick(ServerPlayer player, LivingEntity living)
    {
        if (!(living instanceof EntityMaid maid)) return living;
        if (!player.getAbilities().flying || maid.isOrderedToSit() || maid.isMaidInSittingPose()
                || maid.isSleeping() || maid.isPassenger() || maid.isVehicle() || maid.isLeashed())
        { clear(maid); return maid; }
        long now = player.server.overworld().getGameTime();
        Flight flight = FLIGHTS.get(maid.getUUID());
        if (flight != null && flight.maid != maid) { clear(flight.maid); flight = null; }
        if (flight == null) { flight = new Flight(maid); FLIGHTS.put(maid.getUUID(), flight); }
        install(flight);
        maid.fallDistance = 0;
        boolean differentDimension = maid.level() != player.level();
        if ((differentDimension || maid.distanceToSqr(player) > 64) && now >= flight.nextTeleport)
        {
            flight.nextTeleport = now + 10;
            Vec3 safe = safeNear(player, maid);
            if (safe != null && (!differentDimension || maid.canChangeDimensions()
                    && ForgeHooks.onTravelToDimension(maid, player.level().dimension())))
            {
                if (differentDimension) clear(maid); // Do not serialize a borrowed navigator/gravity into the replacement.
                if (maid.teleportTo(player.serverLevel(), safe.x, safe.y, safe.z, Set.of(), maid.getYRot(), 0))
                {
                    var replacement = player.serverLevel().getEntity(maid.getUUID());
                    if (differentDimension && replacement instanceof LivingEntity next) return tick(player, next);
                    maid.setDeltaMovement(Vec3.ZERO);
                    flight.navigation.stop();
                }
            }
        }
        if (maid.level() != player.level() || maid.isRemoved()) return maid;
        // Stand by two blocks beside the player. Repath only every 10 ticks or after a block change.
        Vec3 offset = new Vec3(2, 0.25, 0).yRot(-player.getYRot() * Mth.DEG_TO_RAD);
        Vec3 target = player.position().add(offset);
        BlockPos targetBlock = BlockPos.containing(target);
        if (maid.distanceToSqr(target) < 1)
        { flight.navigation.stop(); flight.control.hold(); }
        else if (now >= flight.nextPath || !targetBlock.equals(flight.target))
        {
            flight.target = targetBlock;
            flight.nextPath = now + 10;
            if (maid.level().hasChunkAt(targetBlock)) flight.navigation.moveTo(target.x, target.y, target.z, 1);
        }
        return maid;
    }

    private static void install(Flight flight)
    {
        EntityMaid maid = flight.maid;
        // Preserve replacement controllers installed by another mod/water mode rather than restoring stale ones.
        if (maid.getNavigation() != flight.navigation)
        { flight.previousNavigation = maid.getNavigation(); maid.getNavigation().stop(); maid.setNavigation(flight.navigation); }
        if (maid.getMoveControl() != flight.control)
        { flight.previousControl = maid.getMoveControl(); ((LoveMobMoveControlAccessor) maid).blessingofpandora$setMoveControl(flight.control); }
        maid.setNoGravity(true);
    }

    /** Only already loaded, collision-free positions within four blocks of the flying owner. */
    private static Vec3 safeNear(ServerPlayer player, EntityMaid maid)
    {
        ServerLevel level = player.serverLevel();
        int[][] offsets = {{2,0,0},{-2,0,0},{0,0,2},{0,0,-2},{2,1,0},{-2,1,0},{0,1,2},{0,1,-2},{0,2,0}};
        for (int[] offset : offsets)
        {
            Vec3 pos = player.position().add(offset[0], offset[1], offset[2]);
            BlockPos block = BlockPos.containing(pos);
            if (block.getY() < level.getMinBuildHeight() || pos.y + maid.getBbHeight() >= level.getMaxBuildHeight()
                    || !level.hasChunkAt(block) || !level.getWorldBorder().isWithinBounds(block)) continue;
            var box = maid.getBoundingBox().move(pos.subtract(maid.position()));
            if (level.hasChunksAt(BlockPos.containing(box.minX, pos.y, box.minZ),
                    BlockPos.containing(box.maxX, pos.y, box.maxZ))
                    && level.noCollision(maid, box) && !level.containsAnyLiquid(box)) return pos;
        }
        return null;
    }

    public static void clear(LivingEntity entity)
    {
        Flight flight = FLIGHTS.get(entity.getUUID());
        if (flight == null || flight.maid != entity) return;
        FLIGHTS.remove(entity.getUUID());
        EntityMaid maid = flight.maid;
        if (maid.getNavigation() == flight.navigation) { flight.navigation.stop(); maid.setNavigation(flight.previousNavigation); }
        if (maid.getMoveControl() == flight.control)
            ((LoveMobMoveControlAccessor) maid).blessingofpandora$setMoveControl(flight.previousControl);
        maid.setNoGravity(flight.previousNoGravity);
        maid.getPersistentData().remove(SAVED_GRAVITY);
        maid.fallDistance = 0;
    }

    public static void restoreSavedGravity(LivingEntity entity)
    {
        if (entity.getPersistentData().contains(SAVED_GRAVITY))
        {
            entity.setNoGravity(entity.getPersistentData().getBoolean(SAVED_GRAVITY));
            entity.getPersistentData().remove(SAVED_GRAVITY);
        }
    }

    public static void clearAll()
    { for (Flight flight : java.util.List.copyOf(FLIGHTS.values())) clear(flight.maid); }

    private static final class AirNavigation extends FlyingPathNavigation
    {
        AirNavigation(Mob maid, Level level) { super(maid, level); setCanOpenDoors(false); setCanFloat(true); }
        @Override protected Path createPath(Set<BlockPos> targets, int padding, boolean above, int accuracy, float range)
        {
            // The owner tether is eight blocks; do not inherit an arbitrarily enlarged combat follow range.
            // Vanilla's PathNavigationRegion obtains chunks synchronously, so reject an unloaded search area.
            BlockPos origin = mob.blockPosition();
            if (!level.hasChunksAt(origin.offset(-14, 0, -14), origin.offset(14, 0, 14))) return null;
            return super.createPath(targets, 2, above, accuracy, 12);
        }
        @Override public boolean isStableDestination(BlockPos pos)
        { return level.hasChunkAt(pos) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty(); }
    }

    /** Uses existing movement speed, so maid entities need no missing FLYING_SPEED attribute. */
    private static final class AirMoveControl extends MoveControl
    {
        AirMoveControl(Mob maid) { super(maid); }
        void hold() { operation = Operation.WAIT; mob.setDeltaMovement(mob.getDeltaMovement().scale(0.5)); }
        @Override public void tick()
        {
            mob.setNoGravity(true);
            mob.setYya(0); mob.setZza(0); mob.setXxa(0);
            if (operation != Operation.MOVE_TO) { mob.setDeltaMovement(mob.getDeltaMovement().scale(0.6)); return; }
            operation = Operation.WAIT;
            Vec3 delta = new Vec3(wantedX - mob.getX(), wantedY - mob.getY(), wantedZ - mob.getZ());
            double distance = delta.length();
            if (distance < 0.1) { hold(); return; }
            double speed = Math.min(0.55, Math.max(0.15,
                    mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 1.4));
            Vec3 desired = delta.scale(Math.min(speed, distance * 0.35) / distance);
            mob.setDeltaMovement(mob.getDeltaMovement().scale(0.35).add(desired.scale(0.65)));
            mob.setYRot(rotlerp(mob.getYRot(), (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90, 45));
            mob.yBodyRot = mob.getYRot();
        }
    }
}
