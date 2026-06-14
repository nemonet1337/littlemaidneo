package work.nemonet.littlemaidneo.entity.mode;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.pathfinder.Path;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

/**
 * モード実装間で共有するブロックエンティティ探索・インベントリ走査ユーティリティ（R-4）。
 *
 * <p>各モードに散在していた「指定位置のブロックエンティティを型で取得」「コンテナを走査して
 * 条件に一致するスロットを検索」のボイラープレートを一本化する。挙動は従来実装と等価。
 */
public final class ModeHelpers {

    private ModeHelpers() {
    }

    public record NavigationResult(int nextTimer, boolean unreachable) {
    }

    /**
     * 指定された座標に接近する。
     */
    public static NavigationResult approach(LittleMaidEntity mob, BlockPos targetPos, double speed, int recalcTimer, int recalcInterval, double reachDistance, int closeDistance) {
        double distanceSq = mob.distanceToSqr(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        if (reachDistance * reachDistance < distanceSq) {
            int nextTimer = recalcTimer - 1;
            if (nextTimer <= 0) {
                nextTimer = recalcInterval;
                Path path = mob.getNavigation().createPath(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, closeDistance);
                if (path == null || path.getEndNode() == null || !path.getEndNode().asBlockPos().closerThan(targetPos, reachDistance)) {
                    return new NavigationResult(nextTimer, true);
                }
                mob.getNavigation().moveTo(path, speed);
            }
            return new NavigationResult(nextTimer, false);
        }
        return new NavigationResult(recalcTimer, false);
    }

    /**
     * 指定されたエンティティに接近する。
     */
    public static NavigationResult approach(LittleMaidEntity mob, Entity targetEntity, double speed, int recalcTimer, int recalcInterval, double reachDistance, int closeDistance) {
        double distanceSq = mob.distanceToSqr(targetEntity);
        if (reachDistance * reachDistance < distanceSq) {
            int nextTimer = recalcTimer - 1;
            if (nextTimer <= 0) {
                nextTimer = recalcInterval;
                Path path = mob.getNavigation().createPath(targetEntity, closeDistance);
                if (path == null || path.getEndNode() == null || path.getEndNode().asVec3().add(0.5, 0, 0.5).distanceToSqr(targetEntity.position()) > reachDistance * reachDistance) {
                    return new NavigationResult(nextTimer, true);
                }
                mob.getNavigation().moveTo(path, speed);
            }
            return new NavigationResult(nextTimer, false);
        }
        return new NavigationResult(recalcTimer, false);
    }

    /**
     * 指定位置のブロックエンティティが {@code clazz} 型なら返す。
     * {@code pos} が null、または型が一致しない場合は {@link Optional#empty()}。
     */
    public static <T extends BlockEntity> Optional<T> getBlockEntity(Level level, BlockPos pos, Class<T> clazz) {
        if (pos == null) {
            return Optional.empty();
        }
        BlockEntity tile = level.getBlockEntity(pos);
        return clazz.isInstance(tile) ? Optional.of(clazz.cast(tile)) : Optional.empty();
    }

    /**
     * {@code container} を先頭から走査し、{@code predicate} に最初に一致したスロット番号を返す。
     * 一致がなければ {@link OptionalInt#empty()}。
     */
    public static OptionalInt findSlot(Container container, Predicate<ItemStack> predicate) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (predicate.test(container.getItem(i))) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }
}
