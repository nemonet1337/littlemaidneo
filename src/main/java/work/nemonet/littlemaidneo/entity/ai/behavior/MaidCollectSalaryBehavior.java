package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.pathfinder.Path;
import work.nemonet.littlemaidneo.config.LMRBConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class MaidCollectSalaryBehavior extends AbstractMaidBehavior {

    @Nullable
    protected BlockPos targetContainerPos;
    @Nullable
    protected Path toContainerPath;
    protected int pathReCalcCool;
    protected int moveToContainerTime;

    @Nullable
    protected BlockPos prevWaitPos;
    protected int moveToPrevWaitPosTime;

    public MaidCollectSalaryBehavior() {
        super(ImmutableMap.of(
                ModRegistration.IS_WAITING.get(), MemoryStatus.VALUE_ABSENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity entity) {
        return entity.itemContractable.hasSalaryBoxPositions()
                && TameableUtil.hasTameOwner(entity)
                && entity.getRandom().nextFloat() <= (1.0f / getConfigCheckInterval())
                && shouldCollect(entity)
                && canCollectState(entity)
                && searchContainerPos(entity).map(pos -> { this.targetContainerPos = pos; return true; }).orElse(false);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        return (this.targetContainerPos != null && canCollectState(entity)) || prevWaitPos != null;
    }

    @Override
    protected void start(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        moveToContainerTime = 0;
        if (TameableUtil.isWait(entity)) {
            prevWaitPos = entity.blockPosition();
            moveToPrevWaitPosTime = 0;
        }
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        this.targetContainerPos = null;
        this.toContainerPath = null;
        this.pathReCalcCool = 0;
        this.prevWaitPos = null;
        moveToPrevWaitPosTime = 0;
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        if (targetContainerPos == null) {
            // 待機前位置への帰還
            if (prevWaitPos != null) {
                if (prevWaitPos.equals(entity.blockPosition())
                        || moveToPrevWaitPosTime++ > getConfigMaxMoveTimePrevPos()) {
                    prevWaitPos = null;
                    return;
                }
                var nav = entity.getNavigation();
                var path = nav.createPath(prevWaitPos, 0);
                if (path != null) nav.moveTo(path, 1);
            }
            return;
        }

        if (!isContainerAvailable(entity) || !canCollectState(entity)) {
            targetContainerPos = null;
            return;
        }

        if (!isInCollectRange(entity, targetContainerPos, entity.blockPosition())) {
            if (moveToContainerTime++ > getConfigMaxMoveToContainerTime()) {
                this.targetContainerPos = null;
                return;
            }
            moveToContainer(entity);
        } else {
            entity.getNavigation().stop();
            if (collect(entity)) {
                postCollect(entity);
                this.targetContainerPos = null;
            }
        }
    }

    protected void moveToContainer(LittleMaidEntity entity) {
        if (targetContainerPos == null) return;
        if (!isContainerAvailable(entity)) {
            targetContainerPos = null;
            return;
        }

        var navigation = entity.getNavigation();
        if (this.toContainerPath == null || --this.pathReCalcCool <= 0) {
            this.pathReCalcCool = getConfigPathReCalcCool();
            this.toContainerPath = navigation.createPath(targetContainerPos, 1);
            if (toContainerPath == null
                    || toContainerPath.getEndNode() == null
                    || !isInCollectRange(entity, targetContainerPos, toContainerPath.getEndNode().asBlockPos())) {
                targetContainerPos = null;
                return;
            }
            navigation.moveTo(this.toContainerPath, 1);
        }
    }

    protected boolean isInCollectRange(LittleMaidEntity entity, BlockPos containerPos, BlockPos mobPos) {
        return Math.abs(containerPos.getX() - mobPos.getX()) <= 1
                && Math.abs(containerPos.getZ() - mobPos.getZ()) <= 1
                && containerPos.getY() >= mobPos.getY() - 1
                && containerPos.getY() <= mobPos.getY() + Mth.ceil(entity.getBbHeight() - 1) + 2;
    }

    protected boolean collect(LittleMaidEntity entity) {
        if (targetContainerPos == null) throw new IllegalStateException("Target container pos is null");

        var optional = getAvailableContainer(entity);
        if (optional.isEmpty()) { targetContainerPos = null; return false; }
        var inventory = optional.get();

        boolean collected = false;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (!canCollectState(entity)) break;
            var stack = inventory.getItem(i);
            if (!isTargetItem(entity, stack)) continue;
            stack = transfer(entity, stack);
            inventory.setItem(i, stack);
            collected = true;
        }
        return collected;
    }

    protected ItemStack transfer(LittleMaidEntity entity, ItemStack stack) {
        return HopperBlockEntity.addItem(null, entity.getInventory(), stack, null);
    }

    protected boolean isContainerAvailable(LittleMaidEntity entity) {
        return getAvailableContainer(entity).isPresent();
    }

    protected Optional<Container> getAvailableContainer(LittleMaidEntity entity) {
        return getAvailableContainer(entity, this.targetContainerPos);
    }

    protected Optional<Container> getAvailableContainer(LittleMaidEntity entity, BlockPos containerPos) {
        if (containerPos == null) return Optional.empty();
        var world = entity.level();
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
                if (isTargetItem(entity, inv.getItem(i))) return Optional.of(inv);
            }
        }
        return Optional.empty();
    }

    protected boolean isTargetItem(LittleMaidEntity entity, ItemStack stack) {
        return entity.itemContractable.isSalary(stack);
    }

    protected boolean shouldCollect(LittleMaidEntity entity) {
        int salarySlots = entity.itemContractable.checkSalarySlots();
        return salarySlots <= getConfigMinSalarySlots();
    }

    protected boolean canCollectState(LittleMaidEntity entity) {
        int salarySlots = entity.itemContractable.checkSalarySlots();
        var inv = entity.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).isEmpty()) return salarySlots < getConfigMaxSalarySlots();
        }
        return false;
    }

    protected Optional<BlockPos> searchContainerPos(LittleMaidEntity entity) {
        var salaryBoxList = entity.itemContractable.getSalaryBoxPositions();
        if (salaryBoxList.isEmpty()) return Optional.empty();

        List<BlockPos> newSalaryBoxList = Lists.newArrayList();
        BlockPos result = null;
        Path resultPath = null;
        int minDistance = Integer.MAX_VALUE;

        for (BlockPos pos : salaryBoxList) {
            if (getAvailableContainer(entity, pos).isEmpty()) continue;

            int distance = (int) pos.distToCenterSqr(entity.position());
            float sq = getConfigSalaryBoxRange() * getConfigSalaryBoxRange();
            if (distance > sq) continue;

            var nav = entity.getNavigation();
            var path = nav.createPath(pos, 1);
            if (path == null || path.getEndNode() == null
                    || !this.isInCollectRange(entity, pos, BlockPos.containing(path.getEndNode().asVec3()))) continue;

            newSalaryBoxList.add(pos);
            if (distance < minDistance) {
                minDistance = distance;
                result = pos;
                resultPath = path;
            }
        }
        entity.itemContractable.setSalaryBoxPositions(newSalaryBoxList);
        if (resultPath != null) {
            this.toContainerPath = resultPath;
            return Optional.of(result);
        }
        return Optional.empty();
    }

    protected void postCollect(LittleMaidEntity entity) {
        entity.swing(InteractionHand.MAIN_HAND);
        entity.playSound(SoundEvents.ITEM_PICKUP, 1.0F, entity.getRandom().nextFloat() * 0.1F + 1.0F);
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
