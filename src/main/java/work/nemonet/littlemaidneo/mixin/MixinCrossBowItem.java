package work.nemonet.littlemaidneo.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CrossbowItem.class)
public abstract class MixinCrossBowItem extends MixinRangedWeaponItem {

    @Override
    public int getInterval_LMRB(ItemStack stack, LivingEntity user) {
        return CrossbowItem.getChargeDuration(stack, user);
    }
}
