package work.nemonet.littlemaidneo.entity.goal;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.state.BlockState;
import work.nemonet.littlemaidneo.LMRBMod;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.MovingMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

//TODO 180度ターン時に首がグリッとなるのがこわい
//TODO この状態では自由行動の起点が最後に検知した赤石動力付近に再設定されます。
//TODO 処理の再実装
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
        if (TameableUtil.isWait(mob)
                || mob.getMovingMode() != MovingMode.TRACER
                || !this.mob.getNavigation().isDone()) {
            return false;
        }
        this.aroundSignalPos.clear();
        getAroundSignalPoses()
                // 現在位置にあるposは除外する。ただし高度は無視
                // getBlockPos()で判定してもいいが、実装的に動作しない場合があり得るので安全のためこちらに
                .filter(pos -> Mth.floor(this.mob.getX()) != pos.getX()
                        || Mth.floor(this.mob.getZ()) != pos.getZ())
                .forEach(this.aroundSignalPos::add);
        // このタイマーは実行完了時にリセットされる
        // そのため、連続実行時は遅延無し
        recalcTimer = adjustedTickDelay(20);
        return !this.aroundSignalPos.isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        return !TameableUtil.isWait(mob)
                && mob.getMovingMode() == MovingMode.TRACER
                && !this.mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.aroundSignalPos
                .stream()
                .min(Comparator.comparingDouble(pos ->
                // 左55度を0として時計回りに一周回し、角度が浅いposを取る
                // あと高度が高い位置を優先して取る
                -Mth.degreesDifference(getRelYaw(pos), 55f) + 180f - pos.getY()))
                .ifPresent(pos -> {
                    var navigation = this.mob.getNavigation();
                    if (!navigation.moveTo(navigation.createPath(pos, 0), this.speed.get())) {
                        navigation.stop();
                    }
                });
    }

    @Override
    public void stop() {
        this.recalcTimer = 0;
    }

    protected Stream<BlockPos> getAroundSignalPoses() {
        int horizon = LMRBMod.getConfig().movement.tracerHorizonRange;
        int vertical = LMRBMod.getConfig().movement.tracerVerticalRange;
        return BlockPos.betweenClosedStream(
                this.mob.blockPosition().offset(horizon, vertical, horizon),
                this.mob.blockPosition().offset(-horizon, -vertical, -horizon))
                .map(BlockPos::immutable)
                .filter(this::isEmitSignal);
    }

    protected boolean isEmitSignal(BlockPos pos) {
        var state = mob.level().getBlockState(pos);
        return Arrays.stream(Direction.values())
                .anyMatch(direction -> 0 < state.getDirectSignal(this.mob.level(), pos, direction));
    }

    protected float getRelYaw(BlockPos pos) {
        float x = (float) (pos.getX() + 0.5f - this.mob.getX());
        float z = (float) (pos.getZ() + 0.5f - this.mob.getZ());
        float yaw = (float) (-Mth.atan2(x, z) * (180 / Math.PI));
        float mobYaw = this.mob.getYRot();
        return Mth.degreesDifference(mobYaw, yaw);
    }

}
