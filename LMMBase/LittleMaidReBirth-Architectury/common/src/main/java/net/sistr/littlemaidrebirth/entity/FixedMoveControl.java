package net.sistr.littlemaidrebirth.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * ストレイフの後に横滑りするのを修正したクラス
 */
public class FixedMoveControl extends MoveControl {

    public FixedMoveControl(Mob entity) {
        super(entity);
    }

    @Override
    public void setWantedPosition(double x, double y, double z, double speed) {
        super.setWantedPosition(x, y, z, speed);
        this.strafeForwards = 0;
        this.strafeRight = 0;
        this.mob.setZza(0);
        this.mob.setXxa(0);
    }

    @Override
    public void tick() {
        if (this.operation == Operation.WAIT) {
            this.mob.setZza(0);
            this.mob.setXxa(0);
            return;
        }
        super.tick();
    }
}
