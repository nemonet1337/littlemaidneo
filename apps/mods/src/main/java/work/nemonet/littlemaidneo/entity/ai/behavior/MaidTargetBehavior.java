package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import work.nemonet.littlemaidneo.config.LMNConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.targeting.TargetTagManager;
import work.nemonet.littlemaidneo.entity.targeting.TargetingConfig;
import work.nemonet.littlemaidneo.entity.targeting.TargetingSystem;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

import java.util.List;

public class MaidTargetBehavior extends AbstractMaidBehavior {
    private Mob target;
    private int recalc = 0;

    public MaidTargetBehavior() {
        super(ImmutableMap.of());
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity entity) {
        int chance = LMNConfig.get().target.targetingFrequency;
        if (entity.getRandom().nextInt(chance) != 0) {
            return false;
        }

        return targeting(entity);
    }

    private boolean targeting(LittleMaidEntity entity) {
        var aroundMobs = getAroundMobs(entity);
        if (aroundMobs.isEmpty()) {
            clearTarget(entity);
            return false;
        }
        var aroundMaids = getAroundMaids(entity);

        var targetOpt = TargetingSystem.selectTarget(
            new TargetingSystem.Maid(entity),
            aroundMobs.stream().map(TargetingSystem.Mob::new).toList(),
            TameableUtil.getTameOwner(entity).map(TargetingSystem.Master::new).orElse(null),
            aroundMaids.stream().map(TargetingSystem.Maid::new).toList(),
            entity.isBloodSuck(),
                entity
        );

        var enemies = aroundMobs.stream().map(TargetingSystem.Mob::new).toList();
        var maidWrapper = new TargetingSystem.Maid(entity);
        if (TargetingSystem.needsEvacuation(maidWrapper, enemies, entity)) {
            TargetingSystem.getDangerousEnemies(maidWrapper, enemies, entity).forEach(mob ->
                entity.addFleeEntity(
                    mob.getMob(),
                    e -> !e.isAlive() || entity.distanceToSqr(e) > (TargetingConfig.getDangerousAvoidDistance() + 4) * (TargetingConfig.getDangerousAvoidDistance() + 4)
                )
            );
        }

        if (targetOpt.isPresent()) {
            this.target = targetOpt.get();
            setAttackTarget(entity, targetOpt.get());
            return true;
        }

        clearTarget(entity);
        return false;
    }

    private static void setAttackTarget(LittleMaidEntity entity, Mob target) {
        entity.setTarget(target);
        entity.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
    }

    private static void clearTarget(LittleMaidEntity entity) {
        entity.setTarget(null);
        entity.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        if (entity.getLastHurtByMobTimestamp() == entity.tickCount - 1) {
            return targeting(entity);
        }
        if (!isTargetable(entity, this.target, TargetingConfig.getAlertRange())) {
            return targeting(entity);
        }
        recalc = Math.max(0, recalc - 1);
        if (recalc > 0) {
            return true;
        }
        recalc = 10;
        return targeting(entity);
    }

    @Override
    protected void start(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        recalc = 10;
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        recalc = 0;
        this.target = null;
        clearTarget(entity);
    }

    private List<Mob> getAroundMobs(LittleMaidEntity entity) {
        float distance = TargetingConfig.getAlertRange();
        return entity.level().getEntitiesOfClass(
            Mob.class,
            entity.getBoundingBox().inflate(distance, distance / 2f, distance).inflate(1),
            mob -> mob != entity && isTargetable(entity, mob, distance) && entity.getSensing().hasLineOfSight(mob)
        );
    }

    private boolean isTargetable(LittleMaidEntity entity, Mob mob, float distance) {
        return mob != null && (
            entity.distanceToSqr(mob) <= distance * distance &&
            entity.canAttack(mob) &&
            mob.isAlive()
        );
    }

    private List<LittleMaidEntity> getAroundMaids(LittleMaidEntity entity) {
        float distance = TargetingConfig.getAlertRange();
        return entity.level().getEntitiesOfClass(
            LittleMaidEntity.class,
            entity.getBoundingBox().inflate(distance, distance / 2f, distance).inflate(1),
            maid -> maid != entity
        );
    }
}
