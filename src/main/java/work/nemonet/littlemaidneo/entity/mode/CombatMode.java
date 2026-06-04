package work.nemonet.littlemaidneo.entity.mode;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import work.nemonet.littlemaidneo.api.mode.IRangedWeapon;
import work.nemonet.littlemaidneo.api.mode.Mode;
import work.nemonet.littlemaidneo.api.mode.ModeType;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.tags.LMTags;

/**
 * 戦闘モードを統合した単一モード（AI-4）。
 *
 * <p>旧 {@code Fencer}（近接）と {@code Archer}（射撃）の 2 つの登録モードを 1 つに統合し、
 * メインハンドの武器種に応じて近接／射撃のスタイルを動的に切り替える。実装ロジックは
 * 既存の {@link FencerMode} / {@link ArcherMode} を内部ストラテジとして再利用し、挙動を温存する。
 *
 * <p>外部モデルパックが参照する描画 caps（{@code caps_job} / {@code caps_aimedBow}）の互換のため、
 * {@link #getJobName()} は従来どおり {@code "fencer"} / {@code "archer"} を返し、
 * {@link #getBattleModeType()} はアクティブなスタイルの種別（SWORD/BOW）を返す。
 *
 * <p>注: 毛刈り（Ripper）は敵対ターゲットと戦う「戦闘」ではなく受動的な刈り取り作業のため、
 * 本統合には含めず独立モードとして残している。
 */
public class CombatMode extends Mode {

    private final LittleMaidEntity mob;
    private final FencerMode melee;
    private final ArcherMode ranged;
    @Nullable
    private AbstractBattleMode<?> active;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public CombatMode(ModeType<? extends Mode> modeType, String name, LittleMaidEntity mob, float meleeSpeed) {
        super(modeType, name);
        this.mob = mob;
        // サブモードの ModeType は外部から参照されない（登録される ModeType は CombatMode のもの）。
        this.melee = new FencerMode((ModeType) modeType, name, mob, meleeSpeed);
        this.ranged = new ArcherMode((ModeType) modeType, name, mob);
    }

    /**
     * メインハンドの武器種から使用すべき戦闘スタイルを選ぶ。
     * 近接武器でもあり射撃武器でもある場合は近接を優先（旧登録順 fencer&lt;archer のタイブレーク互換）。
     */
    private AbstractBattleMode<?> selectStyle() {
        ItemStack main = mob.getMainHandItem();
        Item item = main.getItem();
        boolean melee = main.has(DataComponents.WEAPON)
                || item instanceof AxeItem
                || main.is(LMTags.Items.FENCER_MODE);
        if (melee) {
            return this.melee;
        }
        boolean ranged = item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof IRangedWeapon
                || main.is(LMTags.Items.ARCHER_MODE);
        return ranged ? this.ranged : this.melee;
    }

    @Override
    public boolean shouldExecute() {
        AbstractBattleMode<?> style = selectStyle();
        boolean ok = style.shouldExecute();
        this.active = ok ? style : null;
        return ok;
    }

    @Override
    public boolean shouldContinueExecuting() {
        AbstractBattleMode<?> a = this.active;
        if (a == null) {
            return false;
        }
        // 武器を持ち替えてスタイルが変わったら一旦終了させ、再選択させる。
        if (selectStyle() != a) {
            return false;
        }
        return a.shouldContinueExecuting();
    }

    @Override
    public void startExecuting() {
        AbstractBattleMode<?> a = this.active;
        if (a != null) {
            a.startExecuting();
        }
    }

    @Override
    public void tick() {
        AbstractBattleMode<?> a = this.active;
        if (a != null) {
            a.tick();
        }
    }

    @Override
    public void resetTask() {
        AbstractBattleMode<?> a = this.active;
        if (a != null) {
            a.resetTask();
            this.active = null;
        }
    }

    @Override
    public boolean isBattleMode() {
        return true;
    }

    @Override
    public BattleModeType getBattleModeType() {
        AbstractBattleMode<?> a = this.active;
        // 非実行時（ターゲット未取得など）でも武器種から推定して返す。
        return (a != null ? a : selectStyle()).getBattleModeType();
    }

    /**
     * 描画 caps（{@code caps_job}）用のジョブ名。外部モデルパック互換のため
     * 従来の {@code "fencer"} / {@code "archer"} を返す。
     */
    @Override
    public String getJobName() {
        return selectStyle() == this.ranged ? "archer" : "fencer";
    }
}
