package work.nemonet.littlemaidneo.entity.goal;

import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;

public class PredicateRevengeGoal extends HurtByTargetGoal {
    protected final Predicate<LivingEntity> target;

    public PredicateRevengeGoal(PathfinderMob mob, Predicate<LivingEntity> target, Class<?>... noRevengeTypes) {
        super(mob, noRevengeTypes);
        this.target = target;
    }

    @Override
    public boolean canUse() {
        return super.canUse() && target.test(this.mob.getLastHurtByMob());
    }
}
