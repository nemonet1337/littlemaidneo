package work.nemonet.littlemaidneo.entity.goal;

import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import work.nemonet.littlemaidneo.LMRBMod;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;

import java.util.function.Supplier;

public class LMMoveToDropItemGoal extends MoveToDropItemGoal {
    protected final LittleMaidEntity maid;

    public LMMoveToDropItemGoal(LittleMaidEntity maid, Supplier<Float> range, Supplier<Integer> frequency, Supplier<Float> speed) {
        super(maid, range, frequency, speed);
        this.maid = maid;
    }

    @Override
    public boolean isInventoryFull() {
        var inv = this.maid.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean isOwnerRange(Entity entity, Entity owner) {
        Vec3 ownerPos = owner.position();
        Vec3 entityPos = entity.position().subtract(ownerPos);
        Vec3 ownerRot = owner.getViewVector(1F);
        double dot = entityPos.dot(ownerRot);
        double range = LMRBMod.getConfig().movement.ownerForwardRange;
        //プレイヤー位置を原点としたアイテムの位置と、プレイヤーの向きの内積がプラス
        //かつ内積の大きさが4m以下
        return 0 < dot && dot < range * range;
    }
}
