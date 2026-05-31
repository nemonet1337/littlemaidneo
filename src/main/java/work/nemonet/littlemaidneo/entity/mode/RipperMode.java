package work.nemonet.littlemaidneo.entity.mode;

import com.google.common.collect.Lists;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import work.nemonet.littlemaidneo.api.mode.Mode;
import work.nemonet.littlemaidneo.api.mode.ModeType;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;

import java.util.Collection;
import java.util.Queue;

public class RipperMode extends Mode {
    protected final LittleMaidEntity mob;
    protected final float radius;
    protected final Queue<Entity> shearable = Lists.newLinkedList();
    protected int timeToRecalcPath;
    protected int timeToIgnore;
    protected int cool;

    public RipperMode(ModeType<? extends Mode> modeType, String name, LittleMaidEntity mob, float radius) {
        super(modeType, name);
        this.mob = mob;
        this.radius = radius;
    }

    @Override
    public boolean shouldExecute() {
        if (0 < cool--) {
            return false;
        }
        cool = 40;
        this.shearable.addAll(findCanShearableMob());
        return !this.shearable.isEmpty();
    }

    public Collection<Entity> findCanShearableMob() {
        AABB bb = new AABB(
                this.mob.getX() + radius,
                this.mob.getY() + radius / 2F,
                this.mob.getZ() + radius,
                this.mob.getX() - radius,
                this.mob.getY() - radius / 2F,
                this.mob.getZ() - radius);
        return this.mob.level().getEntities(this.mob, bb,
                (entity) -> entity instanceof LivingEntity && entity instanceof Shearable
                        && ((Shearable) entity).readyForShearing());
    }

    @Override
    public boolean shouldContinueExecuting() {
        return !this.shearable.isEmpty();
    }

    @Override
    public void tick() {
        if (this.shearable.isEmpty()) {
            return;
        }
        Entity target = this.shearable.peek();
        if (!(target instanceof LivingEntity) || !(target instanceof Shearable)) {
            this.shearable.remove();
            this.timeToIgnore = 0;
            return;
        }
        if (200 < ++this.timeToIgnore) {
            this.shearable.remove();
            this.timeToIgnore = 0;
            return;
        }
        if (target.distanceToSqr(this.mob) < 2.5f * 2.5f) {
            ItemStack stack = this.mob.getMainHandItem();
            if (((Shearable) target).readyForShearing()) {
                ((Shearable) target).shear((ServerLevel) this.mob.level(), SoundSource.PLAYERS, stack);
                stack.hurtAndBreak(1, this.mob, EquipmentSlot.MAINHAND);
            }
            this.shearable.remove();
            this.timeToIgnore = 0;
            this.mob.getNavigation().stop();
            return;
        }
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            var path = this.mob.getNavigation().createPath(target.getX(), target.getY(), target.getZ(), 1);
            if (path == null || path.getEndNode() == null
                    || path.getEndNode().asVec3().add(0.5, 0, 0.5)
                            .distanceToSqr(target.position()) > 2.5f * 2.5f) {
                this.shearable.remove();
                this.timeToIgnore = 0;
            } else {
                this.mob.getNavigation().moveTo(path, 1.0f);
            }
        }
    }

    @Override
    public void resetTask() {
        this.timeToIgnore = 0;
        this.timeToRecalcPath = 0;
        this.shearable.clear();
    }

}
