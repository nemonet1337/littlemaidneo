package net.sistr.littlemaidrebirth.entity.util;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;

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
        tameable.setOwnerUUID(id);
    }

    /**
     * テイムしたご主人のUUIDを返す
     * 存在しない場合、emptyで返す
     */
    public static Optional<UUID> getTameOwnerUuid(OwnableEntity tameable) {
        return Optional.ofNullable(tameable.getOwnerUUID());
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
    }

    public static void switchWait(TamableAnimal tameable) {
        tameable.setOrderedToSit(!tameable.isOrderedToSit());
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
        return entity.getUUID().equals(tameable.getOwnerUUID());
    }

}
