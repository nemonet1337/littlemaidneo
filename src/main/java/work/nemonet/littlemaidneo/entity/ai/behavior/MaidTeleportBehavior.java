package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import work.nemonet.littlemaidneo.config.LMRBConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.util.MaidMode;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.setup.ModRegistration;

public class MaidTeleportBehavior extends Behavior<LittleMaidEntity> {
    private LivingEntity owner;
    private int updateCountdownTicks;

    public MaidTeleportBehavior() {
        super(ImmutableMap.of(
                ModRegistration.OWNER.get(), MemoryStatus.VALUE_PRESENT,
                ModRegistration.IS_WAITING.get(), MemoryStatus.VALUE_ABSENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity entity) {
        if (entity.getMaidMode() != MaidMode.ESCORT) return false;

        LivingEntity tameOwner = TameableUtil.getTameOwner(entity).orElse(null);
        if (tameOwner == null || tameOwner.isSpectator()) return false;

        double distanceSq = entity.distanceToSqr(tameOwner);
        LMRBConfig config = entity.getConfig();

        // 通常の追従テレポート条件
        double startDist = config.movement.teleportStartDistance;
        if (distanceSq >= startDist * startDist) {
            this.owner = tameOwner;
            return true;
        }

        return false;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        if (entity.getMaidMode() != MaidMode.ESCORT) return false;
        if (this.owner == null || !this.owner.isAlive()) return false;

        LMRBConfig config = entity.getConfig();
        double startDist = config.movement.teleportStartDistance;

        return entity.distanceToSqr(this.owner) >= startDist * startDist;
    }

    @Override
    protected void start(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        this.updateCountdownTicks = 0;
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        this.owner = null;
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        if (this.owner == null) return;
        entity.getLookControl().setLookAt(this.owner, 10.0f, entity.getMaxHeadXRot());
        
        if (--this.updateCountdownTicks > 0) return;
        this.updateCountdownTicks = 10; // adjustedTickDelay(10) の代わり

        LMRBConfig config = entity.getConfig();
        int width = config.movement.teleportWidth;
        int height = config.movement.teleportHeight;
        
        TameableUtil.tryTeleportToOwner(entity, this.owner, width, height);
    }
}
