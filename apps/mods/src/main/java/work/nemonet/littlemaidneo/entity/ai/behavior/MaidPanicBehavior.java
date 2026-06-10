package work.nemonet.littlemaidneo.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

import java.util.Map;

public class MaidPanicBehavior extends AbstractMaidBehavior {
    private final float speed;

    public MaidPanicBehavior(float speed) {
        super(Map.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT
        ));
        this.speed = speed;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity mob) {
        if (TameableUtil.hasTameOwner(mob)) {
            return false;
        }
        return mob.getLastHurtByMob() != null || mob.isOnFire();
    }

    @Override
    protected void start(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        Vec3 escapePos = findEscapePos(level, mob);
        if (escapePos != null) {
            mob.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(escapePos, this.speed, 1));
        }
    }

    private Vec3 findEscapePos(ServerLevel level, LittleMaidEntity mob) {
        if (mob.isOnFire()) {
            BlockPos mobPos = mob.blockPosition();
            BlockPos waterPos = null;
            for (BlockPos pos : BlockPos.betweenClosed(mobPos.offset(-5, -4, -5), mobPos.offset(5, 4, 5))) {
                if (level.getFluidState(pos).is(FluidTags.WATER)) {
                    if (waterPos == null || mobPos.distSqr(pos) < mobPos.distSqr(waterPos)) {
                        waterPos = pos.immutable();
                    }
                }
            }
            if (waterPos != null) {
                return Vec3.atBottomCenterOf(waterPos);
            }
        }
        return DefaultRandomPos.getPos(mob, 5, 4);
    }
}
