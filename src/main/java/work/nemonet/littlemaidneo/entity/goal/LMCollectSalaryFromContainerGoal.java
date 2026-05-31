package work.nemonet.littlemaidneo.entity.goal;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.pathfinder.Path;
import work.nemonet.littlemaidneo.config.LMRBConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * 給料箱からアイテム（給料）を回収するゴール。
 * <p>
 * 旧 {@code CollectItemFromContainerGoal<LittleMaidEntity>} + {@code LMCollectSalaryFromContainerGoal<T>}
 * を 1 クラスに統合。
 */
public class LMCollectSalaryFromContainerGoal<T extends LittleMaidEntity> extends Goal {

    protected final T mob;
    @Nullable
    protected BlockPos targetContainerPos;
    @Nullable
    protected Path toContainerPath;
    protected int pathReCalcCool;
    protected int moveToContainerTime;

    @Nullable
    protected BlockPos prevWaitPos;
    protected int moveToPrevWaitPosTime;

    public LMCollectSalaryFromContainerGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.mob.itemContractable.hasSalaryBoxPositions()
                && TameableUtil.hasTameOwner(this.mob)
                && mob.getRandom().nextFloat() <= (1.0f / getConfigCheckInterval())
                && shouldCollect()
                && canCollectState()
                && searchContainerPos().map(pos -> { this.targetContainerPos = pos; return true; }).orElse(false);
    }

    @Override
    public boolean canContinueToUse() {
        return (this.targetContainerPos != null && canCollectState()) || prevWaitPos != null;
    }

    @Override
    public void start() {
        super.start();
        moveToContainerTime = 0;
        if (TameableUtil.isWait(mob)) {
            prevWaitPos = this.mob.blockPosition();
            moveToPrevWaitPosTime = 0;
        }
    }

    @Override
    public void tick() {
        if (targetContainerPos == null) {
            // 待機前位置への帰還
            if (prevWaitPos != null) {
                if (prevWaitPos.equals(this.mob.blockPosition())
                        || moveToPrevWaitPosTime++ > getConfigMaxMoveTimePrevPos()) {
                    prevWaitPos = null;
                    return;
                }
                var nav = this.mob.getNavigation();
                var path = nav.createPath(prevWaitPos, 0);
                if (path != null) nav.moveTo(path, 1);
            }
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
        this.prevWaitPos = null;
        moveToPrevWaitPosTime = 0;
    }

    protected void moveToContainer() {
        if (targetContainerPos == null) return;
        if (!isContainerAvailable()) {
            targetContainerPos = null;
            return;
        }

        var navigation = this.mob.getNavigation();
        if (this.toContainerPath == null || --this.pathReCalcCool <= 0) {
            this.pathReCalcCool = adjustedTickDelay(getConfigPathReCalcCool());
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
        if (targetContainerPos == null) throw new IllegalStateException("Target container pos is null");

        var optional = getAvailableContainer();
        if (optional.isEmpty()) { targetContainerPos = null; return false; }
        var inventory = optional.get();

        boolean collected = false;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (!canCollectState()) break;
            var stack = inventory.getItem(i);
            if (!isTargetItem(stack)) continue;
            stack = transfer(stack);
            inventory.setItem(i, stack);
            collected = true;
        }
        return collected;
    }

    protected ItemStack transfer(ItemStack stack) {
        return HopperBlockEntity.addItem(null, this.mob.getInventory(), stack, null);
    }

    protected boolean isContainerAvailable() {
        return getAvailableContainer().isPresent();
    }

    protected Optional<Container> getAvailableContainer() {
        return getAvailableContainer(this.targetContainerPos);
    }

    protected Optional<Container> getAvailableContainer(BlockPos containerPos) {
        if (containerPos == null) return Optional.empty();
        var world = mob.level();
        boolean touchingAir = false;
        for (Direction direction : Direction.values()) {
            if (world.isEmptyBlock(containerPos.relative(direction))) {
                touchingAir = true;
                break;
            }
        }
        if (!touchingAir) return Optional.empty();
        var blockEntity = world.getBlockEntity(containerPos);
        if (blockEntity instanceof Container inv) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (isTargetItem(inv.getItem(i))) return Optional.of(inv);
            }
        }
        return Optional.empty();
    }

    protected boolean isTargetItem(ItemStack stack) {
        return this.mob.itemContractable.isSalary(stack);
    }

    protected boolean shouldCollect() {
        int salarySlots = this.mob.itemContractable.checkSalarySlots();
        return salarySlots <= getConfigMinSalarySlots();
    }

    protected boolean canCollectState() {
        int salarySlots = this.mob.itemContractable.checkSalarySlots();
        var inv = this.mob.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).isEmpty()) return salarySlots < getConfigMaxSalarySlots();
        }
        return false;
    }

    protected Optional<BlockPos> searchContainerPos() {
        var salaryBoxList = this.mob.itemContractable.getSalaryBoxPositions();
        if (salaryBoxList.isEmpty()) return Optional.empty();

        List<BlockPos> newSalaryBoxList = Lists.newArrayList();
        BlockPos result = null;
        Path resultPath = null;
        int minDistance = Integer.MAX_VALUE;

        for (BlockPos pos : salaryBoxList) {
            if (getAvailableContainer(pos).isEmpty()) continue;

            int distance = (int) pos.distToCenterSqr(this.mob.position());
            float sq = getConfigSalaryBoxRange() * getConfigSalaryBoxRange();
            if (distance > sq) continue;

            var nav = this.mob.getNavigation();
            var path = nav.createPath(pos, 1);
            if (path == null || path.getEndNode() == null
                    || !this.isInCollectRange(pos, BlockPos.containing(path.getEndNode().asVec3()))) continue;

            newSalaryBoxList.add(pos);
            if (distance < minDistance) {
                minDistance = distance;
                result = pos;
                resultPath = path;
            }
        }
        this.mob.itemContractable.setSalaryBoxPositions(newSalaryBoxList);
        if (resultPath != null) {
            this.toContainerPath = resultPath;
            return Optional.of(result);
        }
        return Optional.empty();
    }

    protected void postCollect() {
        mob.swing(InteractionHand.MAIN_HAND);
        mob.playSound(SoundEvents.ITEM_PICKUP, 1.0F, mob.getRandom().nextFloat() * 0.1F + 1.0F);
    }

    protected int getConfigCheckInterval() {
        return LMRBConfig.get().contract.startIntervalOfAutoSalaryReceipt;
    }

    protected int getConfigPathReCalcCool() {
        return LMRBConfig.get().contract.findPathIntervalOfAutoSalaryReceipt;
    }

    protected int getConfigMaxMoveToContainerTime() {
        return LMRBConfig.get().contract.maxMoveTimeOnAutoSalaryReceipt;
    }

    protected int getConfigMaxSalarySlots() {
        return LMRBConfig.get().contract.maxAutoSalaryReceiptSlotSize;
    }

    protected int getConfigMinSalarySlots() {
        return LMRBConfig.get().contract.startAutoSalaryReceiptSlotThreshold;
    }

    protected float getConfigSalaryBoxRange() {
        return LMRBConfig.get().contract.searchSalaryBoxDistance;
    }

    protected int getConfigMaxMoveTimePrevPos() {
        return LMRBConfig.get().contract.maxMoveTimeAfterAutoSalaryReceipt;
    }
}
