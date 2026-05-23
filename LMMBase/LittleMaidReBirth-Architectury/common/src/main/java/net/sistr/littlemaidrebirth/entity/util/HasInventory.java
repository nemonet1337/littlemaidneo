package net.sistr.littlemaidrebirth.entity.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;

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
    void writeInventory(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries);

    /**
     * インベントリ状態をNBTから読み込む
     */
    void readInventory(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries);

}
