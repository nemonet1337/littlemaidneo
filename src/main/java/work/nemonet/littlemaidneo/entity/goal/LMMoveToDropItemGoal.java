package work.nemonet.littlemaidneo.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import work.nemonet.littlemaidneo.config.LMRBConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.compound.SoundPlayable;
import work.nemonet.littlemaidneo.resource.util.LMSounds;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * ドロップアイテムに向かうゴール。
 * <p>
 * 旧 {@code MoveToDropItemGoal}(abstract) + {@code LMMoveToDropItemGoal} を 1 クラスに統合。
 */
public class LMMoveToDropItemGoal extends Goal {

    protected final LittleMaidEntity maid;
    private final Supplier<Float> range;
    private final Supplier<Integer> frequency;
    private final Supplier<Float> speed;

    public LMMoveToDropItemGoal(LittleMaidEntity maid, Supplier<Float> range,
                                Supplier<Integer> frequency, Supplier<Float> speed) {
        this.maid = maid;
        this.range = range;
        this.frequency = frequency;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.maid.getRandom().nextFloat() > 1.0f / this.adjustedTickDelay(frequency.get())
                || isInventoryFull()) {
            return false;
        }
        Stream<BlockPos> positions = findAroundDropItem().stream().map(Entity::blockPosition);
        Path path = positions.map(pos -> maid.getNavigation().createPath(pos, 0))
                .filter(Objects::nonNull)
                .filter(Path::canReach)
                .findAny().orElse(null);
        if (path == null) return false;

        maid.getNavigation().moveTo(path, speed.get());
        return true;
    }

    @Override
    public void start() {
        super.start();
        if (maid instanceof SoundPlayable) {
            ((SoundPlayable) maid).play(LMSounds.FIND_TARGET_I);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !maid.getNavigation().isDone();
    }

    public boolean isInventoryFull() {
        var inv = this.maid.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    public List<ItemEntity> findAroundDropItem() {
        float r = this.range.get();
        return maid.level().getEntitiesOfClass(ItemEntity.class,
                maid.getBoundingBox().inflate(r, r / 4f, r),
                item -> !item.hasPickUpDelay() && item.distanceToSqr(maid) < r * r);
    }

    /** ご主人様の前方範囲内のアイテムかどうかを判定する */
    public boolean isOwnerRange(Entity entity, Entity owner) {
        Vec3 ownerPos = owner.position();
        Vec3 entityPos = entity.position().subtract(ownerPos);
        Vec3 ownerRot = owner.getViewVector(1F);
        double dot = entityPos.dot(ownerRot);
        double r = LMRBConfig.get().movement.ownerForwardRange;
        // プレイヤー位置を原点としたアイテムの位置と、プレイヤーの向きの内積がプラス
        // かつ内積の大きさが range*range 以下
        return 0 < dot && dot < r * r;
    }
}
