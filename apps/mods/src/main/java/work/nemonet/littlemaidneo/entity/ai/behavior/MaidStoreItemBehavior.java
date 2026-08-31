package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.ai.WorkPoi;
import work.nemonet.littlemaidneo.entity.mode.ModeHelpers;
import work.nemonet.littlemaidneo.entity.util.MaidMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import work.nemonet.littlemaidneo.tags.LMTags;
import work.nemonet.littlemaidneo.util.ItemTransfers;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import org.jetbrains.annotations.Nullable;

public class MaidStoreItemBehavior extends AbstractMaidBehavior {
    @Nullable
    protected BlockPos containerPos;
    protected int timeToRecalcPath;

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
                && entity.getMaidMode() != MaidMode.STROLL
                && entity.getMaidMode() != MaidMode.TRACER) return false;

        if (entity.getRandom().nextInt(20) != 0 || !hasStoreItems(entity)) {
            return false;
        }
        int range = (int) LittleMaidEntity.getConfig().work.searchContainerRange;
        var found = WorkPoi.findClosest(
                level,
                entity.blockPosition(),
                range,
                type -> type.is(ModRegistration.CONTAINER_POI) || type.is(PoiTypes.FISHERMAN),
                pos -> isContainer(entity, pos) && Math.abs(pos.getY() - entity.getY()) < 2);
        if (found.isEmpty()) {
            return false;
        }
        containerPos = found.get();
        return true;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        if (containerPos == null) {
            return false;
        }
        if (!hasStoreItems(entity)) {
            return false;
        }
        return isContainer(entity, containerPos);
    }

    protected boolean isContainer(LittleMaidEntity entity, BlockPos pos) {
        BlockState state = entity.level().getBlockState(pos);
        return state.getBlock() instanceof ChestBlock
                || state.getBlock() instanceof BarrelBlock;
    }

    protected boolean hasStoreItems(LittleMaidEntity entity) {
        var inventory = entity.getInventory();
        boolean hasStoreItem = false;
        for (int i = entity.getWorkItemSlotSize(); i < inventory.getContainerSize(); i++) {
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
                work.nemonet.littlemaidneo.entity.util.MaidJobManager.isModeItemForJob(entity.getActiveJobName(), stack);
    }

    protected void storeItems(LittleMaidEntity entity) {
        if (containerPos == null || !isContainer(entity, containerPos)) return;

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
        for (int i = entity.getWorkItemSlotSize(); i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.isEmpty() || isExceptItem(entity, stack)) continue;
            var newStack = ItemTransfers.insertIntoBlock(entity.level(), containerPos, stack, Direction.UP);
            inventory.setItem(i, newStack);
        }
    }

    @Override
    protected void start(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        timeToRecalcPath = 0;
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        if (containerPos == null) {
            return;
        }
        entity.getLookControl().setLookAt(
                containerPos.getX() + 0.5,
                containerPos.getY() + 0.5,
                containerPos.getZ() + 0.5);
        var navResult = ModeHelpers.approach(entity, containerPos, 1.0, timeToRecalcPath, 10, 1.75, 2);
        timeToRecalcPath = navResult.nextTimer();
        if (navResult.unreachable()) {
            containerPos = null;
            return;
        }
        if (!entity.blockPosition().closerThan(containerPos, 1.75)) {
            return;
        }
        entity.getNavigation().stop();
        storeItems(entity);
        containerPos = null;
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        containerPos = null;
    }
}
