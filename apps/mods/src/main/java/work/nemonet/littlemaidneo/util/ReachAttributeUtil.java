package work.nemonet.littlemaidneo.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class ReachAttributeUtil {
    private ReachAttributeUtil() {
    }

    public static void addAttribute(AttributeSupplier.Builder attributeBuilder) {
        attributeBuilder.add(Attributes.ENTITY_INTERACTION_RANGE);
    }

    public static double getAttackRangeSq(LivingEntity entity) {
        double reach = entity.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        return reach * reach;
    }
}
