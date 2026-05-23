package net.sistr.littlemaidrebirth.entity.goal;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.sistr.littlemaidrebirth.util.BlockFinderPD;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class StoreItemToContainerGoal<T extends PathfinderMob> extends Goal {
    protected final T mob;
    protected final Predicate<ItemStack> exceptItems;
    protected final Supplier<Float> searchRangeSq;
    @Nullable
    protected BlockPos containerPos;
    @Nullable
    protected BlockFinderPD blockFinder;
    protected int count;

    public StoreItemToContainerGoal(T mob, Predicate<ItemStack> exceptItems, Supplier<Float> searchRange) {
        this.mob = mob;
        this.exceptItems = exceptItems;
        this.searchRangeSq = () -> searchRange.get() * searchRange.get();
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        boolean runningBF = blockFinder != null
                && !blockFinder.isEnd()
                && count++ < 1000;
        // BF実行中なら
        if (runningBF) {
            blockFinder.tick();

            var result = blockFinder.getResult();

            // BFの結果が得られて、かつ仕舞うアイテムがあるなら
            if (result.isPresent() && hasStoreItems()) {
                containerPos = result.get();
                return true;
            }

            return false;
        }

        // shouldStoreItemの毎tickチェックを避ける
        if (this.mob.getRandom().nextInt(20) == 0
                && hasStoreItems()) {
            bootBF();
        }
        return false;
    }

    public void bootBF() {
        this.count = 0;
        float searchRangeSq = this.searchRangeSq.get();
        blockFinder = new BlockFinderPD(ImmutableList.of(this.mob.blockPosition().above()),
                this::isContainer,
                pos -> mob.level().isEmptyBlock(pos)
                        && Math.abs(pos.getY() - mob.getY()) < 2
                        && pos.distToCenterSqr(this.mob.position()) < searchRangeSq,
                Mth.ceil(searchRangeSq));
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    protected boolean isContainer(BlockPos pos) {
        BlockState state = mob.level().getBlockState(pos);
        return state.getBlock() instanceof ChestBlock
                || state.getBlock() instanceof BarrelBlock;
    }

    protected abstract boolean hasStoreItems();

    protected abstract void storeItems();

    @Override
    public void start() {
        storeItems();
    }

    @Override
    public void stop() {
        containerPos = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
