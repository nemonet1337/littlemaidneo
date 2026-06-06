package work.nemonet.littlemaidneo.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;

import java.util.Map;

public class MaidLookAroundBehavior extends AbstractMaidBehavior {
    private LivingEntity lookTarget;
    private int lookTime;

    public MaidLookAroundBehavior() {
        super(Map.of());
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity mob) {
        if (mob.getRandom().nextFloat() >= 0.02F) {
            return false;
        }
        Player player = level.getNearestPlayer(mob, 8.0D);
        if (player != null && mob.getRandom().nextFloat() < 0.8F) {
            this.lookTarget = player;
            this.lookTime = 40 + mob.getRandom().nextInt(40);
            return true;
        }
        var list = level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(8.0D, 3.0D, 8.0D), e -> e != mob);
        if (!list.isEmpty()) {
            this.lookTarget = list.get(mob.getRandom().nextInt(list.size()));
            this.lookTime = 40 + mob.getRandom().nextInt(40);
            return true;
        }
        this.lookTarget = null;
        this.lookTime = 20 + mob.getRandom().nextInt(20);
        return true;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        return this.lookTime > 0;
    }

    @Override
    protected void tick(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        this.lookTime--;
        if (this.lookTarget != null) {
            mob.getLookControl().setLookAt(this.lookTarget, 30.0F, 30.0F);
        } else {
            double angle = mob.getRandom().nextFloat() * (Math.PI * 2);
            double x = mob.getX() + Math.cos(angle) * 2.0D;
            double z = mob.getZ() + Math.sin(angle) * 2.0D;
            double y = mob.getEyeY();
            mob.getLookControl().setLookAt(x, y, z, 30.0F, 30.0F);
        }
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity mob, long gameTime) {
        this.lookTarget = null;
    }
}
