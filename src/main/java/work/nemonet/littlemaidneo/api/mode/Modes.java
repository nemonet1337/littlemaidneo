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
            new Entry("fencer", Modes::buildFencerMode),
            new Entry("archer", Modes::buildArcherMode),
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

    public static ModeType.Builder<FencerMode> buildFencerMode() {
        return ModeType.<FencerMode>builder((type, maid) -> new FencerMode(type, "Fencer", maid, 1.0f))
                .addItemMatcher(stack -> stack.has(DataComponents.WEAPON), ItemMatcher.Priority.LOWER)
                .addItemMatcher(ItemMatchers.clazz(AxeItem.class), ItemMatcher.Priority.LOWER)
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.FENCER_MODE), ItemMatcher.Priority.HIGHER);
    }

    public static ModeType.Builder<ArcherMode> buildArcherMode() {
        return ModeType.<ArcherMode>builder((type, maid) -> new ArcherMode(type, "Archer", maid))
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
