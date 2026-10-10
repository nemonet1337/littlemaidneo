package work.nemonet.littlemaidneo.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.triggers.RecipeUnlockedTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
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
            HolderGetter<Recipe<?>> recipeLookup = output.lookup(Registries.RECIPE);

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

            ResourceKey<Recipe<?>> spawnEggRecipe = ResourceKey.create(Registries.RECIPE, net.minecraft.resources.Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "little_maid_spawn_egg"));
            ResourceKey<Recipe<?>> salaryBoxRecipe = ResourceKey.create(Registries.RECIPE, net.minecraft.resources.Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "salary_box"));

            Advancement.Builder.advancement()
                    .parent(net.minecraft.resources.Identifier.withDefaultNamespace("recipes/root"))
                    .addCriterion("sugar", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(itemLookup, LMTags.Items.MAIDS_SALARY).build()
                    ))
                    .addCriterion("cake", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(itemLookup, LMTags.Items.MAIDS_EMPLOYABLE).build()
                    ))
                    .addCriterion("gold_ingot", InventoryChangeTrigger.TriggerInstance.hasItems(
                            Items.GOLD_INGOT
                    ))
                    .addCriterion("egg", InventoryChangeTrigger.TriggerInstance.hasItems(
                            Items.EGG
                    ))
                    .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeLookup.getOrThrow(spawnEggRecipe)))
                    .requirements(AdvancementRequirements.Strategy.OR)
                    .rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(spawnEggRecipe))
                    .save(output, LittleMaidNeo.MODID + ":recipes/little_maid_spawn_egg");

            Advancement.Builder.advancement()
                    .parent(net.minecraft.resources.Identifier.withDefaultNamespace("recipes/root"))
                    .addCriterion("sugar", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(itemLookup, Items.SUGAR).build()
                    ))
                    .addCriterion("barrel", InventoryChangeTrigger.TriggerInstance.hasItems(
                            Items.BARREL
                    ))
                    .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeLookup.getOrThrow(salaryBoxRecipe)))
                    .requirements(AdvancementRequirements.Strategy.OR)
                    .rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(salaryBoxRecipe))
                    .save(output, LittleMaidNeo.MODID + ":recipes/salary_box");
        }
    }
}
