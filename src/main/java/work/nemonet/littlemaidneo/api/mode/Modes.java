package work.nemonet.littlemaidneo.api.mode;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ShearsItem;
import work.nemonet.littlemaidneo.entity.mode.*;
import work.nemonet.littlemaidneo.tags.LMTags;

import java.util.List;
import java.util.function.Supplier;

import static work.nemonet.littlemaidneo.LittleMaidNeo.MODID;

/**
 * デフォルトのモードを追加するクラス
 * メイド専用
 */
public class Modes {

    /**
     * モード ID と定義ビルダーの対応表。
     * <p>登録順（同 Priority 時のタイブレーク）＝この並び順。順序を変更しないこと。
     */
    private static final List<Entry> ENTRIES = List.of(
            new Entry("combat", Modes::buildCombatMode),
            new Entry("cooking", Modes::buildCookingMode),
            new Entry("ripper", Modes::buildRipperMode),
            new Entry("torcher", Modes::buildTorcherMode),
            new Entry("healer", Modes::buildHealerMode),
            new Entry("pharmcist", Modes::buildPharmcistMode));

    private record Entry(String id, Supplier<ModeType.Builder<?>> builder) {}

    public static void init() {
        for (Entry entry : ENTRIES) {
            register(entry.id(), entry.builder().get().build());
        }
    }

    /**
     * 戦闘モード（近接 Fencer + 射撃 Archer の統合・AI-4）。
     * <p>武器種に応じて内部でスタイルを切り替える単一モード。アイテムマッチャは旧 Fencer/Archer の和集合。
     */
    public static ModeType.Builder<CombatMode> buildCombatMode() {
        return ModeType.<CombatMode>builder((type, maid) -> new CombatMode(type, "Combat", maid, 1.0f))
                // 近接（旧 Fencer）
                .addItemMatcher(stack -> stack.has(DataComponents.WEAPON), ItemMatcher.Priority.LOWER)
                .addItemMatcher(ItemMatchers.clazz(AxeItem.class), ItemMatcher.Priority.LOWER)
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.FENCER_MODE), ItemMatcher.Priority.HIGHER)
                // 射撃（旧 Archer）
                .addItemMatcher(ItemMatchers.clazz(IRangedWeapon.class), ItemMatcher.Priority.LOWER)
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.ARCHER_MODE), ItemMatcher.Priority.HIGHER);
    }

    public static ModeType.Builder<CookingMode> buildCookingMode() {
        return ModeType.<CookingMode>builder((type, maid) -> new CookingMode(type, "Cooking", maid))
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.COOKING_MODE), ItemMatcher.Priority.HIGHER);
    }

    public static ModeType.Builder<RipperMode> buildRipperMode() {
        return ModeType.<RipperMode>builder((type, maid) -> new RipperMode(type, "Ripper", maid, 8F))
                .addItemMatcher(ItemMatchers.clazz(ShearsItem.class), ItemMatcher.Priority.LOWER)
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.RIPPER_MODE), ItemMatcher.Priority.HIGHER);
    }

    public static ModeType.Builder<TorcherMode> buildTorcherMode() {
        return ModeType.<TorcherMode>builder((type, maid) -> new TorcherMode(type, "Torcher", maid, 12F))
                .addItemMatcher(stack -> stack.getItem() instanceof BlockItem
                        && 9 < ((BlockItem) stack.getItem()).getBlock().defaultBlockState().getLightEmission(),
                        ItemMatcher.Priority.LOWER)
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.TORCHER_MODE), ItemMatcher.Priority.HIGHER);
    }

    public static ModeType.Builder<PharmcistMode> buildPharmcistMode() {
        return ModeType.<PharmcistMode>builder((type, maid) -> new PharmcistMode(type, "Pharmcist", maid))
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.PHARMCIST_MODE), ItemMatcher.Priority.HIGHER);
    }

    public static ModeType.Builder<HealerMode> buildHealerMode() {
        return ModeType.<HealerMode>builder((type, maid) -> new HealerMode(type, "Healer", maid))
                .addItemMatcher(stack -> stack.get(DataComponents.FOOD) != null, ItemMatcher.Priority.LOWER)
                .addItemMatcher(stack -> {
                    var contents = stack.get(DataComponents.POTION_CONTENTS);
                    return contents != null && contents.potion().isPresent();
                }, ItemMatcher.Priority.LOWER)
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.HEALER_MODE), ItemMatcher.Priority.HIGHER);
    }

    private static void register(String id, ModeType<?> modeType) {
        ModeManager.INSTANCE.register(Identifier.fromNamespaceAndPath(MODID, id), modeType);
    }

}
