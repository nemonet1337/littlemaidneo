package work.nemonet.littlemaidneo.entity.ai.behavior;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 永続化が必要なデータを持つBehaviorが実装するインターフェイス
 */
public interface PersistentMaidBehavior {
    void writeBehaviorData(ValueOutput output);
    void readBehaviorData(ValueInput input);
}
