package work.nemonet.littlemaidneo.entity.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.PathType;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

import java.util.EnumSet;
import java.util.function.Supplier;

public class FollowTameOwnerGoal<T extends TamableAnimal> extends Goal {
    protected final T tameable;
    protected final Supplier<Float> speed;
    protected final Supplier<Float> followStartSq;
    protected final Supplier<Float> followEndSq;
    private final PathNavigation navigation;
    private LivingEntity owner;
    private int updateCountdownTicks;
    private float oldWaterPathfindingPenalty;

    public FollowTameOwnerGoal(T tameable, Supplier<Float> speed, Supplier<Float> followStart, Supplier<Float> followEnd) {
        this.tameable = tameable;
        this.speed = speed;
        this.followStartSq = () -> followStart.get() * followStart.get();
        this.followEndSq = () -> followEnd.get() * followEnd.get();
        this.navigation = tameable.getNavigation();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        if (!(tameable.getNavigation() instanceof GroundPathNavigation) && !(tameable.getNavigation() instanceof FlyingPathNavigation)) {
            throw new IllegalArgumentException("Unsupported mob type for FollowOwnerGoal");
        }
    }

    @Override
    public boolean canUse() {
        LivingEntity tameOwner = TameableUtil.getTameOwner(tameable).orElse(null);
        if (tameOwner == null) {
            return false;
        } else if (tameOwner.isSpectator()) {
            return false;
        } else if (TameableUtil.isWait(tameable)) {
            return false;
        } else if (this.tameable.distanceToSqr(tameOwner) < followStartSq.get()) {
            return false;
        } else {
            this.owner = tameOwner;
            return true;
        }
    }

    public boolean canContinueToUse() {
        if (this.navigation.isDone()) {
            return false;
        } else if (TameableUtil.isWait(tameable)) {
            return false;
        } else {
            return followEndSq.get() < this.tameable.distanceToSqr(this.owner);
        }
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
        if (--this.updateCountdownTicks > 0) {
            return;
        }
        this.updateCountdownTicks = adjustedTickDelay(10);
        this.navigation.moveTo(this.owner, this.speed.get());
    }

}
