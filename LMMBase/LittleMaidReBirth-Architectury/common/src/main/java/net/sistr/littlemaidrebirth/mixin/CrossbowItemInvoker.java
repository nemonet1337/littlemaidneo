package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;

/**
 * MC 1.21.1 では CrossbowItem.getSpeed は private static であり @Invoker でアクセスできないため、
 * 同等のロジックをインラインで実装する。
 * FireworkRocketItem が含まれる場合は 1.6f、それ以外は 3.15f を返す。
 */
public class CrossbowItemInvoker {

    public static float getSpeed(ChargedProjectiles chargedProjectiles) {
        if (chargedProjectiles == null)
            return 3.15f;
        return chargedProjectiles.contains(Items.FIREWORK_ROCKET) ? 1.6f : 3.15f;
    }

}
