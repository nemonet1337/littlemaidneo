package net.sistr.littlemaidrebirth.entity.goal;

import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;

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
