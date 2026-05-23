package net.sistr.littlemaidrebirth.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

/**
 * 手のリーチの属性に関するユーティリティ
 */
public class ReachAttributeUtil {

    @ExpectPlatform
    public static void addAttribute(AttributeSupplier.Builder attributeBuilder) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static double getAttackRangeSq(LivingEntity entity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static double getAttackRange(LivingEntity entity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static double getRangeSq(LivingEntity entity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static double getRange(LivingEntity entity) {
        throw new AssertionError();
    }

}
