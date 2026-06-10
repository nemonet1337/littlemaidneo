package work.nemonet.littlemaidneo.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;

import java.util.Map;

/**
 * メイドさん用Behaviorの共通基底クラス。
 *
 * <p>バニラのBehaviorクラスの「canStillUse()がデフォルトでfalseを返すため、
 * オーバーライドし忘れるとtick()が呼ばれない」というフットガンを解消するため、
 * デフォルトでtrue（継続実行可能）を返す permissive な実装を提供する。
 */
public abstract class AbstractMaidBehavior extends Behavior<LittleMaidEntity> {

    public AbstractMaidBehavior(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState) {
        super(requiredMemoryState);
    }

    public AbstractMaidBehavior(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState, int duration) {
        super(requiredMemoryState, duration);
    }

    public AbstractMaidBehavior(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState, int minDuration, int maxDuration) {
        super(requiredMemoryState, minDuration, maxDuration);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity entity) {
        return true;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        return true;
    }
}
