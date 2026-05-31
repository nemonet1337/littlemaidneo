package work.nemonet.littlemaidneo.entity.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.PathType;
import work.nemonet.littlemaidneo.entity.util.HasMovingMode;
import work.nemonet.littlemaidneo.entity.util.MovingMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

import java.util.EnumSet;
import java.util.function.Supplier;

/**
 * ESCORT モードのメイドさんがご主人様を追いかけるゴール。
 * <p>
 * 旧 {@code FollowTameOwnerGoal<T>} + {@code HasMMFollowTameOwnerGoal<T>} を 1 クラスに統合。
 * ジェネリック境界 {@code T extends TamableAnimal & HasMovingMode} は引き続き有効。
 */
public class HasMMFollowTameOwnerGoal<T extends TamableAnimal & HasMovingMode> extends Goal {

    protected final T tameable;
    protected final Supplier<Float> speed;
    protected final Supplier<Float> followStartSq;
    protected final Supplier<Float> followEndSq;
    private final PathNavigation navigation;
    private LivingEntity owner;
    private int updateCountdownTicks;
    private float oldWaterPathfindingPenalty;

    public HasMMFollowTameOwnerGoal(T tameable, Supplier<Float> speed, Supplier<Float> followStart, Supplier<Float> followEnd) {
        this.tameable = tameable;
        this.speed = speed;
        this.followStartSq = () -> followStart.get() * followStart.get();
        this.followEndSq = () -> followEnd.get() * followEnd.get();
        this.navigation = tameable.getNavigation();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        if (!(tameable.getNavigation() instanceof GroundPathNavigation)
                && !(tameable.getNavigation() instanceof FlyingPathNavigation)) {
            throw new IllegalArgumentException("Unsupported mob type for HasMMFollowTameOwnerGoal");
        }
    }

    @Override
    public boolean canUse() {
        // ESCORTモードのみ追従する
        if (this.tameable.getMovingMode() != MovingMode.ESCORT) return false;

        LivingEntity tameOwner = TameableUtil.getTameOwner(tameable).orElse(null);
        if (tameOwner == null) return false;
        if (tameOwner.isSpectator()) return false;
        if (TameableUtil.isWait(tameable)) return false;
        if (this.tameable.distanceToSqr(tameOwner) < followStartSq.get()) return false;
        this.owner = tameOwner;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.navigation.isDone()) return false;
        if (TameableUtil.isWait(tameable)) return false;
        return followEndSq.get() < this.tameable.distanceToSqr(this.owner);
    }

    @Override
    public void start() {
        this.updateCountdownTicks = 0;
        this.oldWaterPathfindingPenalty = this.tameable.getPathfindingMalus(PathType.WATER);
        this.tameable.setPathfindingMalus(PathType.WATER, 0.0f);
    }

    @Override
    public void stop() {
        this.owner = null;
        this.navigation.stop();
        this.tameable.setPathfindingMalus(PathType.WATER, this.oldWaterPathfindingPenalty);
    }

    @Override
    public void tick() {
        this.tameable.getLookControl().setLookAt(this.owner, 10.0f, this.tameable.getMaxHeadXRot());
        if (--this.updateCountdownTicks > 0) return;
        this.updateCountdownTicks = adjustedTickDelay(10);
        this.navigation.moveTo(this.owner, this.speed.get());
    }
}
