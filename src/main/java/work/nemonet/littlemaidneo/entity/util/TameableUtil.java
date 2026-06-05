package work.nemonet.littlemaidneo.entity.util;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import work.nemonet.littlemaidneo.config.LMRBConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.setup.ModRegistration;

public class TameableUtil {

    /**
     * テイムしたご主人を返す
     * 同じワールドに存在しない場合、emptyで返す
     */
    public static Optional<LivingEntity> getTameOwner(OwnableEntity tameable) {
        return Optional.ofNullable(tameable.getOwner());
    }

    /**
     * テイムしたご主人のUUIDをセットする
     * テイムしたことになる
     */
    public static void setTameOwnerUuid(TamableAnimal tameable, UUID id) {
        tameable.setOwnerReference(EntityReference.of(id));
    }

    /**
     * テイムしたご主人のUUIDを返す
     * 存在しない場合、emptyで返す
     */
    public static Optional<UUID> getTameOwnerUuid(OwnableEntity tameable) {
        EntityReference<LivingEntity> ref = tameable.getOwnerReference();
        return ref != null ? Optional.of(ref.getUUID()) : Optional.empty();
    }

    /**
     * テイムしたご主人が居るならtrueを返す
     * ご主人がワールドに居るかどうかは関係ない
     */
    public static boolean hasTameOwner(OwnableEntity tameable) {
        return getTameOwnerUuid(tameable).isPresent();
    }

    /**
     * 待機中であるか否かを返す
     */
    public static boolean isWait(TamableAnimal tameable) {
        return tameable.isOrderedToSit();
    }

    /**
     * 待機状態をセットする
     */
    public static void setWait(TamableAnimal tameable, boolean isWait) {
        tameable.setOrderedToSit(isWait);
        syncWaitMemory(tameable);
    }

    public static void switchWait(TamableAnimal tameable) {
        tameable.setOrderedToSit(!tameable.isOrderedToSit());
        syncWaitMemory(tameable);
    }

    /**
     * 待機状態（orderedToSit）を Brain の IS_WAITING メモリへ即時反映する。
     * <p>このメモリ同期は {@link work.nemonet.littlemaidneo.entity.ai.sensor.LittleMaidSensor}
     * が 20 tick ごとに行うため、砂糖などで待機を切り替えた直後の最大 1 秒間は
     * Behavior が反応せず「待機にならない」ように見える（特にワープ/追従中に顕著）。
     * 切り替え時にここで直接同期することで遅延を解消する。
     */
    private static void syncWaitMemory(TamableAnimal tameable) {
        if (tameable instanceof LittleMaidEntity maid && !maid.level().isClientSide()) {
            if (maid.isOrderedToSit()) {
                maid.getBrain().setMemory(ModRegistration.IS_WAITING.get(), Unit.INSTANCE);
            } else {
                maid.getBrain().eraseMemory(ModRegistration.IS_WAITING.get());
            }
        }
    }

    /**
     * ご主人が同じならtrue
     * ご主人を持っていない場合はfalse
     */
    public static boolean equalTameOwner(OwnableEntity a, OwnableEntity b) {
        var aOwner = getTameOwner(a);
        var bOwner = getTameOwner(b);
        if (aOwner.isEmpty() || bOwner.isEmpty()) {
            return false;
        }
        return aOwner.get().equals(bOwner.get());
    }

    public static boolean isTameOwner(OwnableEntity tameable, LivingEntity entity) {
        EntityReference<LivingEntity> ref = tameable.getOwnerReference();
        return ref != null && entity.getUUID().equals(ref.getUUID());
    }

    public static boolean tryTeleportToOwner(LittleMaidEntity tameable, LivingEntity owner, int widthRange, int heightRange) {
        BlockPos ownerPos = owner.blockPosition();
        var navigation = tameable.getNavigation();
        int maxTry = LMRBConfig.get().movement.maxTryTeleportCount;
        for (int i = 0; i < maxTry; ++i) {
            int x = tameable.getRandom().nextInt(widthRange * 2 + 1) - widthRange;
            int y = tameable.getRandom().nextInt(heightRange * 2 + 1) - heightRange;
            int z = tameable.getRandom().nextInt(widthRange * 2 + 1) - widthRange;
            
            int targetX = ownerPos.getX() + x;
            int targetY = ownerPos.getY() + y;
            int targetZ = ownerPos.getZ() + z;

            if (isOwnerForwardRange(owner, targetX, targetY, targetZ)) continue;
            
            BlockPos targetPos = new BlockPos(targetX, targetY, targetZ);
            if (canTeleportTo(tameable, targetPos)) {
                tameable.snapTo(targetX + 0.5, targetY, targetZ + 0.5, tameable.getYRot(), tameable.getXRot());
                navigation.stop();
                return true;
            }
        }
        return false;
    }

    private static boolean isOwnerForwardRange(LivingEntity owner, int x, int y, int z) {
        if (LMRBConfig.get().movement.canTeleportOwnerForwards) return false;
        Vec3 ownerPos = owner.position();
        Vec3 entityPos = new Vec3(x + 0.5, y, z + 0.5).subtract(ownerPos);
        Vec3 ownerRot = owner.getViewVector(1F);
        double dot = entityPos.dot(ownerRot);
        double range = LMRBConfig.get().movement.ownerForwardRange;
        return 0 < dot && dot < range * range;
    }

    private static boolean canTeleportTo(LittleMaidEntity tameable, BlockPos pos) {
        PathType pathNodeType = WalkNodeEvaluator.getPathTypeStatic(
                new net.minecraft.world.level.pathfinder.PathfindingContext(tameable.level(), tameable), pos.mutable());
        if (pathNodeType != PathType.WALKABLE) return false;
        BlockPos blockPos = pos.subtract(tameable.blockPosition());
        return tameable.level().noCollision(tameable, tameable.getBoundingBox().move(blockPos));
    }
}
