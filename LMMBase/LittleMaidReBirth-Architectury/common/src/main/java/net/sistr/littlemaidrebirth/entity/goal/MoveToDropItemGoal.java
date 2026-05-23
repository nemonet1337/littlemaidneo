package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.pathfinder.Path;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

//ドロップアイテムに向かうGoal
public abstract class MoveToDropItemGoal extends Goal {
    private final PathfinderMob mob;
    private final Supplier<Float> range;
    private final Supplier<Integer> frequency;
    private final Supplier<Float> speed;

    public MoveToDropItemGoal(PathfinderMob mob, Supplier<Float> range, Supplier<Integer> frequency, Supplier<Float> speed) {
        this.mob = mob;
        this.range = range;
        this.frequency = frequency;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.mob.getRandom().nextFloat() > 1.0f / this.adjustedTickDelay(frequency.get())
                || isInventoryFull()) {
            return false;
        }
        Stream<BlockPos> positions = findAroundDropItem().stream().map(Entity::blockPosition);
        Path path = positions.map(pos -> mob.getNavigation().createPath(pos, 0))
                .filter(Objects::nonNull)
                .filter(Path::canReach)
                .findAny().orElse(null);
        if (path == null) {
            return false;
        }

        mob.getNavigation().moveTo(path, speed.get());
        return true;
    }

    @Override
    public void start() {
        super.start();
        if (mob instanceof SoundPlayable) {
            ((SoundPlayable) mob).play(LMSounds.FIND_TARGET_I);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !mob.getNavigation().isDone();
    }

    public abstract boolean isInventoryFull();

    public List<ItemEntity> findAroundDropItem() {
        float range = this.range.get();
        return mob.level().getEntitiesOfClass(ItemEntity.class,
                mob.getBoundingBox().inflate(range, range / 4f, range),
                item -> !item.hasPickUpDelay() && item.distanceToSqr(mob) < range * range);
    }
}
