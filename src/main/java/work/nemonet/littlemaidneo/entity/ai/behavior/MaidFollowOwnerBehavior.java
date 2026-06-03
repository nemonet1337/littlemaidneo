package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;
import work.nemonet.littlemaidneo.config.LMRBConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.MovingMode;
import work.nemonet.littlemaidneo.setup.ModRegistration;

public class MaidFollowOwnerBehavior extends Behavior<LittleMaidEntity> {
    public MaidFollowOwnerBehavior() {
        super(ImmutableMap.of(
                ModRegistration.OWNER.get(), MemoryStatus.VALUE_PRESENT,
                ModRegistration.IS_WAITING.get(), MemoryStatus.VALUE_ABSENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity entity) {
        // ご主人様への追従は護衛（ESCORT）モードのみ。FREEDOM/TRACER では
        // それぞれ MaidFreedomBehavior / RedstoneTraceGoal が WALK_TARGET を制御するため、
        // ここで追従すると WALK_TARGET を奪い合い徘徊と追従が競合する（Brain 移行で欠落した条件）。
        return entity.getMovingMode() == MovingMode.ESCORT;
    }

    // Behavior の canStillUse 既定は false で、override しないと tick() が一度も呼ばれない
    // （start() しか実行されない）。本 Behavior はロジックを tick() に持つため必須。
    // 待機・モード変更に追従させるため毎 tick 条件を再評価する。
    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        return checkExtraStartConditions(level, entity)
                && entity.getBrain().hasMemoryValue(ModRegistration.OWNER.get())
                && !entity.getBrain().hasMemoryValue(ModRegistration.IS_WAITING.get());
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        Player owner = entity.getBrain().getMemory(ModRegistration.OWNER.get()).orElse(null);
        if (owner == null) {
            return;
        }

        double distanceSq = entity.distanceToSqr(owner);
        LMRBConfig config = entity.getConfig();

        double followStartDist = config.movement.followStartDistance;
        double followEndDist = config.movement.followEndDistance;
        double sprintStartDist = config.movement.sprintStartDistance;
        double sprintEndDist = config.movement.sprintEndDistance;

        float followSpeed = config.movement.followSpeed;
        float sprintSpeed = config.movement.sprintSpeed;

        if (distanceSq >= sprintStartDist * sprintStartDist) {
            entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(
                    new EntityTracker(owner, false),
                    sprintSpeed,
                    (int) sprintEndDist
            ));
            entity.setSprinting(true);
        } else if (distanceSq >= followStartDist * followStartDist) {
            entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(
                    new EntityTracker(owner, false),
                    followSpeed,
                    (int) followEndDist
            ));
            entity.setSprinting(false);
        } else if (distanceSq <= followEndDist * followEndDist) {
            entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            entity.setSprinting(false);
        }
    }
}
