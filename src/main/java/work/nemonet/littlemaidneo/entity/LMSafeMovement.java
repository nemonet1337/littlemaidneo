package work.nemonet.littlemaidneo.entity;

import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import work.nemonet.littlemaidneo.config.LMRBConfig;

/**
 * メイドさんの「安全移動」ロジックの移譲先。
 * <p>
 * 落下ダメージや危険ブロック（炎・溶岩等）でメイドさんが死なないよう、縁(ledge)での移動ベクトルを
 * 押し戻す処理を {@link LittleMaidEntity#maybeBackOffFromEdge(Vec3, MoverType)} から分離したもの。
 * 挙動は分離前と同一。{@code fallDistance} と {@code calculateFallDamage}（いずれも外部から
 * 直接参照できない）へは {@code LittleMaidEntity} のパッケージプライベートブリッジ
 * （{@code fallDistance_LM()} / {@code getDangerHeightThreshold_LM()}）経由でアクセスする。
 */
final class LMSafeMovement {

    private LMSafeMovement() {
    }

    static Vec3 maybeBackOffFromEdge(LittleMaidEntity mob, Vec3 movement, MoverType type) {
        if (type != MoverType.SELF && type != MoverType.PLAYER) {
            return movement;
        }

        LMRBConfig config = LittleMaidEntity.getConfig();

        if (!config.health.immortal &&
                !config.health.nonMobDamageImmunity &&
                config.health.enableSafeMove &&
                canClipAtLedge(mob)) {
            boolean shouldBackByDamage = isDamageSourceEmpty(mob, mob.getBoundingBox()) &&
                    !isDamageSourceEmpty(mob,
                            mob.getBoundingBox().move(movement.x, 0, movement.z));
            boolean shouldBackByFall = !config.health.fallImmunity &&
                    !isSafeFallHeight(mob,
                            mob.position().add(movement.x, 0, movement.z));

            if (shouldBackByDamage || shouldBackByFall) {
                BiPredicate<Double, Double> shouldBackPredicate = (x, z) -> false;
                if (shouldBackByDamage) {
                    BiPredicate<Double, Double> finalPredicate = shouldBackPredicate;
                    shouldBackPredicate = (x, z) -> finalPredicate.test(x, z) ||
                    // 危険物がbox内にある
                            !isDamageSourceEmpty(mob,
                                    mob.getBoundingBox().move(x, 0, z));
                }

                if (shouldBackByFall) {
                    BiPredicate<Double, Double> finalPredicate = shouldBackPredicate;
                    shouldBackPredicate = (x, z) -> finalPredicate.test(x, z) ||
                    // 足場がbox内にない
                            mob.level().noCollision(
                                    mob,
                                    mob.getBoundingBox()
                                            .move(x, 0, z)
                                            .expandTowards(
                                                    0,
                                                    -(mob.getDangerHeightThreshold_LM() -
                                                            mob.fallDistance_LM()),
                                                    0))
                            ||
                            // または、すぐ下に足場がなく、危険物がbox内にある
                            (mob.level().noCollision(
                                    mob,
                                    mob.getBoundingBox()
                                            .move(x, 0, z)
                                            .expandTowards(0, -mob.maxUpStep(), 0))
                                    &&
                                    !isDamageSourceEmpty(mob,
                                            mob.getBoundingBox()
                                                    .move(x, 0, z)
                                                    .expandTowards(
                                                            0,
                                                            -mob.getDangerHeightThreshold_LM(),
                                                            0)));
                }

                movement = pushBack(movement, shouldBackPredicate);
            }
        }

        return movement;
    }

    private static Vec3 pushBack(
            Vec3 movement,
            BiPredicate<Double, Double> pushBackPredicate) {
        double dot = 0.05;
        double mX = movement.x;
        double mZ = movement.z;
        while (mX != 0.0 && pushBackPredicate.test(mX, 0d)) {
            if (mX < dot && mX >= -dot) {
                mX = 0.0;
                continue;
            }
            if (mX > 0.0) {
                mX -= dot;
                continue;
            }
            mX += dot;
        }
        while (mZ != 0.0 && pushBackPredicate.test(0d, mZ)) {
            if (mZ < dot && mZ >= -dot) {
                mZ = 0.0;
                continue;
            }
            if (mZ > 0.0) {
                mZ -= dot;
                continue;
            }
            mZ += dot;
        }
        while (mX != 0.0 && mZ != 0.0 && pushBackPredicate.test(mX, mZ)) {
            mX = mX < dot && mX >= -dot ? 0.0 : (mX > 0.0 ? mX - dot : mX + dot);
            if (mZ < dot && mZ >= -dot) {
                mZ = 0.0;
                continue;
            }
            if (mZ > 0.0) {
                mZ -= dot;
                continue;
            }
            mZ += dot;
        }
        return new Vec3(mX, movement.y, mZ);
    }

    private static boolean isDamageSourceEmpty(LittleMaidEntity mob, AABB box) {
        int minX = Mth.floor(box.minX);
        int maxX = Mth.floor(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.floor(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.floor(box.maxZ);

        for (int x = 0; x < maxX - minX + 1; x++) {
            for (int y = 0; y < maxY - minY + 1; y++) {
                for (int z = 0; z < maxZ - minZ + 1; z++) {
                    PathType pathNodeType = WalkNodeEvaluator.getPathTypeStatic(
                            new PathfindingContext(
                                    mob.level(),
                                    mob),
                            new BlockPos(minX + x, minY + y, minZ + z).mutable());
                    if (pathNodeType == PathType.FIRE ||
                            pathNodeType == PathType.DAMAGE_CAUTIOUS ||
                            pathNodeType == PathType.LAVA) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isSafeFallHeight(LittleMaidEntity mob, Vec3 pos) {
        BlockHitResult result = mob.level().clip(
                new ClipContext(
                        pos,
                        pos.subtract(
                                0,
                                mob.getDangerHeightThreshold_LM() - mob.fallDistance_LM() + 0.1,
                                0),
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        mob));
        if (result.getType() == HitResult.Type.MISS) {
            return false;
        }
        Vec3 hitPos = result.getLocation();
        if (mob.getDangerHeightThreshold_LM() - mob.fallDistance_LM() < pos.y - hitPos.y) {
            return false;
        }
        BlockPos checkPos = new BlockPos(
                Mth.floor(pos.x),
                Mth.floor(pos.y - 1),
                Mth.floor(pos.z));
        for (int i = 0; i < pos.y - hitPos.y + 1; i++) {
            PathType pathNodeType = WalkNodeEvaluator.getPathTypeStatic(
                    new PathfindingContext(
                            mob.level(),
                            mob),
                    checkPos.mutable());
            if (pathNodeType == PathType.WALKABLE ||
                    pathNodeType == PathType.BLOCKED) {
                return true;
            }
            if (pathNodeType == PathType.FIRE ||
                    pathNodeType == PathType.DAMAGE_CAUTIOUS ||
                    pathNodeType == PathType.LAVA) {
                return false;
            }
            checkPos = checkPos.below();
        }
        return false;
    }

    private static boolean canClipAtLedge(LittleMaidEntity mob) {
        float canClipHeight = mob.getDangerHeightThreshold_LM() + 1.0f;
        // 着地しているか、落下距離が危険高度未満かつ下に足場があるとき
        return (mob.onGround() ||
                (mob.fallDistance_LM() < canClipHeight &&
                        !mob.level().noCollision(
                                mob,
                                mob.getBoundingBox().expandTowards(
                                        0.0,
                                        mob.fallDistance_LM() - canClipHeight,
                                        0.0))));
    }
}
