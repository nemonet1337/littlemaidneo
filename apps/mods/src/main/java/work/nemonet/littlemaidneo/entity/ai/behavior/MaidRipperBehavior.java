package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.Lists;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.mode.ModeHelpers;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;

public class MaidRipperBehavior extends AbstractMaidBehavior {
    protected final float radius = 8F;
    protected final Queue<Entity> shearable = Lists.newLinkedList();
    protected int timeToRecalcPath;
    protected int timeToIgnore;
    protected int cool;

    public MaidRipperBehavior() {
        super(Map.of(
                work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_JOB_NAME.get(), MemoryStatus.VALUE_PRESENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity mob) {
        String job = mob.getBrain().getMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_JOB_NAME.get()).orElse("");
        if (!job.equals("ripper")) {
            return false;
        }
        if (0 < cool--) {
            return false;
        }
        cool = 40;
        this.shearable.addAll(findCanShearableMob(mob));
        return !this.shearable.isEmpty();
    }

    public Collection<Entity> findCanShearableMob(LittleMaidEntity mob) {
        AABB bb = new AABB(
                mob.getX() + radius,
                mob.getY() + radius / 2F,
                mob.getZ() + radius,
                mob.getX() - radius,
                mob.getY() - radius / 2F,
                mob.getZ() - radius);
        return mob.level().getEntities(mob, bb,
                (entity) -> entity instanceof LivingEntity && entity instanceof Shearable
                        && ((Shearable) entity).readyForShearing());
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        String job = mob.getBrain().getMemory(work.nemonet.littlemaidneo.setup.ModRegistration.ACTIVE_JOB_NAME.get()).orElse("");
        if (!job.equals("ripper")) {
            return false;
        }
        return !this.shearable.isEmpty();
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity mob, long gameTime) {
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
        var navResult = ModeHelpers.approach(mob, target, 1.0f, timeToRecalcPath, 10, 2.5, 1);
        timeToRecalcPath = navResult.nextTimer;
        if (navResult.unreachable) {
            this.shearable.remove();
            this.timeToIgnore = 0;
            return;
        }
        if (target.distanceToSqr(mob) >= 2.5f * 2.5f) {
            return;
        }

        ItemStack stack = mob.getMainHandItem();
        if (((Shearable) target).readyForShearing()) {
            ((Shearable) target).shear((ServerLevel) mob.level(), SoundSource.PLAYERS, stack);
            stack.hurtAndBreak(1, mob, EquipmentSlot.MAINHAND);
        }
        this.shearable.remove();
        this.timeToIgnore = 0;
        mob.getNavigation().stop();
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        this.timeToIgnore = 0;
        this.timeToRecalcPath = 0;
        this.shearable.clear();
    }
}
