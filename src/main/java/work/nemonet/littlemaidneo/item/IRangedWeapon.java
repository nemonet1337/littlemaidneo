package work.nemonet.littlemaidneo.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 射撃武器のパラメーターを返すインターフェイス
 * 主にMob의 AIに使用する
 */
public interface IRangedWeapon {

    /**
     * 射程範囲を返すメソッド
     * AIの射程判定に使用する
     */
    float getMaxRange_LM(ItemStack stack, LivingEntity user);

    /**
     * 射撃間隔を返すメソッド
     * AIの射撃間隔に使用する
     */
    int getInterval_LM(ItemStack stack, LivingEntity user);

}
