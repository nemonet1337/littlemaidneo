package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import work.nemonet.littlemaidneo.config.LMNConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.MaidMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.setup.ModRegistration;

/**
 * Brain Behavior: 赤石動力を探知して移動する（旧 {@code RedstoneTraceGoal} の移植・AI-3）。
 *
 * <p>TRACER モードのみで起動する。これにより全移動モード（ESCORT/FREEDOM/TRACER）が
 * Brain Behavior に統一され、TRACER だけ GoalSelector に残っていた非対称が解消される。
 * 移動は WALK_TARGET ではなく直接 navigation を操作し、旧 Goal の挙動を厳密に維持する。
 * 実行時間上限は実質無制限とし、継続可否は {@link #canStillUse} で旧 {@code canContinueToUse}
 * と同一条件で判定する。
 */
public class MaidTraceBehavior extends AbstractMaidBehavior {

    private final List<BlockPos> aroundSignalPos = Lists.newArrayList();
    private int recalcTimer;

    public MaidTraceBehavior() {
        super(ImmutableMap.of(
                ModRegistration.IS_WAITING.get(), MemoryStatus.VALUE_ABSENT
        ), Integer.MAX_VALUE);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity entity) {
        if (!TameableUtil.hasTameOwner(entity)) {
            return false;
        }
        // 実行に失敗した場合、遅延される（旧 Goal の recalcTimer 互換）。
        if (recalcTimer > 0) {
            recalcTimer--;
            return false;
        }
        if (entity.getMaidMode() != MaidMode.TRACER || !entity.getNavigation().isDone()) {
            return false;
        }
        this.aroundSignalPos.clear();
        getAroundSignalPoses(entity)
                // 現在立っている列(X/Z)にある pos は除外する（高度は無視）。
                .filter(pos -> Mth.floor(entity.getX()) != pos.getX()
                        || Mth.floor(entity.getZ()) != pos.getZ())
                .forEach(this.aroundSignalPos::add);
        recalcTimer = 20;
        return !this.aroundSignalPos.isEmpty();
    }

    @Override
    protected void start(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        float speed = entity.getConfig().movement.tracerSpeed;
        this.aroundSignalPos.stream()
                .min(Comparator.comparingDouble(pos ->
                        // 左 55 度に近い信号を優先し、Y が高い位置を優先する。
                        -Mth.degreesDifference(getRelYaw(entity, pos), 55f) + 180f - pos.getY()))
                .ifPresent(pos -> {
                    var navigation = entity.getNavigation();
                    // accuracy=1 で信号ブロックの「隣」を目標にする（立てない信号源への到達不能を回避）。
                    if (navigation.moveTo(navigation.createPath(pos, 1), speed)) {
                        // 自由行動の原点を信号付近へ更新する。
                        entity.setFreedomPos(pos);
                    } else {
                        navigation.stop();
                    }
                });
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        return !TameableUtil.isWait(entity)
                && entity.getMaidMode() == MaidMode.TRACER
                && !entity.getNavigation().isDone();
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        this.recalcTimer = 0;
    }

    private Stream<BlockPos> getAroundSignalPoses(LittleMaidEntity entity) {
        int horizon = LMNConfig.get().movement.tracerHorizonRange;
        int vertical = LMNConfig.get().movement.tracerVerticalRange;
        return BlockPos.betweenClosedStream(
                entity.blockPosition().offset(horizon, vertical, horizon),
                entity.blockPosition().offset(-horizon, -vertical, -horizon))
                .map(BlockPos::immutable)
                .filter(pos -> isEmitSignal(entity, pos));
    }

    private boolean isEmitSignal(LittleMaidEntity entity, BlockPos pos) {
        var state = entity.level().getBlockState(pos);
        return Arrays.stream(Direction.values()).anyMatch(
                direction -> 0 < state.getDirectSignal(entity.level(), pos, direction));
    }

    private float getRelYaw(LittleMaidEntity entity, BlockPos pos) {
        float x = (float) (pos.getX() + 0.5f - entity.getX());
        float z = (float) (pos.getZ() + 0.5f - entity.getZ());
        float yaw = (float) (-Mth.atan2(x, z) * (180 / Math.PI));
        float mobYaw = entity.getYRot();
        return Mth.degreesDifference(mobYaw, yaw);
    }
}
