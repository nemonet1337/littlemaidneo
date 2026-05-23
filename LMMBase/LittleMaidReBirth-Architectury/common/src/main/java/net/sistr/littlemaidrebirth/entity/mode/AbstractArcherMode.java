package net.sistr.littlemaidrebirth.entity.mode;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

import java.util.Optional;
import java.util.function.Predicate;

public abstract class AbstractArcherMode<T> extends AbstractBattleMode<T> {
    protected final LittleMaidEntity mob;
    protected int seeTime;
    protected boolean strafingClockwise;
    protected boolean strafingBackwards;
    protected int strafingTime = -1;

    public AbstractArcherMode(ModeType<? extends AbstractArcherMode> modeType,
                              String name, LittleMaidEntity mob) {
        super(mob, modeType, name);
        this.mob = mob;
    }

    public void startExecuting() {
        this.mob.setAggressive(true);
        this.mob.setAimingBow(true);
        this.mob.play(LMSounds.FIND_TARGET_N);
        this.mob.getNavigation().stop();
    }

    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }
        double distanceSq = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
        boolean canSee = this.mob.getSensing().hasLineOfSight(target);
        ItemStack itemStack = this.mob.getMainHandItem();
        float maxRange = getMaxRange(itemStack);
        boolean prevCanSee = 0 < this.seeTime;
        //見えなくなるか、見えるようになったら
        if (canSee != prevCanSee) {
            this.seeTime = 0;
        }
        //見えなくなったら
        if (prevCanSee && !canSee) {
            this.strafingTime = 0;
            this.strafingClockwise = !this.strafingClockwise;
        }

        if (canSee) {
            ++this.seeTime;
        } else {
            --this.seeTime;
        }

        //レンジ内
        if (distanceSq < maxRange * maxRange) {
            ++this.strafingTime;
        } else {
            this.strafingTime = 0;
        }

        //1秒ごとに10%の確率で反転
        if (20 <= this.strafingTime) {
            if ((double) this.mob.getRandom().nextFloat() < 0.1D) {
                this.strafingClockwise = !this.strafingClockwise;
            }
            this.strafingTime = 0;
        }

        if (maxRange * maxRange < distanceSq) {
            this.strafingBackwards = false;
        } else if (distanceSq < maxRange * maxRange * 0.75F) {
            this.strafingBackwards = true;
        }

        this.mob.getMoveControl().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
        this.mob.lookAt(target, 30.0F, 30.0F);
        this.mob.getLookControl().setLookAt(target, 30f, 30f);

        tickRangedAttack(target, itemStack, canSee, distanceSq, maxRange);
    }

    protected abstract void tickRangedAttack(LivingEntity target, ItemStack itemStack, boolean canSee, double distanceSq, float maxRange);

    protected abstract float getMaxRange(ItemStack itemStack);

    protected Optional<EntityHitResult> raycastShootLine(LivingEntity target, float maxRange, Predicate<Entity> predicate) {
        var targetAt = target.getEyePosition();
        var toTargetVec = targetAt.subtract(this.mob.getEyePosition()).normalize();
        Vec3 start = this.mob.getEyePosition(1F);
        Vec3 end = start.add(toTargetVec.scale(maxRange));
        AABB box = new AABB(start, end).inflate(1D);
        var result = ProjectileUtil.getEntityHitResult(mob.level(), this.mob, start, end, box, predicate);
        return Optional.ofNullable(result);
    }

    public void resetTask() {
        this.mob.setAggressive(false);
        this.mob.setAimingBow(false);
        this.seeTime = 0;
    }

    @Override
    public BattleModeType getBattleModeType() {
        return BattleModeType.BOW;
    }
}
