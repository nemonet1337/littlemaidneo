package work.nemonet.littlemaidneo.entity.goal;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.MovingMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.util.BlockFinderPD;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * アイテムをコンテナに仕舞うゴール。
 * <p>
 * 旧 {@code StoreItemToContainerGoal<LittleMaidEntity>} + {@code LMStoreItemToContainerGoal<T>} を 1 クラスに統合。
 */
public class LMStoreItemToContainerGoal<T extends LittleMaidEntity> extends Goal {

    protected final T mob;
    protected final Predicate<ItemStack> exceptItems;
    protected final Supplier<Float> searchRangeSq;
    @Nullable
    protected BlockPos containerPos;
    @Nullable
    protected BlockFinderPD blockFinder;
    protected int count;

    public LMStoreItemToContainerGoal(T mob, Predicate<ItemStack> exceptItems, Supplier<Float> searchRange) {
        this.mob = mob;
        this.exceptItems = exceptItems;
        this.searchRangeSq = () -> searchRange.get() * searchRange.get();
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // ESCORTモード中・ストライキ中・待機中は発動しない
        if (this.mob.isStrike()) return false;
        if (TameableUtil.getTameOwnerUuid(mob).isEmpty()) return false;
        if (TameableUtil.isWait(mob)) return false;
        if (this.mob.getMovingMode() != MovingMode.FREEDOM
                && this.mob.getMovingMode() != MovingMode.TRACER) return false;

        boolean runningBF = blockFinder != null && !blockFinder.isEnd() && count++ < 1000;
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
        if (this.mob.getRandom().nextInt(20) == 0 && hasStoreItems()) {
            bootBF();
        }
        return false;
    }

    public void bootBF() {
        this.count = 0;
        float searchRangeSq = this.searchRangeSq.get();
        blockFinder = new BlockFinderPD(
                ImmutableList.of(this.mob.blockPosition().above()),
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

    protected boolean hasStoreItems() {
        var inventory = this.mob.getInventory();
        boolean hasStoreItem = false;
        for (int i = this.mob.getWorkItemSlotSize(); i < inventory.getContainerSize() - 1; i++) {
            var stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            if (!hasStoreItem && !this.exceptItems.test(stack)) {
                hasStoreItem = true;
            }
        }
        return hasStoreItem;
    }

    protected void storeItems() {
        if (containerPos == null) return;

        var container = HopperBlockEntity.getContainerAt(this.mob.level(), containerPos);
        if (container == null) return;

        this.mob.level().playSound(null, containerPos,
                SoundEvents.CHEST_OPEN, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        this.mob.swing(InteractionHand.MAIN_HAND);

        if (this.mob.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            double cx = containerPos.getX() + 0.5;
            double cy = containerPos.getY() + 0.5;
            double cz = containerPos.getZ() + 0.5;
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, cx, cy + 0.5, cz, 5, 0.3, 0.3, 0.3, 0.0);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE, this.mob.getX(), this.mob.getY() + 1.0, this.mob.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
        }

        var inventory = this.mob.getInventory();
        for (int i = this.mob.getWorkItemSlotSize(); i < inventory.getContainerSize() - 1; i++) {
            var stack = inventory.getItem(i);
            if (this.exceptItems.test(stack)) continue;
            var newStack = HopperBlockEntity.addItem(inventory, container, stack, Direction.UP);
            inventory.setItem(i, newStack);
        }
    }

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
