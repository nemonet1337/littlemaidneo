package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.pathfinder.Path;
import net.sistr.littlemaidrebirth.LMRBMod;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;

/**
 * コンテナからアイテムを回収するGaol
 */
public abstract class CollectItemFromContainerGoal<T extends Mob> extends Goal {
    protected final T mob;
    @Nullable
    protected BlockPos targetContainerPos;
    @Nullable
    protected Path toContainerPath;
    protected int pathReCalcCool;
    protected int moveToContainerTime;

    protected CollectItemFromContainerGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.mob.getRandom().nextFloat() > (1.0f / getConfigCheckInterval())) {
            return false;
        }

        if (shouldCollect() && canCollectState()) {
            this.targetContainerPos = searchContainerPos().orElse(null);
            return this.targetContainerPos != null;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetContainerPos != null && canCollectState();
    }

    @Override
    public void start() {
        super.start();
        moveToContainerTime = 0;
    }

    @Override
    public void tick() {
        if (targetContainerPos == null) {
            return;
        }

        if (!isContainerAvailable() || !canCollectState()) {
            targetContainerPos = null;
            return;
        }

        if (!isInCollectRange(targetContainerPos, this.mob.blockPosition())) {
            if (moveToContainerTime++ > getConfigMaxMoveToContainerTime()) {
                this.targetContainerPos = null;
                return;
            }
            moveToContainer();
        } else {
            this.mob.getNavigation().stop();
            if (collect()) {
                postCollect();
                this.targetContainerPos = null;
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        this.targetContainerPos = null;
        this.toContainerPath = null;
        this.pathReCalcCool = 0;
    }

    protected void moveToContainer() {
        if (targetContainerPos == null) {
            return;
        }

        if (!isContainerAvailable()) {
            targetContainerPos = null;
            return;
        }

        // ナビゲーションでコンテナ位置に向かう

        var navigation = this.mob.getNavigation();

        if (this.toContainerPath == null || --this.pathReCalcCool <= 0) {
            this.pathReCalcCool = adjustedTickDelay(getConfigPathReCalcCool());

            // コンテナが利用できない状態、またはコンテナ位置に到達できない場合、ターゲット位置をnullにして終了
            this.toContainerPath = navigation.createPath(targetContainerPos, 1);
            if (toContainerPath == null
                    || toContainerPath.getEndNode() == null
                    || !isInCollectRange(targetContainerPos, toContainerPath.getEndNode().asBlockPos())) {
                targetContainerPos = null;
                return;
            }

            navigation.moveTo(this.toContainerPath, 1);
        }
    }

    protected boolean isInCollectRange(BlockPos containerPos, BlockPos mobPos) {
        return Math.abs(containerPos.getX() - mobPos.getX()) <= 1
                && Math.abs(containerPos.getZ() - mobPos.getZ()) <= 1
                && containerPos.getY() >= mobPos.getY() - 1
                && containerPos.getY() <= mobPos.getY() + Mth.ceil(mob.getBbHeight() - 1) + 2;
    }

    protected boolean collect() {
        if (targetContainerPos == null) {
            throw new IllegalStateException("Target container pos is null");
        }

        var optional = getAvailableContainer();
        if (optional.isEmpty()) {
            targetContainerPos = null;
            return false;
        }
        var inventory = optional.get();

        boolean collected = false;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (!canCollectState()) {
                break;
            }

            var stack = inventory.getItem(i);
            if (!isTargetItem(stack)) continue;

            stack = transfer(stack);

            inventory.setItem(i, stack);
            collected = true;
        }

        return collected;
    }

    protected abstract ItemStack transfer(ItemStack stack);

    protected boolean isContainerAvailable() {
        return getAvailableContainer().isPresent();
    }

    protected Optional<Container> getAvailableContainer() {
        return getAvailableContainer(this.targetContainerPos);
    }

    protected Optional<Container> getAvailableContainer(BlockPos containerPos) {
        var world = mob.getCommandSenderWorld();

        boolean touchingAir = false;
        for (Direction direction : Direction.values()) {
            if (world.isEmptyBlock(containerPos.relative(direction))) {
                touchingAir = true;
                break;
            }
        }
        if (!touchingAir) {
            return Optional.empty();
        }

        var blockEntity = world.getBlockEntity(containerPos);
        if (blockEntity instanceof Container inventory) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (isTargetItem(inventory.getItem(i))) {
                    return Optional.of(inventory);
                }
            }
        }
        return Optional.empty();
    }

    protected abstract boolean isTargetItem(ItemStack stack);

    protected abstract boolean shouldCollect();

    protected abstract boolean canCollectState();

    protected abstract Optional<BlockPos> searchContainerPos();

    protected abstract void postCollect();

    protected int getConfigCheckInterval() {
        return LMRBMod.getConfig().contract.startIntervalOfAutoSalaryReceipt;
    }

    protected int getConfigPathReCalcCool() {
        return LMRBMod.getConfig().contract.findPathIntervalOfAutoSalaryReceipt;
    }

    protected int getConfigMaxMoveToContainerTime() {
        return LMRBMod.getConfig().contract.maxMoveTimeOnAutoSalaryReceipt;
    }
}
