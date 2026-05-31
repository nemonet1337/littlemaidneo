package work.nemonet.littlemaidneo.entity.util;

import net.minecraft.world.Container;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * インベントリを持っていることを示すインターフェイス
 */
public interface HasInventory {

    /**
     * インベントリを返す
     */
    Container getInventory();

    /**
     * インベントリの状態をNBTに書き出す
     */
    void writeInventory(ValueOutput output);

    /**
     * インベントリ状態をNBTから読み込む
     */
    void readInventory(ValueInput input);

}
