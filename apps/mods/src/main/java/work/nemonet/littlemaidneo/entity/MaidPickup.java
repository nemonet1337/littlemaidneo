package work.nemonet.littlemaidneo.entity;

import com.google.common.collect.Lists;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import work.nemonet.littlemaidneo.util.LMCollidable;

/**
 * 周囲のアイテム／経験値オーブを拾う処理。
 *
 * <p>{@link LittleMaidEntity} から切り出した委譲クラス（{@code MaidResurrection} 等と同じ
 * static 委譲パターン）。{@code LittleMaidEntity} 側は
 * {@link #pickup(LittleMaidEntity)} を呼ぶだけにする。
 */
public final class MaidPickup {

    private MaidPickup() {
    }

    public static void pickup(LittleMaidEntity mob) {
        var config = LittleMaidEntity.getConfig();
        if (!config.misc.canPickupExperienceOrb && !config.misc.canPickupItem) {
            return;
        }
        if (mob.getHealth() <= 0 || mob.isSpectator()) {
            return;
        }
        var aabb = mob.getBoundingBox().inflate(1.0, 0.5, 1.0);
        // LMCollidable（ItemEntity / ExperienceOrb の Mixin）だけをセクション走査の段階で絞り込む。
        // 無条件の getEntities は毎 tick 周囲の全エンティティを収集するため、多数のメイドさんがいると重い。
        var aroundItems = mob.level().getEntities(mob, aabb,
                e -> e instanceof LMCollidable && !e.isRemoved());
        var exps = Lists.<Entity>newArrayList();
        for (Entity entity : aroundItems) {
            if (entity instanceof ExperienceOrb) {
                if (config.misc.canPickupExperienceOrb) {
                    exps.add(entity);
                }
                continue;
            }
            if (!config.misc.canPickupItem) {
                continue;
            }
            ((LMCollidable) entity).onCollision_LM(mob);
        }
        if (!exps.isEmpty()) {
            var collidable = ((LMCollidable) Util.getRandom(exps, mob.getRandom()));
            if (collidable != null) {
                collidable.onCollision_LM(mob);
            }
        }
    }
}
