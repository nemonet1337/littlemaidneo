package work.nemonet.littlemaidneo.data;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import work.nemonet.littlemaidneo.LittleMaidNeo;

public class LMLanguageProvider extends LanguageProvider {
    private final String locale;

    public LMLanguageProvider(PackOutput output, String locale) {
        super(output, LittleMaidNeo.MODID, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        if ("en_us".equals(locale)) {
            add("itemGroup.littlemaidneo", "LittleMaidNeo");

            add("entity.littlemaidneo.multi_model", "Little Maid");
            add("entity.littlemaidneo.little_maid_mob", "LittleMaid");
            add("entity.littlemaidneo.little_maid_mob.InsideSkirt", "InsideSkirt");
            add("entity.littlemaidneo.maid_soul", "Maid Soul");

            add("state.littlemaidneo.None", "None");
            add("state.littlemaidneo.Wait", "Wait");
            add("state.littlemaidneo.Escort", "Escort");
            add("state.littlemaidneo.Freedom", "Freedom");
            add("state.littlemaidneo.Tracer", "Tracer");
            add("state.littlemaidneo.Strike", "Strike");
            add("mode.littlemaidneo.Combat", "Combat");
            add("mode.littlemaidneo.Cooking", "Cooking");
            add("mode.littlemaidneo.Pharmcist", "Pharmacist");
            add("mode.littlemaidneo.Ripper", "Ripper");
            add("mode.littlemaidneo.Torcher", "Torcher");
            add("mode.littlemaidneo.Healer", "Healer");

            add("block.littlemaidneo.salary_box", "LittleMaid Salary Box");

            add("item.littlemaidneo.little_maid_spawn_egg", "LittleMaid Spawn Egg");

            add("advancements.husbandry.contract_maid.title", "Realization");
            add("advancements.husbandry.contract_maid.description", "Employ a LittleMaid");
            add("advancements.husbandry.resurrect_maid.title", "LittleMaid Never Dies");
            add("advancements.husbandry.resurrect_maid.description", "Lighting a candle cake surrounded by sugarcane");

            add("key.categories.littlemaidneo", "LittleMaid Neo");
            add("key.littlemaidneo.open_maid_screen", "Open Maid Screen");
            add("key.littlemaidneo.model_select", "Model Select");
            add("key.littlemaidneo.sound_pack_select", "Sound Pack Select");
            add("key.littlemaidneo.open_maid_manager_screen", "Open Maid Manager Screen");

            add("screen.littlemaidneo.model_select", "Model Select");
            add("screen.littlemaidneo.sound_pack_select", "Sound Pack Select");

            add("gui.littlemaidneo.littlemaid.tooltip.open_target_tag_setting", "Target Tag Setting");
            add("gui.littlemaidneo.littlemaid.tooltip.open_sound_pack_select", "Sound Pack Selection");
            add("gui.littlemaidneo.littlemaid.tooltip.open_model_select", "Model Selection");
            add("gui.littlemaidneo.littlemaid.tooltip.change_moving_mode", "Moving mode change");
            add("gui.littlemaidneo.littlemaid.tooltip.change_blood_suck", "Change target of combat");
            add("gui.littlemaidneo.littlemaid.tooltip.change_blood_suck.to_blood_suck", " - Currently the maid is calm and only hunts ENEMY mobs");
            add("gui.littlemaidneo.littlemaid.tooltip.change_blood_suck.is_blood_suck", " - Currently the maid is bloodthirsty and even hunt UNKNOWN mobs");
            add("gui.littlemaidneo.littlemaid.tooltip.open_maid_manager", "Maid Manager");
            add("gui.littlemaidneo.littlemaid.tooltip.setting_work_item_slot", "Work item slot setting - the maid does not store \"items in this slot\" in the chest.");
            add("gui.littlemaidneo.littlemaid.tooltip.strike_warning", "On strike! Give sugar (salary) to resume work.");

            add("gui.littlemaidneo.target_tag.tags.attack_prohibited", "Attack Prohibited");
            add("gui.littlemaidneo.target_tag.tags.preemptive_attack_prohibited", "Preemptive Attack Prohibited");
            add("gui.littlemaidneo.target_tag.tags.preemptive_attack_allowed", "Preemptive Attack Allowed");
            add("gui.littlemaidneo.target_tag.tags.no_weapon_restriction", "No Weapon Restriction");
            add("gui.littlemaidneo.target_tag.tags.melee_weapon_prohibited", "Melee Weapon Prohibited");
            add("gui.littlemaidneo.target_tag.tags.ranged_weapon_prohibited", "Ranged Weapon Prohibited");
            add("gui.littlemaidneo.target_tag.tags.approach_allowed", "Approach Allowed");
            add("gui.littlemaidneo.target_tag.tags.approach_prohibited", "Approach Prohibited");

            add("gui.littlemaidneo.maidmanager.open_inventory", "Inventory");

            add("container.littlemaidneo.salary_box", "Maid Salary Box");

            add("screen.littlemaidmodelloader.model_select_screen.change_screen", "Model / Armor");

            add("littlemaidneo.configuration.title", "LittleMaidNeo Configs");
            add("littlemaidneo.configuration.section.littlemaidneo.common.toml", "LittleMaidNeo Configs");
            add("littlemaidneo.configuration.section.littlemaidneo.common.toml.title", "LittleMaidNeo Configs");
            add("chat.littlemaidneo.maid_died", "%1$s died due to %2$s");
            add("chat.littlemaidneo.owner_name_prefix", "Owner");
            add("chat.littlemaidneo.book_parameters_applied", "Parameters applied from the book.");
            add("chat.littlemaidneo.need_more_sugar_for_strike", "Need 8 or more sugar to resume from strike!");

            add("commands.littlemaidneo.reload.start", "Reloading LittleMaidNeo resources...");
            add("commands.littlemaidneo.reload.success", "Reloaded LittleMaidNeo resources successfully.");
            add("commands.littlemaidneo.reload.failure", "Failed to reload LittleMaidNeo resources: %s");
            add("commands.littlemaidneo.models.list.count", "Total loaded models: %s");
            add("commands.littlemaidneo.executor.null", "This command must be run by an entity.");
            add("commands.littlemaidneo.maid.count.nearby", "Nearby maids (within %2$s blocks): %1$s");
            add("commands.littlemaidneo.executor.not_player", "Only players can run this subcommand.");
            add("commands.littlemaidneo.maid.tp.success", "Teleported %s maids to your location.");
            add("commands.littlemaidneo.maid.dismiss.success", "Dismissed %s maids nearby.");
        } else if ("ja_jp".equals(locale)) {
            add("itemGroup.littlemaidneo", "リトルメイドネオ");

            add("entity.littlemaidneo.multi_model", "リトルメイド");
            add("entity.littlemaidneo.little_maid_mob", "リトルメイド");
            add("entity.littlemaidneo.little_maid_mob.InsideSkirt", "InsideSkirt");
            add("entity.littlemaidneo.maid_soul", "メイドソウル");

            add("state.littlemaidneo.None", "からっぽ");
            add("state.littlemaidneo.Wait", "待機");
            add("state.littlemaidneo.Escort", "従者");
            add("state.littlemaidneo.Freedom", "自由人");
            add("state.littlemaidneo.Tracer", "赤石探知");
            add("state.littlemaidneo.Strike", "ストライキ");
            add("mode.littlemaidneo.Combat", "戦闘係");
            add("mode.littlemaidneo.Cooking", "お料理係");
            add("mode.littlemaidneo.Pharmcist", "調合係");
            add("mode.littlemaidneo.Ripper", "毛狩り隊");
            add("mode.littlemaidneo.Torcher", "照明係");
            add("mode.littlemaidneo.Healer", "回復係");

            add("block.littlemaidneo.salary_box", "メイドさんのお給料箱");

            add("item.littlemaidneo.little_maid_spawn_egg", "リトルメイドのスポーンエッグ");

            add("advancements.husbandry.contract_maid.title", "悟り。");
            add("advancements.husbandry.contract_maid.description", "リトルメイドを雇用する");
            add("advancements.husbandry.resurrect_maid.title", "不滅のメイドソウル");
            add("advancements.husbandry.resurrect_maid.description", "サトウキビに囲まれたロウソク付きケーキに火を灯す");

            add("key.categories.littlemaidneo", "リトルメイドネオ");
            add("key.littlemaidneo.open_maid_screen", "メイド画面を開く");
            add("key.littlemaidneo.model_select", "モデル選択");
            add("key.littlemaidneo.sound_pack_select", "サウンドパック選択");
            add("key.littlemaidneo.open_maid_manager_screen", "メイドさん管理画面を開く");

            add("screen.littlemaidneo.model_select", "モデル選択");
            add("screen.littlemaidneo.sound_pack_select", "サウンドパック選択");

            add("gui.littlemaidneo.littlemaid.tooltip.open_target_tag_setting", "敵対対象タグ設定");
            add("gui.littlemaidneo.littlemaid.tooltip.open_sound_pack_select", "サウンドパック選択");
            add("gui.littlemaidneo.littlemaid.tooltip.open_model_select", "モデル選択");
            add("gui.littlemaidneo.littlemaid.tooltip.change_moving_mode", "移動モード変更");
            add("gui.littlemaidneo.littlemaid.tooltip.change_blood_suck", "戦闘対象変更");
            add("gui.littlemaidneo.littlemaid.tooltip.change_blood_suck.to_blood_suck", "―現在メイドさんは冷静で、ENEMYのモブだけ狩る");
            add("gui.littlemaidneo.littlemaid.tooltip.change_blood_suck.is_blood_suck", "―現在メイドさんは血に飢え、UNKNOWNのモブまで狩る");
            add("gui.littlemaidneo.littlemaid.tooltip.open_maid_manager", "メイドさん管理画面");
            add("gui.littlemaidneo.littlemaid.tooltip.setting_work_item_slot", "お仕事アイテムスロット設定―メイドさんは\"このスロットのアイテム\"をチェストに仕舞わない");
            add("gui.littlemaidneo.littlemaid.tooltip.strike_warning", "ストライキ中！砂糖（お給料）をあげてください。");

            add("gui.littlemaidneo.target_tag.tags.attack_prohibited", "攻撃禁止");
            add("gui.littlemaidneo.target_tag.tags.preemptive_attack_prohibited", "先制攻撃禁止");
            add("gui.littlemaidneo.target_tag.tags.preemptive_attack_allowed", "先制攻撃許可");
            add("gui.littlemaidneo.target_tag.tags.no_weapon_restriction", "武器制限なし");
            add("gui.littlemaidneo.target_tag.tags.melee_weapon_prohibited", "近距離武器禁止");
            add("gui.littlemaidneo.target_tag.tags.ranged_weapon_prohibited", "遠距離武器禁止");
            add("gui.littlemaidneo.target_tag.tags.approach_allowed", "接近許可");
            add("gui.littlemaidneo.target_tag.tags.approach_prohibited", "接近禁止");

            add("gui.littlemaidneo.maidmanager.open_inventory", "インベントリ");

            add("container.littlemaidneo.salary_box", "メイドさんのお給料箱");

            add("screen.littlemaidmodelloader.model_select_screen.change_screen", "Model / Armor");

            add("littlemaidneo.configuration.title", "リトルメイドネオ設定");
            add("littlemaidneo.configuration.section.littlemaidneo.common.toml", "リトルメイドネオ設定");
            add("littlemaidneo.configuration.section.littlemaidneo.common.toml.title", "リトルメイドネオ設定");
            add("chat.littlemaidneo.maid_died", "%1$s は %2$s によって死亡しました");
            add("chat.littlemaidneo.owner_name_prefix", "ご主人");
            add("chat.littlemaidneo.book_parameters_applied", "本からパラメータを読み込み、適用しました。");
            add("chat.littlemaidneo.need_more_sugar_for_strike", "ストライキを解除するには砂糖が8個以上必要です！");

            add("commands.littlemaidneo.reload.start", "リトルメイドネオのリソースを再読み込み中...");
            add("commands.littlemaidneo.reload.success", "リトルメイドネオのリソースを再読み込みしました。");
            add("commands.littlemaidneo.reload.failure", "リソースの再読み込みに失敗しました: %s");
            add("commands.littlemaidneo.models.list.count", "読み込み済みモデルの総数: %s");
            add("commands.littlemaidneo.executor.null", "このコマンドはエンティティから実行する必要があります。");
            add("commands.littlemaidneo.maid.count.nearby", "付近（%2$sブロック以内）のメイドさんの数: %1$s");
            add("commands.littlemaidneo.executor.not_player", "このサブコマンドはプレイヤーのみ実行可能です。");
            add("commands.littlemaidneo.maid.tp.success", "%s体のメイドをご主人の元へテレポートしました。");
            add("commands.littlemaidneo.maid.dismiss.success", "%s体のメイドの契約を解除しました。");
        }
    }
}
