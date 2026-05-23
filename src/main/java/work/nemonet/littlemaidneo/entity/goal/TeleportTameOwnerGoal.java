package work.nemonet.littlemaidneo.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import work.nemonet.littlemaidneo.LMRBMod;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

import java.util.EnumSet;
import java.util.function.Supplier;

public class TeleportTameOwnerGoal<T extends PathfinderMob & OwnableEntity> extends Goal {
    protected final T tameable;
    protected final Level world;
    protected final Supplier<Float> teleportStartSq;
    private final PathNavigation navigation;
    private LivingEntity owner;
    private int updateCountdownTicks;

    public TeleportTameOwnerGoal(T tameable, Supplier<Float> teleportStart) {
        this.tameable = tameable;
        this.world = tameable.level();
        this.teleportStartSq = () -> teleportStart.get() * teleportStart.get();
        this.navigation = tameable.getNavigation();
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity tameOwner = TameableUtil.getTameOwner(this.tameable).orElse(null);
        if (tameOwner == null) {
            return false;
        } else if (tameOwner.isSpectator()) {
            return false;
        } else if (this.tameable.distanceToSqr(tameOwner) < teleportStartSq.get()) {
            return false;
        } else {
            this.owner = tameOwner;
            return true;
        }
    }

    public boolean canContinueToUse() {
        return teleportStartSq.get() < this.tameable.distanceToSqr(this.owner);
    }

    @Override
    public void start() {
        this.updateCountdownTicks = 0;
    }

    @Override
    public void stop() {
        this.owner = null;
        this.navigation.stop();
    }

    @Override
    public void tick() {
        this.tameable.getLookControl().setLookAt(this.owner, 10.0f, this.tameable.getMaxHeadXRot());
        if (--this.updateCountdownTicks > 0) {
            return;
        }
        this.updateCountdownTicks = adjustedTickDelay(10);
        tryTeleport();
    }

    protected void tryTeleport() {
        BlockPos ownerPos = this.owner.blockPosition();
        for (int i = 0; i < getConfigMaxTryTeleportCount(); ++i) {
            int teleportWidthRange = getConfigTeleportWidthRange();
            int teleportHeightRange = getConfigTeleportHeightRange();
            int x = this.getRandomInt(-teleportWidthRange, teleportWidthRange);
            int y = this.getRandomInt(-teleportHeightRange, teleportHeightRange);
            int z = this.getRandomInt(-teleportWidthRange, teleportWidthRange);
            boolean bl = this.tryTeleportTo(ownerPos.getX() + x, ownerPos.getY() + y, ownerPos.getZ() + z);
            if (!bl)
                continue;
            return;
        }
    }

    protected boolean tryTeleportTo(int x, int y, int z) {
        if (isOwnerRange(this.owner, x, y, z)) {
            return false;
        }
        if (!this.canTeleportTo(new BlockPos(x, y, z))) {
            return false;
        }
        this.tameable.moveTo(x + 0.5, y, z + 0.5, this.tameable.getYRot(), this.tameable.getXRot());
        this.navigation.stop();
        return true;
    }

    protected boolean isOwnerRange(Entity owner, int x, int y, int z) {
        if (getConfigCanTeleportOwnerForwards()) {
            return false;
        }
        Vec3 ownerPos = owner.position();
        Vec3 entityPos = new Vec3(x + 0.5, y, z + 0.5).subtract(ownerPos);
        Vec3 ownerRot = owner.getViewVector(1F);
        double dot = entityPos.dot(ownerRot);
        double range = getConfigOwnerForwardRange();
        // プレイヤー位置を原点としたアイテムの位置と、プレイヤーの向きの内積がプラス
        // かつ内積の大きさが4m以下
        return 0 < dot && dot < range * range;
    }

    protected boolean canTeleportTo(BlockPos pos) {
        PathType pathNodeType = WalkNodeEvaluator.getPathTypeStatic(
                new net.minecraft.world.level.pathfinder.PathfindingContext(this.world, this.tameable), pos.mutable());
        if (pathNodeType != PathType.WALKABLE) {
            return false;
        }
        BlockPos blockPos = pos.subtract(this.tameable.blockPosition());
        return this.world.noCollision(this.tameable, this.tameable.getBoundingBox().move(blockPos));
    }

    protected int getRandomInt(int min, int max) {
        return this.tameable.getRandom().nextInt(max - min + 1) + min;
    }

    protected boolean getConfigCanTeleportOwnerForwards() {
        return LMRBMod.getConfig().movement.canTeleportOwnerForwards;
    }

    protected float getConfigOwnerForwardRange() {
        return LMRBMod.getConfig().movement.ownerForwardRange;
    }

    protected int getConfigMaxTryTeleportCount() {
        return LMRBMod.getConfig().movement.maxTryTeleportCount;
    }

    protected int getConfigTeleportWidthRange() {
        return LMRBMod.getConfig().movement.teleportWidth;
    }

    protected int getConfigTeleportHeightRange() {
        return LMRBMod.getConfig().movement.teleportHeight;
    }

}
