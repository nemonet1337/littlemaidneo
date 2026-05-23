package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.sistr.littlemaidrebirth.api.mode.IRangedWeapon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ProjectileWeaponItem.class)
public abstract class MixinRangedWeaponItem implements IRangedWeapon {
    @Shadow
    public abstract int getDefaultProjectileRange();

    @Override
    public float getMaxRange_LMRB(ItemStack stack, LivingEntity user) {
        return getDefaultProjectileRange();
    }

    @Override
    public int getInterval_LMRB(ItemStack stack, LivingEntity user) {
        return 20;
    }
}
