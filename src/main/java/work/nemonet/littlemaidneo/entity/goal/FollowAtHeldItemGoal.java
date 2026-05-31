package work.nemonet.littlemaidneo.entity.goal;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

import java.util.EnumSet;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * アイテムを持ったプレイヤーを見て、近づいたら追従するゴール。
 * <p>
 * 旧 {@code StareAtHeldItemGoal<T>} → {@code TameableStareAtHeldItemGoal<T>} → {@code FollowAtHeldItemGoal<T>}
 * の3段継承を 1 クラスに統合。
 */
public class FollowAtHeldItemGoal<T extends TamableAnimal> extends Goal {

    protected final T mob;
    protected final Supplier<Float> stareAtRange;
    protected final Predicate<ItemStack> targetItem;
    private final boolean isTamed;
    private final Supplier<Float> followRangeSq;
    protected Player stareAt;
    protected int reCalcCool;

    public FollowAtHeldItemGoal(T mob, Supplier<Float> stareAtRange, Predicate<ItemStack> targetItem,
                                Supplier<Float> followRange, boolean isTamed) {
        this.mob = mob;
        this.stareAtRange = stareAtRange;
        this.targetItem = targetItem;
        this.isTamed = isTamed;
        this.followRangeSq = () -> followRange.get() * followRange.get();
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // 飼い慣らし状態チェック
        if (TameableUtil.hasTameOwner(this.mob) != isTamed) return false;
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
        // 視線
        mob.getLookControl().setLookAt(stareAt, 30F, 30F);
        // 追従
        if (mob.distanceToSqr(stareAt) < followRangeSq.get()) {
            mob.getNavigation().stop();
            return;
        }
        if (0 < reCalcCool--) return;
        reCalcCool = adjustedTickDelay(10);
        mob.getNavigation().moveTo(stareAt, 1);
    }
}
