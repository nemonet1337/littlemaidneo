package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.setup.ModRegistration;

public class MaidWorkModeBehavior extends Behavior<LittleMaidEntity> {

    public MaidWorkModeBehavior() {
        super(ImmutableMap.of(
                ModRegistration.IS_WAITING.get(), MemoryStatus.VALUE_ABSENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity entity) {
        if (entity.isStrike()) return false;
        if (entity.isEmergency() && !entity.getConfig().health.enableWorkInEmergency) return false;
        
        return entity.getMode().map(mode -> mode.shouldExecute()).orElse(false);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        if (entity.isStrike()) return false;
        if (entity.isEmergency() && !entity.getConfig().health.enableWorkInEmergency) return false;

        return entity.getMode().map(mode -> mode.shouldContinueExecuting()).orElse(false);
    }

    @Override
    protected void start(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        entity.getMode().ifPresent(mode -> mode.startExecuting());
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        entity.getMode().ifPresent(mode -> mode.resetTask());
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        entity.getMode().ifPresent(mode -> mode.tick());
    }
}
