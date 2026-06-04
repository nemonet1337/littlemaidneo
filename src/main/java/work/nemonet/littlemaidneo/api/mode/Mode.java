package work.nemonet.littlemaidneo.api.mode;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
     * アクティブなスタイルに応じて従来のジョブ名を返すためにオーバーライドする。
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

}
