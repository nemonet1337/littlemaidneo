package net.sistr.littlemaidrebirth.entity.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

public class EPEntityUtil {

    @ExpectPlatform
    public static AbstractArrow arrowCustomHook(BowItem bowItem, AbstractArrow arrow) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static ItemStack arrowCustomHook(LivingEntity user, ItemStack weapon, ItemStack arrow) {
        throw new AssertionError();
    }

}
