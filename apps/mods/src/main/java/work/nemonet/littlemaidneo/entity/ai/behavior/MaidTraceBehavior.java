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
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;
import work.nemonet.littlemaidneo.config.LMNConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.MaidMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.setup.ModRegistration;

/**
 * Brain Behavior: 赤石動力を探知して移動する（旧 {@code RedstoneTraceGoal} の移植・AI-3）。
 *
 * <p>TRACER モードのみで起動する。到達可能な信号を選んで WALK_TARGET に書き、
 * 経路不能・スタック時の再計画で迷子を抑える。
 */
public class MaidTraceBehavior extends AbstractMaidBehavior {

    private final List<BlockPos> aroundSignalPos = Lists.newArrayList();
    private int recalcTimer;
    /** 現在向かっている信号（経路再評価・除外用）。 */
    @Nullable
    private BlockPos currentTarget;
    /** スタック検知用: 前回位置。 */
    @Nullable
    private BlockPos lastPos;
    private int stuckTicks;

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
        if (entity.getMaidMode() != MaidMode.TRACER) {
            return false;
        }
        this.aroundSignalPos.clear();
        getAroundSignalPoses(entity)
                // 現在立っている列(X/Z)にある pos は除外する（高度は無視）。
                .filter(pos -> Mth.floor(entity.getX()) != pos.getX()
                        || Mth.floor(entity.getZ()) != pos.getZ())
                // 直前に失敗した目標は少しの間避ける（同じ到達不能点へのループ防止）
                .filter(pos -> currentTarget == null || !pos.equals(currentTarget))
                .forEach(this.aroundSignalPos::add);
        // 候補が currentTarget 除外だけで空になったら除外を解除して再収集
        if (this.aroundSignalPos.isEmpty() && currentTarget != null) {
            currentTarget = null;
            getAroundSignalPoses(entity)
                    .filter(pos -> Mth.floor(entity.getX()) != pos.getX()
                            || Mth.floor(entity.getZ()) != pos.getZ())
                    .forEach(this.aroundSignalPos::add);
        }
        recalcTimer = 20;
        return !this.aroundSignalPos.isEmpty();
    }

    @Override
    protected void start(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        float speed = LittleMaidEntity.getConfig().movement.tracerSpeed;
        PathNavigation navigation = entity.getNavigation();
        this.stuckTicks = 0;
        this.lastPos = entity.blockPosition();

        // スコアが良い順に見て、実際に経路が取れる最初の信号へ向かう
        // （経路不能な信号源へ張り付いて迷子になるのを防ぐ）
        this.aroundSignalPos.stream()
                .sorted(Comparator.comparingDouble(pos -> score(entity, pos)))
                .filter(pos -> {
                    Path path = navigation.createPath(pos, 1);
                    return path != null && path.canReach();
                })
                .findFirst()
                .ifPresentOrElse(pos -> {
                    Path path = navigation.createPath(pos, 1);
                    if (path != null && path.canReach()) {
                        entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                                new WalkTarget(Vec3.atCenterOf(pos), speed, 1));
                        this.currentTarget = pos;
                        entity.setFreedomPos(pos);
                    } else {
                        entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                        this.currentTarget = pos;
                        this.recalcTimer = 10;
                    }
                }, () -> {
                    entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                    this.currentTarget = null;
                    this.recalcTimer = 40;
                });
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        if (TameableUtil.isWait(entity) || entity.getMaidMode() != MaidMode.TRACER) {
            return false;
        }
        if (currentTarget == null || entity.blockPosition().closerThan(currentTarget, 2.5)) {
            return false;
        }
        if (isStuck(entity)) {
            entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            this.recalcTimer = 10;
            return false;
        }
        return true;
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        // 進行監視（canStillUse でも見るが、tick で lastPos を更新）
        BlockPos now = entity.blockPosition();
        if (lastPos != null && lastPos.distManhattan(now) == 0) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            lastPos = now;
        }
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        // 到達成功時は currentTarget をクリアし、次の信号へ進める
        if (currentTarget != null && entity.blockPosition().closerThan(currentTarget, 2.5)) {
            currentTarget = null;
            entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
        this.recalcTimer = 0;
        this.stuckTicks = 0;
        this.lastPos = null;
    }

    private boolean isStuck(LittleMaidEntity entity) {
        // 約 2 秒（40 tick）ほぼ動かなければスタック
        return stuckTicks >= 40;
    }

    /**
     * 小さいほど優先。旧ロジック:
     * 左 55 度に近い信号を優先し、Y が高い位置を優先する。
     */
    private double score(LittleMaidEntity entity, BlockPos pos) {
        return -Mth.degreesDifference(getRelYaw(entity, pos), 55f) + 180f - pos.getY();
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
