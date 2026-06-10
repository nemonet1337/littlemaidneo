package work.nemonet.littlemaidneo.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import work.nemonet.littlemaidneo.item.IRangedWeapon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ProjectileWeaponItem.class)
public abstract class MixinRangedWeaponItem implements IRangedWeapon {
    @Shadow
    public abstract int getDefaultProjectileRange();

    @Override
    public float getMaxRange_LM(ItemStack stack, LivingEntity user) {
        return getDefaultProjectileRange();
    }

    @Override
    public int getInterval_LM(ItemStack stack, LivingEntity user) {
        if ((Object) this instanceof net.minecraft.world.item.CrossbowItem) {
            return net.minecraft.world.item.CrossbowItem.getChargeDuration(stack, user);
        }
        return 20;
    }
}
