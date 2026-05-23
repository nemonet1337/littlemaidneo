package work.nemonet.littlemaidneo.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.pathfinder.Path;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.MovingMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

import java.util.EnumSet;
import java.util.function.Supplier;

//雇い主が居ない場合も実行される
public class FreedomGoal<T extends LittleMaidEntity> extends WaterAvoidingRandomStrollGoal {
    private final T maid;
    private final Supplier<Float> distance;
    private final Supplier<Float> distanceSq;
    private BlockPos freedomPos;
    private int reCalcCool;

    public FreedomGoal(T mob, float speedIn, Supplier<Float> distance) {
        super(mob, speedIn);
        this.maid = mob;
        this.distance = distance;
        this.distanceSq = () -> distance.get() * distance.get();
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return TameableUtil.hasTameOwner(maid)
                && !TameableUtil.isWait(maid)
                && maid.getNavigation().isDone()
                && maid.getMovingMode() == MovingMode.FREEDOM
                && super.canUse();
    }

    @Override
    public void start() {
        super.start();
        freedomPos = this.maid.getFreedomPos().orElse(null);
    }

    @Override
    public void tick() {
        super.tick();

        // 自由行動の起点が存在する場合、起点から自由行動範囲の外に行かないようにする

        if (freedomPos == null) {
            return;
        }
        if (freedomPos.distToCenterSqr(mob.position()) < distanceSq.get()) {
            return;
        }
        if (0 < --reCalcCool) {
            return;
        }
        reCalcCool = adjustedTickDelay(20);
        // freedomPosを目指して移動
        Path path = mob.getNavigation().createPath(
                freedomPos.getX(), freedomPos.getY(), freedomPos.getZ(), Mth.floor(distance.get() * 0.5));
        if (path != null
                && path.getEndNode() != null
                && path.getEndNode().distanceManhattan(freedomPos) < distance.get()) {
            mob.getNavigation().moveTo(path, speedModifier);
            return;
        }
        mob.getNavigation().stop();
        // 移動しても着きそうにない場合はTP
        if (mob.onGround()
                && mob.level().noCollision(
                        mob.getBoundingBox()
                                .move(mob.position().scale(-1))
                                .move(freedomPos))) {
            mob.randomTeleport(freedomPos.getX() + 0.5D, freedomPos.getY(), freedomPos.getZ() + 0.5D, true);
        }

    }

    @Override
    public void stop() {
        super.stop();
        freedomPos = null;
        reCalcCool = 0;
    }
}
