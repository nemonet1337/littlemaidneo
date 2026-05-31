package work.nemonet.littlemaidneo.entity.goal;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.state.BlockState;
import work.nemonet.littlemaidneo.config.LMRBConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.MovingMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

/**
 * ゴール: 赤石動力を探知して移動する
 */
public class RedstoneTraceGoal extends Goal {

    protected final LittleMaidEntity mob;
    protected final Supplier<Float> speed;
    protected final List<BlockPos> aroundSignalPos = Lists.newArrayList();
    protected int recalcTimer;

    public RedstoneTraceGoal(LittleMaidEntity mob, Supplier<Float> speed) {
        this.mob = mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!TameableUtil.hasTameOwner(this.mob)) {
            return false;
        }

        // 実行に失敗した場合、遅延される
        if (0 < recalcTimer) {
            recalcTimer--;
            return false;
        }
        if (TameableUtil.isWait(mob) ||
                mob.getMovingMode() != MovingMode.TRACER ||
                !this.mob.getNavigation().isDone()) {
            return false;
        }
        this.aroundSignalPos.clear();
        getAroundSignalPoses()
                // 現在位置にあるposは除外する。ただし高度は無視
                // getBlockPos()で判定してもいいが、実装的に動作しない場合があり得るので安全のためこちらに
                // TODO getBlockPos()で判定して動作させる
                .filter(
                        pos -> Mth.floor(this.mob.getX()) != pos.getX() ||
                                Mth.floor(this.mob.getZ()) != pos.getZ())
                .forEach(this.aroundSignalPos::add);
        // このタイマーは実行完了時にリセットされる
        // そのため、連続実行時は遅延無し
        recalcTimer = adjustedTickDelay(20);
        return !this.aroundSignalPos.isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        return (!TameableUtil.isWait(mob) &&
                mob.getMovingMode() == MovingMode.TRACER &&
                !this.mob.getNavigation().isDone());
    }

    @Override
    public void start() {
        this.aroundSignalPos
                .stream()
                .min(
                        Comparator.comparingDouble(
                                pos ->
                                // Prioritize signals closest to 55 degrees on the left side (yaw difference)
                                // and prioritize positions with a higher Y coordinate.
                                -Mth.degreesDifference(getRelYaw(pos), 55f) +
                                        180f -
                                        pos.getY()))
                .ifPresent(pos -> {
                    var navigation = this.mob.getNavigation();
                    if (navigation.moveTo(
                            navigation.createPath(pos, 0),
                            this.speed.get())) {
                        // Update origin of freedom movement to the vicinity of the signal
                        this.mob.setFreedomPos(pos);
                    } else {
                        navigation.stop();
                    }
                });
    }

    @Override
    public void tick() {
        super.tick();
        // Limit head rotation relative to body to prevent sudden/scary neck twisting on
        // turns
        float maxAngle = 50f;
        float diff = Mth.wrapDegrees(this.mob.getYHeadRot() - this.mob.yBodyRot);
        if (Math.abs(diff) > maxAngle) {
            this.mob.setYHeadRot(this.mob.yBodyRot + Mth.clamp(diff, -maxAngle, maxAngle));
        }
    }

    @Override
    public void stop() {
        this.recalcTimer = 0;
    }

    protected Stream<BlockPos> getAroundSignalPoses() {
        int horizon = LMRBConfig.get().movement.tracerHorizonRange;
        int vertical = LMRBConfig.get().movement.tracerVerticalRange;
        return BlockPos.betweenClosedStream(
                this.mob.blockPosition().offset(horizon, vertical, horizon),
                this.mob.blockPosition().offset(-horizon, -vertical, -horizon))
                .map(BlockPos::immutable)
                .filter(this::isEmitSignal);
    }

    protected boolean isEmitSignal(BlockPos pos) {
        var state = mob.level().getBlockState(pos);
        return Arrays.stream(Direction.values()).anyMatch(
                direction -> 0 < state.getDirectSignal(this.mob.level(), pos, direction));
    }

    protected float getRelYaw(BlockPos pos) {
        float x = (float) (pos.getX() + 0.5f - this.mob.getX());
        float z = (float) (pos.getZ() + 0.5f - this.mob.getZ());
        float yaw = (float) (-Mth.atan2(x, z) * (180 / Math.PI));
        float mobYaw = this.mob.getYRot();
        return Mth.degreesDifference(mobYaw, yaw);
    }
}
