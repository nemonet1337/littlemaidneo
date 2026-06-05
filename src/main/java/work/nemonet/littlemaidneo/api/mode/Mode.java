package work.nemonet.littlemaidneo.api.mode;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import work.nemonet.littlemaidneo.entity.mode.*;
import work.nemonet.littlemaidneo.tags.LMTags;

import java.util.List;
import java.util.function.Supplier;

import static work.nemonet.littlemaidneo.LittleMaidNeo.MODID;

/**
 * MobのAIモードのクラス
 */
public abstract class Mode {
    private final ModeType<? extends Mode> modeType;
    private final String name;

    protected Mode(ModeType<? extends Mode> modeType, String name) {
        this.modeType = modeType;
        this.name = name;
    }

    /**
     * モード開始時(切り替わった時)に一度だけ処理
     */
    public void startModeTask() {

    }

    /**
     * 処理を開始すべきか
     */
    abstract public boolean shouldExecute();

    /**
     * 処理を続行すべきか
     */
    abstract public boolean shouldContinueExecuting();

    /**
     * 処理開始時に一回だけ処理
     */
    public void startExecuting() {

    }

    /**
     * 毎tick処理
     */
    public void tick() {

    }

    /**
     * 処理終了時に一回だけ処理
     */
    public void resetTask() {

    }

    /**
     * モード終了時(切り替わった時)に一回だけ処理
     */
    public void endModeTask() {

    }

    /**
     * ワールド保存時に処理
     */
    public void writeModeData(ValueOutput output) {

    }

    /**
     * ワールド読み込み時に処理
     */
    public void readModeData(ValueInput input) {

    }

    /**
     * モード名表示用
     */
    public final String getName() {
        return name;
    }

    /**
     * モードタイプ取得
     */
    public final ModeType<? extends Mode> getModeType() {
        return modeType;
    }

    /**
     * 外部モデルパックが参照する描画 caps（{@code caps_job}）用のジョブ名。
     * 既定はモード名の小文字。統合モード（例: {@link work.nemonet.littlemaidneo.entity.mode.CombatMode}）は
     * アクティブなスタイルに応じて従来のジョブ名をお返すためにオーバーライドする。
     */
    public String getJobName() {
        return getName().toLowerCase();
    }

    public boolean isBattleMode() {
        return false;
    }

    public BattleModeType getBattleModeType() {
        return isBattleMode() ? BattleModeType.SWORD : BattleModeType.NONE;
    }

    public enum BattleModeType {
        NONE,
        SWORD,
        BOW
    }

    // =================================================================
    // 旧 Modes (登録ヘルパー) の統合
    // =================================================================

    private static final List<Entry> ENTRIES = List.of(
            new Entry("combat", Mode::buildCombatMode),
            new Entry("cooking", Mode::buildCookingMode),
            new Entry("ripper", Mode::buildRipperMode),
            new Entry("torcher", Mode::buildTorcherMode),
            new Entry("healer", Mode::buildHealerMode),
            new Entry("pharmcist", Mode::buildPharmcistMode));

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
