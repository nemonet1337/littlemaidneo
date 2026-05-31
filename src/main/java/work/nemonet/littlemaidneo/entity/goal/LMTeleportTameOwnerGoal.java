package work.nemonet.littlemaidneo.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import work.nemonet.littlemaidneo.config.LMRBConfig;
import work.nemonet.littlemaidneo.entity.util.MovingMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;

import java.util.EnumSet;
import java.util.function.Supplier;

/**
 * メイドさん（ESCORT モード）がご主人様から離れすぎた際にテレポートするゴール。
 * <p>
 * 旧 {@code TeleportTameOwnerGoal<LittleMaidEntity>} + {@code LMTeleportTameOwnerGoal} を 1 クラスに統合。
 */
public class LMTeleportTameOwnerGoal extends Goal {

    protected final LittleMaidEntity tameable;
    protected final Level world;
    protected final Supplier<Float> teleportStartSq;
    private final PathNavigation navigation;
    private LivingEntity owner;
    private int updateCountdownTicks;

    public LMTeleportTameOwnerGoal(LittleMaidEntity tameable, Supplier<Float> teleportStart) {
        this.tameable = tameable;
        this.world = tameable.level();
        this.teleportStartSq = () -> teleportStart.get() * teleportStart.get();
        this.navigation = tameable.getNavigation();
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // ESCORTモードのみ発動
        if (this.tameable.getMovingMode() != MovingMode.ESCORT) return false;

        LivingEntity tameOwner = TameableUtil.getTameOwner(this.tameable).orElse(null);
        if (tameOwner == null) return false;
        if (tameOwner.isSpectator()) return false;
        if (this.tameable.distanceToSqr(tameOwner) < teleportStartSq.get()) return false;
        this.owner = tameOwner;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.tameable.getMovingMode() != MovingMode.ESCORT) return false;
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
        if (--this.updateCountdownTicks > 0) return;
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
            if (this.tryTeleportTo(ownerPos.getX() + x, ownerPos.getY() + y, ownerPos.getZ() + z)) return;
        }
    }

    protected boolean tryTeleportTo(int x, int y, int z) {
        if (isOwnerRange(this.owner, x, y, z)) return false;
        if (!this.canTeleportTo(new BlockPos(x, y, z))) return false;
        this.tameable.snapTo(x + 0.5, y, z + 0.5, this.tameable.getYRot(), this.tameable.getXRot());
        this.navigation.stop();
        return true;
    }

    protected boolean isOwnerRange(Entity owner, int x, int y, int z) {
        if (getConfigCanTeleportOwnerForwards()) return false;
        Vec3 ownerPos = owner.position();
        Vec3 entityPos = new Vec3(x + 0.5, y, z + 0.5).subtract(ownerPos);
        Vec3 ownerRot = owner.getViewVector(1F);
        double dot = entityPos.dot(ownerRot);
        double range = getConfigOwnerForwardRange();
        // プレイヤー位置を原点としたメイドさんの位置と、プレイヤーの向きの内積がプラス
        // かつ内積の大きさが range*range 以下
        return 0 < dot && dot < range * range;
    }

    protected boolean canTeleportTo(BlockPos pos) {
        PathType pathNodeType = WalkNodeEvaluator.getPathTypeStatic(
                new net.minecraft.world.level.pathfinder.PathfindingContext(this.world, this.tameable), pos.mutable());
        if (pathNodeType != PathType.WALKABLE) return false;
        BlockPos blockPos = pos.subtract(this.tameable.blockPosition());
        return this.world.noCollision(this.tameable, this.tameable.getBoundingBox().move(blockPos));
    }

    protected int getRandomInt(int min, int max) {
        return this.tameable.getRandom().nextInt(max - min + 1) + min;
    }

    protected boolean getConfigCanTeleportOwnerForwards() {
        return LMRBConfig.get().movement.canTeleportOwnerForwards;
    }

    protected float getConfigOwnerForwardRange() {
        return LMRBConfig.get().movement.ownerForwardRange;
    }

    protected int getConfigMaxTryTeleportCount() {
        return LMRBConfig.get().movement.maxTryTeleportCount;
    }

    protected int getConfigTeleportWidthRange() {
        return LMRBConfig.get().movement.teleportWidth;
    }

    protected int getConfigTeleportHeightRange() {
        return LMRBConfig.get().movement.teleportHeight;
    }
}
