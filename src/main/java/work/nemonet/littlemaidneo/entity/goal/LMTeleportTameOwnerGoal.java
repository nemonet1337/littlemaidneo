package work.nemonet.littlemaidneo.entity.goal;

import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.MovingMode;

import java.util.function.Supplier;

public class LMTeleportTameOwnerGoal extends TeleportTameOwnerGoal<LittleMaidEntity> {
    protected final LittleMaidEntity maid;

    public LMTeleportTameOwnerGoal(LittleMaidEntity maid, Supplier<Float> teleportStart) {
        super(maid, teleportStart);
        this.maid = maid;
    }

    @Override
    public boolean canUse() {
        if (this.tameable.getMovingMode() != MovingMode.ESCORT) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.tameable.getMovingMode() != MovingMode.ESCORT) {
            return false;
        }
        return super.canContinueToUse();
    }
}
