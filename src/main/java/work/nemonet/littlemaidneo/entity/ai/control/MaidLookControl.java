package work.nemonet.littlemaidneo.entity.ai.control;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;

public class MaidLookControl extends LookControl {
    public MaidLookControl(Mob mob) {
        super(mob);
    }

    @Override
    public void tick() {
        super.tick();
        // 首振り制限（最大50度）
        float maxAngle = 50.0F;
        float diff = Mth.wrapDegrees(this.mob.getYHeadRot() - this.mob.yBodyRot);
        if (Math.abs(diff) > maxAngle) {
            this.mob.setYHeadRot(this.mob.yBodyRot + Mth.clamp(diff, -maxAngle, maxAngle));
        }
    }
}
