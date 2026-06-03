package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.config.LMRBConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import work.nemonet.littlemaidneo.tags.LMTags;

public class MaidStareBehavior extends Behavior<LittleMaidEntity> {
    private Player stareTarget;
    private int reCalcCool;

    public MaidStareBehavior() {
        super(ImmutableMap.of(
                ModRegistration.IS_WAITING.get(), MemoryStatus.VALUE_ABSENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity entity) {
        LMRBConfig config = entity.getConfig();
        boolean hasOwner = TameableUtil.hasTameOwner(entity);
        double range = hasOwner ? config.misc.stareAtSalaryRange : config.misc.stareAtEmployItemRange;

        Player player = level.getNearestPlayer(entity, range);
        if (player != null && isHeldTargetItem(entity, player, hasOwner)) {
            stareTarget = player;
            return true;
        }
        stareTarget = null;
        return false;
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        if (stareTarget == null) return;

        boolean hasOwner = TameableUtil.hasTameOwner(entity);
        if (!isHeldTargetItem(entity, stareTarget, hasOwner)) {
            doStop(entity);
            return;
        }

        LMRBConfig config = entity.getConfig();
        entity.getLookControl().setLookAt(stareTarget, 30.0F, 30.0F);

        double followRange = hasOwner ? config.misc.followAtHeldSalaryRange : config.misc.followAtHeldEmployItemRange;
        double distanceSq = entity.distanceToSqr(stareTarget);
        if (distanceSq < followRange * followRange) {
            entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            entity.getNavigation().stop();
            entity.setBegging(entity.getNavigation().isDone());
        } else {
            entity.setBegging(false);
            if (reCalcCool-- <= 0) {
                reCalcCool = 10;
                entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(
                        new EntityTracker(stareTarget, false),
                        1.0F,
                        (int) followRange
                ));
            }
        }
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        doStop(entity);
    }

    private void doStop(LittleMaidEntity entity) {
        entity.setBegging(false);
        entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        stareTarget = null;
    }

    private boolean isHeldTargetItem(LittleMaidEntity entity, Player player, boolean hasOwner) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (hasOwner) {
            return main.is(LMTags.Items.MAIDS_SALARY) || off.is(LMTags.Items.MAIDS_SALARY);
        } else {
            return main.is(LMTags.Items.MAIDS_EMPLOYABLE) || off.is(LMTags.Items.MAIDS_EMPLOYABLE);
        }
    }
}
