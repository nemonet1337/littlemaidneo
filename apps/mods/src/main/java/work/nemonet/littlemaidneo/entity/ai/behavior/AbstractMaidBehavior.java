package work.nemonet.littlemaidneo.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.MaidJob;

import java.util.Map;

/**
 * メイドさん用Behaviorの共通基底クラス。
 *
 * <p>Brain の {@link Behavior} は「開始条件」「継続条件」をそれぞれ
 * {@link #checkExtraStartConditions} と {@link #canStillUse} で明示する。
 * ここでは両方とも既定 {@code false}（override 必須）とする — override を忘れると
 * いつまでも動き続ける permissive な既定は、むしろ新たなフットガンだったため。
 *
 * <p>ジョブ判定は {@link #activeJob} / {@link #requireJob} に集約する
 * （旧来の {@code getMemory(ACTIVE_JOB_NAME).orElse("")} + 文字列比較の重複を排除）。
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
        return false;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        return false;
    }

    /**
     * 現在有効なジョブ。ジョブ未設定／ストライキ中は {@link MaidJob#NONE}。
     * （ストライキ中は {@code MaidBrain.updateActivity} が WORK を選ばないため、
     * 作業系 Behavior はそもそも評価されない。）
     */
    protected MaidJob activeJob(LittleMaidEntity mob) {
        return mob.getActiveJob();
    }

    /** 指定ジョブが有効か。 */
    protected boolean requireJob(LittleMaidEntity mob, MaidJob job) {
        return activeJob(mob) == job;
    }
}
