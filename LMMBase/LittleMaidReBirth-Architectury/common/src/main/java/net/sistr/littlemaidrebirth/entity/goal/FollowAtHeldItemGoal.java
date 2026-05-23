package net.sistr.littlemaidrebirth.entity.goal;

import java.util.EnumSet;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;

public class FollowAtHeldItemGoal<T extends TamableAnimal> extends TameableStareAtHeldItemGoal<T> {
    private final Supplier<Float> followRangeSq;
    protected int reCalcCool;

    public FollowAtHeldItemGoal(T mob, Supplier<Float> stareAtRange, Predicate<ItemStack> targetItem, Supplier<Float> followRange, boolean isTamed) {
        super(mob, stareAtRange, targetItem, isTamed);
        this.followRangeSq = () -> followRange.get() * followRange.get();
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public void tick() {
        super.tick();
        if (mob.distanceToSqr(stareAt) < followRangeSq.get()) {
            mob.getNavigation().stop();
            return;
        }
        if (0 < reCalcCool--) {
            return;
        }
        reCalcCool = adjustedTickDelay(10);
        mob.getNavigation().moveTo(stareAt, 1);
    }
}
