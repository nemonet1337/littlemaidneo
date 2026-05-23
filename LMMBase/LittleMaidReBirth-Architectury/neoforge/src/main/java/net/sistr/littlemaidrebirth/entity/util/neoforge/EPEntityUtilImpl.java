package net.sistr.littlemaidrebirth.entity.util.neoforge;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CommonHooks;

public class EPEntityUtilImpl {
    public static AbstractArrow arrowCustomHook(BowItem bowItem, AbstractArrow projectile) {
        return projectile;
    }

    public static ItemStack arrowCustomHook(LivingEntity user, ItemStack weapon, ItemStack arrow) {
        return CommonHooks.getProjectile(user, weapon, arrow);
    }
}
