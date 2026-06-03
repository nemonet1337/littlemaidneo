package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import work.nemonet.littlemaidneo.config.LMRBConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.MovingMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.setup.ModRegistration;

public class MaidFreedomBehavior extends Behavior<LittleMaidEntity> {
    private int reCalcCool;

    public MaidFreedomBehavior() {
        super(ImmutableMap.of(
                ModRegistration.IS_WAITING.get(), MemoryStatus.VALUE_ABSENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity entity) {
        boolean hasOwner = TameableUtil.hasTameOwner(entity);
        if (hasOwner) {
            return entity.getMovingMode() == MovingMode.FREEDOM;
        } else {
            return true;
        }
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        if (!entity.getNavigation().isDone()) {
            checkFreedomPosBounds(entity);
            return;
        }

        if (reCalcCool-- > 0) {
            return;
        }
        reCalcCool = 20 + entity.getRandom().nextInt(20);

        LMRBConfig config = entity.getConfig();
        float speed = config.movement.freedomSpeed;
        
        Vec3 randPos;
        if (entity.isInWater()) {
            randPos = DefaultRandomPos.getPos(entity, 15, 7);
        } else {
            randPos = LandRandomPos.getPos(entity, 10, 7);
        }

        if (randPos != null) {
            entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(randPos, speed, 1));
        }

        checkFreedomPosBounds(entity);
    }

    private boolean hasOwner(LittleMaidEntity entity) {
        return TameableUtil.hasTameOwner(entity);
    }

    private void checkFreedomPosBounds(LittleMaidEntity entity) {
        if (!hasOwner(entity)) return;

        BlockPos freedomPos = entity.getFreedomPos().orElse(null);
        if (freedomPos == null) return;

        LMRBConfig config = entity.getConfig();
        double distance = config.movement.freedomRange;
        double distanceSq = distance * distance;

        if (freedomPos.distToCenterSqr(entity.position()) >= distanceSq) {
            entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(Vec3.atCenterOf(freedomPos), config.movement.freedomSpeed, (int)(distance * 0.5)));

            if (entity.getNavigation().isDone() && entity.onGround()) {
                if (entity.level().noCollision(entity.getBoundingBox().move(entity.position().scale(-1)).move(Vec3.atCenterOf(freedomPos)))) {
                    entity.randomTeleport(freedomPos.getX() + 0.5D, freedomPos.getY(), freedomPos.getZ() + 0.5D, true);
                    entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                }
            }
        }
    }
}
