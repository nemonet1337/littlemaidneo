package work.nemonet.littlemaidneo.entity.ai.behavior;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.RecipePropertySet;

/**
 * 醸造（メイドさん薬師ジョブ）の共有判定。バニラ醸造台と同じスロット構成・データ駆動判定を使う。
 */
final class BrewingSupport {

    /** 瓶スロット（0-2）。 */
    static final int BOTTLE_SLOTS = 3;
    /** 試薬スロット。 */
    static final int SLOT_REAGENT = 3;
    /** 燃料スロット。 */
    static final int SLOT_FUEL = 4;

    private BrewingSupport() {
    }

    /** 醸造台の瓶スロットに置ける「ベース」か（バニラの {@code BREWING_INPUTS} 判定）。 */
    static boolean isBrewableBottle(ServerLevel level, ItemStack stack) {
        return level.recipeAccess().propertySet(RecipePropertySet.BREWING_INPUTS).test(stack);
    }

    /** 醸造燃料か（データ駆動）。 */
    static boolean isFuel(ItemStack stack) {
        return stack.has(DataComponents.BREWING_FUEL);
    }

    /** ポーション系アイテム（瓶容器）か。 */
    static boolean isPotionContainer(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }

    /** 醸造済みの完成ポーションか（ベースではない）。 */
    static boolean isFinishedPotion(ServerLevel level, ItemStack stack) {
        return isPotionContainer(stack) && !isBrewableBottle(level, stack);
    }

    /** 空き瓶を水入り瓶にしたスタックを作る。 */
    static ItemStack createWaterBottle() {
        var stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
        return stack;
    }
}
