package work.nemonet.littlemaidneo.entity.goal;

import java.util.EnumSet;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class StareAtHeldItemGoal<T extends PathfinderMob> extends Goal {
    protected final T mob;
    protected final Supplier<Float> stareAtRange;
    protected final Predicate<ItemStack> targetItem;
    protected Player stareAt;

    public StareAtHeldItemGoal(T mob, Supplier<Float> stareAtRange, Predicate<ItemStack> targetItem) {
        this.mob = mob;
        this.stareAtRange = stareAtRange;
        this.targetItem = targetItem;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        stareAt = mob.level().getNearestPlayer(mob, stareAtRange.get());
        return stareAt != null && isHeldTargetItem(stareAt);
    }

    @Override
    public boolean canContinueToUse() {
        return isHeldTargetItem(stareAt);
    }

    public boolean isHeldTargetItem(Player player) {
        return targetItem.test(player.getMainHandItem()) || targetItem.test(player.getOffhandItem());
    }

    @Override
    public void tick() {
        mob.getLookControl().setLookAt(stareAt, 30F, 30F);
    }
}
