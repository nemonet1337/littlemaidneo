package work.nemonet.littlemaidneo.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.advancement.criterion.ContractMaidCriterion;
import work.nemonet.littlemaidneo.advancement.criterion.LMNCriteria;
import work.nemonet.littlemaidneo.advancement.criterion.ResurrectMaidCriterion;
import work.nemonet.littlemaidneo.tags.LMTags;

import java.util.Optional;

public class LMAdvancementProvider {
    public static AdvancementSubProvider.Factory create() {
        return output -> new SubProvider(output);
    }

    public static class SubProvider extends AdvancementSubProvider {
        private final BootstrapContext<Advancement> output;

        protected SubProvider(BootstrapContext<Advancement> output) {
            super(output);
            this.output = output;
        }

        @Override
        public void generate() {
            HolderGetter<Item> itemLookup = output.lookup(Registries.ITEM);

            // parent の Identifier 版は @Deprecated(delete) だが、String 版は提供されておらず
            // MC 側でまだ置き換え先が無いため現状維持（NeoForge 更新時に追従する）。
            @SuppressWarnings("removal")
            AdvancementHolder contractMaid = Advancement.Builder.advancement()
                    .parent(net.minecraft.resources.Identifier.withDefaultNamespace("husbandry/root"))
                    .display(
                            Items.CAKE,
                            Component.translatable("advancements.husbandry.contract_maid.title"),
                            Component.translatable("advancements.husbandry.contract_maid.description"),
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("contracted_maid", LMNCriteria.CONTRACT_MAID.createCriterion(new ContractMaidCriterion.TriggerInstance(Optional.empty(), Optional.empty())))
                    .save(output, LittleMaidNeo.MODID + ":husbandry/contract_maid");

            Advancement.Builder.advancement()
                    .parent(contractMaid)
                    .display(
                            Items.CAKE,
                            Component.translatable("advancements.husbandry.resurrect_maid.title"),
                            Component.translatable("advancements.husbandry.resurrect_maid.description"),
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("resurrected_maid", LMNCriteria.RESURRECT_MAID.createCriterion(new ResurrectMaidCriterion.TriggerInstance(Optional.empty(), Optional.empty())))
                    .save(output, LittleMaidNeo.MODID + ":husbandry/resurrect_maid");

            // レシピ解除実績（recipes/<recipe_id>）は LMRecipeProvider がレシピと同時に自動生成する。
            // 同じ id をここでも生成すると二重出力で内容が競合するため、ここでは生成しない。
        }
    }
}
