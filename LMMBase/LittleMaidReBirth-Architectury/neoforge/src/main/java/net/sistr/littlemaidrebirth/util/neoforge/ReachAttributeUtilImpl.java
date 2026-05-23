package net.sistr.littlemaidrebirth.util.neoforge;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class ReachAttributeUtilImpl {

    public static void addAttribute(AttributeSupplier.Builder attributeBuilder) {
        attributeBuilder.add(Attributes.ENTITY_INTERACTION_RANGE);
    }

    public static double getAttackRangeSq(LivingEntity entity) {
        double reach = getAttackRange(entity);
        return reach * reach;
    }

    public static double getAttackRange(LivingEntity entity) {
        return entity.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
    }

    public static double getRangeSq(LivingEntity entity) {
        double reach = getRange(entity);
        return reach * reach;
    }

    public static double getRange(LivingEntity entity) {
        return entity.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
    }

}
