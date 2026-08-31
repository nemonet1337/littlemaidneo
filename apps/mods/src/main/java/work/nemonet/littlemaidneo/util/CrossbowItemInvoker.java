package work.nemonet.littlemaidneo.util;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;

/**
 * 花火矢は 1.6、それ以外のチャージ済みクロスボウは 3.15。
 */
public final class CrossbowItemInvoker {
    private CrossbowItemInvoker() {
    }

    public static float getSpeed(ChargedProjectiles chargedProjectiles) {
        if (chargedProjectiles == null) {
            return 3.15f;
        }
        return chargedProjectiles.contains(Items.FIREWORK_ROCKET) ? 1.6f : 3.15f;
    }
}
