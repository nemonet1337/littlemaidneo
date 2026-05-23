package work.nemonet.littlemaidneo.entity.util;

import net.minecraft.world.entity.Entity;

/**
 * 開いているGUIのエンティティを取得するインターフェイス
 */
public interface GuiEntitySupplier<T extends Entity> {

    T getGuiEntity();

}
