package work.nemonet.littlemaidneo.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

import java.util.Map;

public class MaidPanicBehavior extends AbstractMaidBehavior {

    public MaidPanicBehavior() {
        super(Map.of());
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity mob) {
        if (TameableUtil.hasTameOwner(mob)) {
            return false;
        }
        boolean recentlyHurt = mob.getLastHurtByMob() != null
                && mob.tickCount - mob.getLastHurtByMobTimestamp() < 100;
        return recentlyHurt || mob.isOnFire();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        return checkExtraStartConditions(level, mob);
    }

    @Override
    protected void start(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        setEscapeWalk(level, mob);
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        if (!mob.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET) || mob.getNavigation().isDone()) {
            setEscapeWalk(level, mob);
        }
    }

    private void setEscapeWalk(ServerLevel level, LittleMaidEntity mob) {
        Vec3 escapePos = findEscapePos(level, mob);
        if (escapePos != null) {
            float speed = LittleMaidEntity.getConfig().movement.escapeSpeed;
            mob.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(escapePos, speed, 1));
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
