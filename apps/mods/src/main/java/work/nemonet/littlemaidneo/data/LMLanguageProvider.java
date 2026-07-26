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

    private void addCombinedStateEn(String moving, String job, String text) {
        add("state.littlemaidneo." + moving + "_" + job, text);
    }

    private void addCombinedStateJa(String moving, String job, String text) {
        add("state.littlemaidneo." + moving + "_" + job, text);
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
            add("state.littlemaidneo.Stroll", "Stroll");
            add("state.littlemaidneo.Tracer", "Tracer");
            add("state.littlemaidneo.Strike", "Strike");
            add("chat.littlemaidneo.main_hand.left", "%s is now left-handed.");
            add("chat.littlemaidneo.main_hand.right", "%s is now right-handed.");
            add("commands.littlemaidneo.config.bake.success", "LittleMaidNeo config rebaked (server values applied).");
            add("commands.littlemaidneo.config.bake.failure", "Config bake failed: %s");
            add("mode.littlemaidneo.Combat", "Combat");
            add("mode.littlemaidneo.Fencer", "Fencer");
            add("mode.littlemaidneo.Archer", "Archer");
            add("mode.littlemaidneo.Cooking", "Cooking");
            add("mode.littlemaidneo.Pharmcist", "Pharmacist");
            add("mode.littlemaidneo.Ripper", "Ripper");
            add("mode.littlemaidneo.Torcher", "Torcher");
            add("mode.littlemaidneo.Healer", "Healer");
            // Combined movement × job display names
            addCombinedStateEn("Escort", "Fencer", "Escort Fencer");
            addCombinedStateEn("Freedom", "Fencer", "Free Fencer");
            addCombinedStateEn("Tracer", "Fencer", "Tracer Fencer");
            addCombinedStateEn("Escort", "Archer", "Escort Archer");
            addCombinedStateEn("Freedom", "Archer", "Free Archer");
            addCombinedStateEn("Tracer", "Archer", "Tracer Archer");
            addCombinedStateEn("Escort", "Cooking", "Escort Cook");
            addCombinedStateEn("Freedom", "Cooking", "Free Cook");
            addCombinedStateEn("Tracer", "Cooking", "Tracer Cook");
            addCombinedStateEn("Escort", "Pharmcist", "Escort Pharmacist");
            addCombinedStateEn("Freedom", "Pharmcist", "Free Pharmacist");
            addCombinedStateEn("Tracer", "Pharmcist", "Tracer Pharmacist");
            addCombinedStateEn("Escort", "Ripper", "Escort Ripper");
            addCombinedStateEn("Freedom", "Ripper", "Free Ripper");
            addCombinedStateEn("Tracer", "Ripper", "Tracer Ripper");
            addCombinedStateEn("Escort", "Torcher", "Escort Torcher");
            addCombinedStateEn("Freedom", "Torcher", "Free Torcher");
            addCombinedStateEn("Tracer", "Torcher", "Tracer Torcher");
            addCombinedStateEn("Escort", "Healer", "Escort Healer");
            addCombinedStateEn("Freedom", "Healer", "Free Healer");
            addCombinedStateEn("Tracer", "Healer", "Tracer Healer");
            addCombinedStateEn("Stroll", "Fencer", "Stroll Fencer");
            addCombinedStateEn("Stroll", "Archer", "Stroll Archer");
            addCombinedStateEn("Stroll", "Cooking", "Stroll Cook");
            addCombinedStateEn("Stroll", "Pharmcist", "Stroll Pharmacist");
            addCombinedStateEn("Stroll", "Ripper", "Stroll Ripper");
            addCombinedStateEn("Stroll", "Torcher", "Stroll Torcher");
            addCombinedStateEn("Stroll", "Healer", "Stroll Healer");

            add("gui.littlemaidneo.littlemaid.salary.ok", "Salary: OK");
            add("gui.littlemaidneo.littlemaid.salary.unpaid", "Unpaid days: %s");
            add("gui.littlemaidneo.littlemaid.salary.strike", "On strike!");
            add("gui.littlemaidneo.target_tag.reset_all", "Reset IFF");
            add("gui.littlemaidneo.target_tag.reset_all.tooltip", "Clear all target tags back to defaults");

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

            add("gui.littlemaidneo.target_tag.tags.attack_prohibited", "Do Not Attack (Peaceful)");
            add("gui.littlemaidneo.target_tag.tags.preemptive_attack_prohibited", "Counterattack Only (Passive)");
            add("gui.littlemaidneo.target_tag.tags.preemptive_attack_allowed", "Attack Preemptively (Active)");
            add("gui.littlemaidneo.target_tag.tags.no_weapon_restriction", "Use All Weapons");
            add("gui.littlemaidneo.target_tag.tags.melee_weapon_prohibited", "Do Not Use Melee Weapons");
            add("gui.littlemaidneo.target_tag.tags.ranged_weapon_prohibited", "Do Not Use Ranged Weapons");
            add("gui.littlemaidneo.target_tag.tags.approach_allowed", "Approach");
            add("gui.littlemaidneo.target_tag.tags.approach_prohibited", "Do Not Approach");
            add("gui.littlemaidneo.model_select.toggle_mode", "Toggle Select Mode (Maid / Armor)");
            add("gui.littlemaidneo.model_select.toggle_contract", "Toggle Contract Status (Contract / Wild)");

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
            add("state.littlemaidneo.Stroll", "お散歩");
            add("state.littlemaidneo.Tracer", "赤石探知");
            add("state.littlemaidneo.Strike", "ストライキ");
            add("chat.littlemaidneo.main_hand.left", "%s は左利きになりました。");
            add("chat.littlemaidneo.main_hand.right", "%s は右利きになりました。");
            add("commands.littlemaidneo.config.bake.success", "リトルメイドネオのコンフィグを再適用しました。");
            add("commands.littlemaidneo.config.bake.failure", "コンフィグの再適用に失敗: %s");
            add("mode.littlemaidneo.Combat", "戦闘係");
            add("mode.littlemaidneo.Fencer", "剣士");
            add("mode.littlemaidneo.Archer", "弓兵");
            add("mode.littlemaidneo.Cooking", "お料理係");
            add("mode.littlemaidneo.Pharmcist", "調合係");
            add("mode.littlemaidneo.Ripper", "毛狩り隊");
            add("mode.littlemaidneo.Torcher", "照明係");
            add("mode.littlemaidneo.Healer", "回復係");
            // 移動×お仕事の組み合わせ表示名（旧版の護衛剣士など）
            addCombinedStateJa("Escort", "Fencer", "護衛剣士");
            addCombinedStateJa("Freedom", "Fencer", "自由剣士");
            addCombinedStateJa("Tracer", "Fencer", "探知剣士");
            addCombinedStateJa("Escort", "Archer", "護衛弓兵");
            addCombinedStateJa("Freedom", "Archer", "自由弓兵");
            addCombinedStateJa("Tracer", "Archer", "探知弓兵");
            addCombinedStateJa("Escort", "Cooking", "護衛お料理係");
            addCombinedStateJa("Freedom", "Cooking", "自由お料理係");
            addCombinedStateJa("Tracer", "Cooking", "探知お料理係");
            addCombinedStateJa("Escort", "Pharmcist", "護衛調合係");
            addCombinedStateJa("Freedom", "Pharmcist", "自由調合係");
            addCombinedStateJa("Tracer", "Pharmcist", "探知調合係");
            addCombinedStateJa("Escort", "Ripper", "護衛毛狩り隊");
            addCombinedStateJa("Freedom", "Ripper", "自由毛狩り隊");
            addCombinedStateJa("Tracer", "Ripper", "探知毛狩り隊");
            addCombinedStateJa("Escort", "Torcher", "護衛照明係");
            addCombinedStateJa("Freedom", "Torcher", "自由照明係");
            addCombinedStateJa("Tracer", "Torcher", "探知照明係");
            addCombinedStateJa("Escort", "Healer", "護衛回復係");
            addCombinedStateJa("Freedom", "Healer", "自由回復係");
            addCombinedStateJa("Tracer", "Healer", "探知回復係");
            addCombinedStateJa("Stroll", "Fencer", "散歩剣士");
            addCombinedStateJa("Stroll", "Archer", "散歩弓兵");
            addCombinedStateJa("Stroll", "Cooking", "散歩お料理係");
            addCombinedStateJa("Stroll", "Pharmcist", "散歩調合係");
            addCombinedStateJa("Stroll", "Ripper", "散歩毛狩り隊");
            addCombinedStateJa("Stroll", "Torcher", "散歩照明係");
            addCombinedStateJa("Stroll", "Healer", "散歩回復係");

            add("gui.littlemaidneo.littlemaid.salary.ok", "お給料: 正常");
            add("gui.littlemaidneo.littlemaid.salary.unpaid", "未払い: %s日");
            add("gui.littlemaidneo.littlemaid.salary.strike", "ストライキ中！");
            add("gui.littlemaidneo.target_tag.reset_all", "IFFリセット");
            add("gui.littlemaidneo.target_tag.reset_all.tooltip", "すべての敵対タグを初期状態に戻します");

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

            add("gui.littlemaidneo.target_tag.tags.attack_prohibited", "攻撃しない (平和的)");
            add("gui.littlemaidneo.target_tag.tags.preemptive_attack_prohibited", "反撃のみ (パッシブ)");
            add("gui.littlemaidneo.target_tag.tags.preemptive_attack_allowed", "先制攻撃する (アクティブ)");
            add("gui.littlemaidneo.target_tag.tags.no_weapon_restriction", "すべての武器を使用");
            add("gui.littlemaidneo.target_tag.tags.melee_weapon_prohibited", "近接武器を使用しない");
            add("gui.littlemaidneo.target_tag.tags.ranged_weapon_prohibited", "遠距離武器を使用しない");
            add("gui.littlemaidneo.target_tag.tags.approach_allowed", "近づく");
            add("gui.littlemaidneo.target_tag.tags.approach_prohibited", "近づかない");
            add("gui.littlemaidneo.model_select.toggle_mode", "テクスチャ選択モード切り替え (メイド/アーマー)");
            add("gui.littlemaidneo.model_select.toggle_contract", "契約状態切り替え (契約/野生)");

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
