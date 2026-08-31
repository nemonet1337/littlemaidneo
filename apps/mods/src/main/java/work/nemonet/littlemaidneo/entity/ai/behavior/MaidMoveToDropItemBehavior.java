package work.nemonet.littlemaidneo.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.Vec3;
import work.nemonet.littlemaidneo.config.LMNConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.compound.SoundPlayable;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.setup.ModRegistration;

import java.util.List;
import java.util.stream.Collectors;

public class MaidMoveToDropItemBehavior extends AbstractMaidBehavior {
    @Nullable
    private BlockPos dest;

    public MaidMoveToDropItemBehavior() {
        super(ImmutableMap.of(
                ModRegistration.IS_WAITING.get(), MemoryStatus.VALUE_ABSENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LittleMaidEntity entity) {
        var config = LittleMaidEntity.getConfig();
        
        if (entity.isEmergency() && !config.health.enableWorkInEmergency) {
            return false;
        }

        boolean hasOwner = TameableUtil.hasTameOwner(entity);
        if (!hasOwner) {
            if (!config.misc.canPickupItemByNoOwner) {
                return false;
            }
        }
        return canUse(entity);
    }

    private boolean canUse(LittleMaidEntity entity) {
        var config = LittleMaidEntity.getConfig();
        int freq = config.movement.pickupItemFrequency;
        if (entity.getRandom().nextFloat() > 1.0f / freq || isInventoryFull(entity)) {
            return false;
        }
        for (ItemEntity item : findAroundDropItem(entity)) {
            Path path = entity.getNavigation().createPath(item.blockPosition(), 0);
            if (path == null || !path.canReach()) {
                continue;
            }
            this.dest = item.blockPosition();
            entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(item.position(), config.movement.pickupItemSpeed, 0));
            return true;
        }
        return false;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        return dest != null && !entity.blockPosition().closerThan(dest, 1.5) && !isInventoryFull(entity);
    }

    @Override
    protected void stop(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        this.dest = null;
    }

    @Override
    protected void start(ServerLevel level, LittleMaidEntity entity, long gameTime) {
        if (entity instanceof SoundPlayable) {
            entity.play(LMSounds.FIND_TARGET_I);
        }
    }

    public boolean isInventoryFull(LittleMaidEntity entity) {
        var inv = entity.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    public List<ItemEntity> findAroundDropItem(LittleMaidEntity entity) {
        var config = LittleMaidEntity.getConfig();
        float r = config.movement.pickupItemRange;
        List<ItemEntity> rawList = entity.level().getEntitiesOfClass(ItemEntity.class,
                entity.getBoundingBox().inflate(r, r / 4f, r),
                item -> !item.hasPickUpDelay() && item.distanceToSqr(entity) < r * r);

        return TameableUtil.getTameOwner(entity)
                .map(owner -> rawList.stream()
                        .filter(item -> !isOwnerRange(item, owner))
                        .collect(Collectors.toList()))
                .orElse(rawList);
    }

    /** ご主人様の前方範囲内のアイテムかどうかを判定する */
    public boolean isOwnerRange(Entity entity, Entity owner) {
        Vec3 ownerPos = owner.position();
        Vec3 entityPos = entity.position().subtract(ownerPos);
        Vec3 ownerRot = owner.getViewVector(1F);
        double dot = entityPos.dot(ownerRot);
        double r = LMNConfig.get().movement.ownerForwardRange;
        return 0 < dot && dot < r * r;
    }
}
