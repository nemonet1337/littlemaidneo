package work.nemonet.littlemaidneo.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import work.nemonet.littlemaidneo.config.LMRBConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;

import java.util.Map;

public class MaidAvoidBehavior extends AbstractMaidBehavior {
    private LivingEntity avoidTarget;
    private int cooldown;

    public MaidAvoidBehavior() {
        super(Map.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity mob) {
        mob.fleeEntities.entrySet().removeIf(entry -> entry.getValue().test(entry.getKey()));

        if (mob.fleeEntities.isEmpty()) {
            return false;
        }

        double avoidDist = LMRBConfig.get().target.dangerousAvoidDistance;
        LivingEntity nearest = null;
        double nearestDistSq = avoidDist * avoidDist;

        for (LivingEntity danger : mob.fleeEntities.keySet()) {
            if (danger.isAlive()) {
                double distSq = mob.distanceToSqr(danger);
                if (distSq < nearestDistSq) {
                    nearest = danger;
                    nearestDistSq = distSq;
                }
            }
        }

        if (nearest != null) {
            this.avoidTarget = nearest;
            return true;
        }
        return false;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        return this.avoidTarget != null && this.avoidTarget.isAlive() && mob.distanceToSqr(this.avoidTarget) < 16.0 * 16.0;
    }

    @Override
    protected void start(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        this.cooldown = 0;
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        mob.fleeEntities.entrySet().removeIf(entry -> entry.getValue().test(entry.getKey()));

        if (this.avoidTarget == null || !this.avoidTarget.isAlive()) {
            this.avoidTarget = null;
            return;
        }

        if (--this.cooldown <= 0) {
            this.cooldown = 10;
            Vec3 avoidPos = DefaultRandomPos.getPosAway(mob, 16, 7, this.avoidTarget.position());
            if (avoidPos != null) {
                float walkSpeed = LMRBConfig.get().movement.followSpeed;
                float sprintSpeed = LMRBConfig.get().movement.sprintSpeed;
                float speed = mob.distanceToSqr(this.avoidTarget) < 49.0 ? sprintSpeed : walkSpeed;
                mob.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(avoidPos, speed, 1));
            }
        }
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        this.avoidTarget = null;
        mob.getNavigation().stop();
        mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }
}
