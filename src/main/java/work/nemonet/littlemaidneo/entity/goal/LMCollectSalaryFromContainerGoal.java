package work.nemonet.littlemaidneo.entity.goal;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.pathfinder.Path;
import work.nemonet.littlemaidneo.LMRBMod;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;


public class LMCollectSalaryFromContainerGoal<T extends LittleMaidEntity> extends CollectItemFromContainerGoal<T> {
    @Nullable
    protected BlockPos prevWaitPos;
    protected int moveToPrevWaitPosTime;

    public LMCollectSalaryFromContainerGoal(T mob) {
        super(mob);
    }

    @Override
    public boolean canUse() {
        return this.mob.itemContractable.hasSalaryBoxPositions()
                && TameableUtil.hasTameOwner(this.mob)
                && super.canUse();
    }

    @Override
    public void start() {
        super.start();
        if (TameableUtil.isWait(mob)) {
            prevWaitPos = this.mob.blockPosition();
            moveToPrevWaitPosTime = 0;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() || prevWaitPos != null;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.targetContainerPos == null && prevWaitPos != null) {
            if (prevWaitPos.equals(this.mob.blockPosition())
            || moveToPrevWaitPosTime++ > getConfigMaxMoveTimePrevPos()) {
                prevWaitPos = null;
                return;
            }

            var nav = this.mob.getNavigation();
            var path = nav.createPath(prevWaitPos, 0);
            if (path != null) {
                nav.moveTo(path, 1);
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        this.prevWaitPos = null;
        moveToPrevWaitPosTime = 0;
    }

    @Override
    protected ItemStack transfer(ItemStack stack) {
        return HopperBlockEntity.addItem(null, this.mob.getInventory(), stack, null);
    }

    @Override
    protected boolean isTargetItem(ItemStack stack) {
        return this.mob.itemContractable.isSalary(stack);
    }

    @Override
    protected void postCollect() {
        mob.swing(InteractionHand.MAIN_HAND);
        mob.playSound(SoundEvents.ITEM_PICKUP,
                1.0F, mob.getRandom().nextFloat() * 0.1F + 1.0F);
    }

    @Override
    protected boolean shouldCollect() {
        int salarySlots = this.mob.itemContractable.checkSalarySlots();
        return salarySlots <= getConfigMinSalarySlots();
    }

    @Override
    protected boolean canCollectState() {
        boolean hasEmptySlot = false;
        int salarySlots = this.mob.itemContractable.checkSalarySlots();
        var inv = this.mob.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            var stack = inv.getItem(i);
            if (stack.isEmpty()) {
                hasEmptySlot = true;
                break;
            }
        }
        return hasEmptySlot && salarySlots < getConfigMaxSalarySlots();
    }

    @Override
    protected Optional<BlockPos> searchContainerPos() {
        var salaryBoxList = this.mob.itemContractable.getSalaryBoxPositions();

        if (salaryBoxList.isEmpty()) {
            return Optional.empty();
        }

        List<BlockPos> newSalaryBoxList = Lists.newArrayList();

        BlockPos result = null;
        Path resultPath = null;
        int minDistance = Integer.MAX_VALUE;
        for (BlockPos pos : salaryBoxList) {
            if (getAvailableContainer(pos).isEmpty()) {
                continue;
            }

            var distance = (int) pos.distToCenterSqr(this.mob.position());

            var sq = getConfigSalaryBoxRange() * getConfigSalaryBoxRange();
            if (distance > sq) {
                continue;
            }

            var nav = this.mob.getNavigation();
            var path = nav.createPath(pos, 1);
            if (path == null
                    || path.getEndNode() == null
                    || !this.isInCollectRange(pos, BlockPos.containing(path.getEndNode().asVec3()))) {
                continue;
            }

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

    protected int getConfigMaxSalarySlots() {
        return LMRBMod.getConfig().contract.maxAutoSalaryReceiptSlotSize;
    }

    protected int getConfigMinSalarySlots() {
        return LMRBMod.getConfig().contract.startAutoSalaryReceiptSlotThreshold;
    }

    protected float getConfigSalaryBoxRange() {
        return LMRBMod.getConfig().contract.searchSalaryBoxDistance;
    }

    protected int getConfigMaxMoveTimePrevPos() {
        return LMRBMod.getConfig().contract.maxMoveTimeAfterAutoSalaryReceipt;
    }

}
