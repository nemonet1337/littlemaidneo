package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.setup.ModRegistration;

public class MaidWaitBehavior extends Behavior<LittleMaidEntity> {
    public MaidWaitBehavior() {
        super(ImmutableMap.of(
                ModRegistration.IS_WAITING.get(), MemoryStatus.VALUE_PRESENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity entity) {
        return true;
    }

    @Override
    protected void start(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        entity.getNavigation().stop();
        entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        entity.getNavigation().stop();
    }
}
