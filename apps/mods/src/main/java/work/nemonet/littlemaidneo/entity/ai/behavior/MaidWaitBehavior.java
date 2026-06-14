package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.setup.ModRegistration;

public class MaidWaitBehavior extends AbstractMaidBehavior {
    public MaidWaitBehavior() {
        super(ImmutableMap.of(
                ModRegistration.IS_WAITING.get(), MemoryStatus.VALUE_PRESENT
        ));
    }

    // canStillUse 既定は false のため、override しないと start()/stop() を毎 tick 繰り返し
    // tick() が呼ばれない。待機中は継続して走らせる。
    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        return entity.getBrain().hasMemoryValue(ModRegistration.IS_WAITING.get());
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
