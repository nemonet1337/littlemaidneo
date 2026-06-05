package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.MaidMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import work.nemonet.littlemaidneo.tags.LMTags;
import work.nemonet.littlemaidneo.util.BlockFinderPD;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class MaidStoreItemBehavior extends Behavior<LittleMaidEntity> {
    @Nullable
    protected BlockPos containerPos;
    @Nullable
    protected BlockFinderPD blockFinder;
    protected int count;

    public MaidStoreItemBehavior() {
        super(ImmutableMap.of(
                ModRegistration.IS_WAITING.get(), MemoryStatus.VALUE_ABSENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity entity) {
        if (entity.isStrike()) return false;
        if (TameableUtil.getTameOwnerUuid(entity).isEmpty()) return false;
        if (TameableUtil.isWait(entity)) return false;
        if (entity.getMaidMode() != MaidMode.FREEDOM
                && entity.getMaidMode() != MaidMode.TRACER) return false;

        boolean runningBF = blockFinder != null && !blockFinder.isEnd() && count++ < 1000;
        if (runningBF) {
            blockFinder.tick();
            var result = blockFinder.getResult();
            if (result.isPresent() && hasStoreItems(entity)) {
                containerPos = result.get();
                return true;
            }
            return false;
        }

        if (entity.getRandom().nextInt(20) == 0 && hasStoreItems(entity)) {
            bootBF(entity);
        }
        return false;
    }

    public void bootBF(LittleMaidEntity entity) {
        this.count = 0;
        float range = (float) entity.getConfig().work.searchContainerRange;
        float searchRangeSq = range * range;
        blockFinder = new BlockFinderPD(
                ImmutableList.of(entity.blockPosition().above()),
                pos -> isContainer(entity, pos),
                pos -> entity.level().isEmptyBlock(pos)
                        && Math.abs(pos.getY() - entity.getY()) < 2
                        && pos.distToCenterSqr(entity.position()) < searchRangeSq,
                Mth.ceil(searchRangeSq));
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        return false;
    }

    protected boolean isContainer(LittleMaidEntity entity, BlockPos pos) {
        BlockState state = entity.level().getBlockState(pos);
        return state.getBlock() instanceof ChestBlock
                || state.getBlock() instanceof BarrelBlock;
    }

    protected boolean hasStoreItems(LittleMaidEntity entity) {
        var inventory = entity.getInventory();
        boolean hasStoreItem = false;
        for (int i = entity.getWorkItemSlotSize(); i < inventory.getContainerSize() - 1; i++) {
            var stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            if (!hasStoreItem && !isExceptItem(entity, stack)) {
                hasStoreItem = true;
            }
        }
        return hasStoreItem;
    }

    protected boolean isExceptItem(LittleMaidEntity entity, ItemStack stack) {
        return stack.is(LMTags.Items.MAIDS_SALARY) ||
                entity.hasModeImpl.getMode()
                        .filter(mode -> mode.getModeType().isModeItem(stack))
                        .isPresent();
    }

    protected void storeItems(LittleMaidEntity entity) {
        if (containerPos == null) return;

        var container = HopperBlockEntity.getContainerAt(entity.level(), containerPos);
        if (container == null) return;

        entity.level().playSound(null, containerPos,
                SoundEvents.CHEST_OPEN, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        entity.swing(InteractionHand.MAIN_HAND);

        if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            double cx = containerPos.getX() + 0.5;
            double cy = containerPos.getY() + 0.5;
            double cz = containerPos.getZ() + 0.5;
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, cx, cy + 0.5, cz, 5, 0.3, 0.3, 0.3, 0.0);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE, entity.getX(), entity.getY() + 1.0, entity.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
        }

        var inventory = entity.getInventory();
        for (int i = entity.getWorkItemSlotSize(); i < inventory.getContainerSize() - 1; i++) {
            var stack = inventory.getItem(i);
            if (isExceptItem(entity, stack)) continue;
            var newStack = HopperBlockEntity.addItem(inventory, container, stack, Direction.UP);
            inventory.setItem(i, newStack);
        }
    }

    @Override
    protected void start(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        storeItems(entity);
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        containerPos = null;
    }
}
